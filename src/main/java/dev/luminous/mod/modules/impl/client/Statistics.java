package dev.luminous.mod.modules.impl.client;

import dev.luminous.Alien;
import dev.luminous.api.events.eventbus.EventHandler;
import dev.luminous.api.events.impl.PacketEvent;
import dev.luminous.api.utils.render.Render2DUtil;
import dev.luminous.api.utils.render.TextUtil;
import dev.luminous.mod.gui.font.FontRenderers;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.ColorSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;

import java.awt.*;
import java.text.DecimalFormat;
import java.util.LinkedHashMap;
import java.util.Map;

public class Statistics extends Module {
    public static Statistics INSTANCE;

    private final BooleanSetting showSessionTime = add(new BooleanSetting("SessionTime", true));
    private final BooleanSetting showModuleUsage = add(new BooleanSetting("ModuleUsage", true));
    private final BooleanSetting showInteractions = add(new BooleanSetting("Interactions", true));
    private final BooleanSetting showTPSGraph = add(new BooleanSetting("TPSGraph", true));
    private final BooleanSetting showPingGraph = add(new BooleanSetting("PingGraph", true));
    private final BooleanSetting customFont = add(new BooleanSetting("CustomFont", true));
    private final ColorSetting color = add(new ColorSetting("Color", new Color(0, 180, 255)));
    private final ColorSetting backgroundColor = add(new ColorSetting("Background", new Color(20, 20, 30, 200)));
    private final SliderSetting posX = add(new SliderSetting("X", 10, 0, 1000, 1));
    private final SliderSetting posY = add(new SliderSetting("Y", 100, 0, 1000, 1));

    private long sessionStartTime;
    private final Map<String, Integer> moduleUsageCount = new LinkedHashMap<>();
    private int blocksPlaced = 0;
    private int entitiesHit = 0;
    private final float[] tpsHistory = new float[60];
    private final int[] pingHistory = new int[60];
    private int historyIndex = 0;
    private long lastUpdateTime = 0;

    private final DecimalFormat timeFormat = new DecimalFormat("00");

    public Statistics() {
        super("Statistics", Category.Client);
        setChinese("统计");
        INSTANCE = this;
    }

    @Override
    public void onEnable() {
        sessionStartTime = System.currentTimeMillis();
        moduleUsageCount.clear();
        blocksPlaced = 0;
        entitiesHit = 0;
        historyIndex = 0;
        lastUpdateTime = System.currentTimeMillis();
    }

    @EventHandler
    public void onPacketSend(PacketEvent.Send event) {
        if (event.getPacket() instanceof PlayerInteractBlockC2SPacket) {
            blocksPlaced++;
        } else if (event.getPacket() instanceof PlayerInteractEntityC2SPacket) {
            entitiesHit++;
        }
    }

    @Override
    public void onUpdate() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastUpdateTime >= 1000) {
            tpsHistory[historyIndex] = Alien.SERVER.getCurrentTPS();
            if (mc.player != null && mc.getNetworkHandler() != null) {
                var entry = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
                pingHistory[historyIndex] = entry != null ? entry.getLatency() : 0;
            }
            historyIndex = (historyIndex + 1) % 60;
            lastUpdateTime = currentTime;
        }
    }

    public void recordModuleToggle(String moduleName) {
        moduleUsageCount.put(moduleName, moduleUsageCount.getOrDefault(moduleName, 0) + 1);
    }

    @Override
    public void onRender2D(DrawContext drawContext, float tickDelta) {
        int x = posX.getValueInt();
        int y = posY.getValueInt();
        int width = 240;
        int height = calculateHeight();

        MatrixStack matrixStack = drawContext.getMatrices();

        Render2DUtil.drawRound(matrixStack, x, y, width, height, 6, backgroundColor.getValue());

        int textY = y + 8;
        int lineHeight = getLineHeight() + 4;

        TextUtil.drawString(drawContext, "§l§nStatistics", x + 8, textY, color.getValue().getRGB(), customFont.getValue());
        textY += lineHeight + 4;

        if (showSessionTime.getValue()) {
            long sessionTime = System.currentTimeMillis() - sessionStartTime;
            String timeStr = formatTime(sessionTime);
            drawStat(drawContext, "Session Time", timeStr, x + 8, textY);
            textY += lineHeight;
        }

        if (showInteractions.getValue()) {
            drawStat(drawContext, "Blocks Placed", String.valueOf(blocksPlaced), x + 8, textY);
            textY += lineHeight;
            drawStat(drawContext, "Entities Hit", String.valueOf(entitiesHit), x + 8, textY);
            textY += lineHeight;
        }

        if (showModuleUsage.getValue() && !moduleUsageCount.isEmpty()) {
            textY += 4;
            TextUtil.drawStringWithScale(drawContext, "§7Top Modules:", x + 8, textY, new Color(180, 180, 180), 0.9f, customFont.getValue());
            textY += lineHeight;

            moduleUsageCount.entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .limit(3)
                .forEach(entry -> {
                    String text = "  " + entry.getKey() + ": §f" + entry.getValue();
                    TextUtil.drawStringWithScale(drawContext, text, x + 8, textY + moduleUsageCount.entrySet().stream().sorted((a, b) -> b.getValue().compareTo(a.getValue())).toList().indexOf(entry) * lineHeight,
                        new Color(200, 200, 200), 0.85f, customFont.getValue());
                });
            textY += lineHeight * Math.min(3, moduleUsageCount.size());
        }

        if (showTPSGraph.getValue()) {
            textY += 6;
            TextUtil.drawStringWithScale(drawContext, "§7TPS History:", x + 8, textY, new Color(180, 180, 180), 0.9f, customFont.getValue());
            textY += lineHeight + 2;
            drawGraph(matrixStack, x + 8, textY, width - 16, 30, tpsHistory, 20f, new Color(46, 204, 113));
            textY += 35;
        }

        if (showPingGraph.getValue()) {
            textY += 6;
            TextUtil.drawStringWithScale(drawContext, "§7Ping History:", x + 8, textY, new Color(180, 180, 180), 0.9f, customFont.getValue());
            textY += lineHeight + 2;
            drawGraph(matrixStack, x + 8, textY, width - 16, 30, convertIntToFloat(pingHistory), 200f, new Color(52, 152, 219));
        }
    }

    private void drawStat(DrawContext drawContext, String label, String value, int x, int y) {
        String text = label + ": §f" + value;
        TextUtil.drawStringWithScale(drawContext, text, x, y, new Color(200, 200, 200), 0.9f, customFont.getValue());
    }

    private void drawGraph(MatrixStack matrixStack, int x, int y, int width, int height, float[] data, float maxValue, Color color) {
        Render2DUtil.drawRect(matrixStack, x, y, width, height, new Color(0, 0, 0, 80));

        int points = Math.min(data.length, width);
        float xStep = (float) width / points;

        for (int i = 1; i < points; i++) {
            int idx1 = (historyIndex + i - 1) % data.length;
            int idx2 = (historyIndex + i) % data.length;

            float value1 = Math.min(data[idx1], maxValue) / maxValue;
            float value2 = Math.min(data[idx2], maxValue) / maxValue;

            float x1 = x + (i - 1) * xStep;
            float y1 = y + height - (value1 * height);
            float x2 = x + i * xStep;
            float y2 = y + height - (value2 * height);

            Render2DUtil.drawLine(matrixStack, x1, y1, x2, y2, 2f, color);
        }

        String avgText = String.format("Avg: %.1f", calculateAverage(data));
        TextUtil.drawStringWithScale(null, avgText, x + width - 40, y + height - 10,
            new Color(220, 220, 220, 180), 0.7f, customFont.getValue());
    }

    private float calculateAverage(float[] data) {
        float sum = 0;
        int count = 0;
        for (float value : data) {
            if (value > 0) {
                sum += value;
                count++;
            }
        }
        return count > 0 ? sum / count : 0;
    }

    private float[] convertIntToFloat(int[] data) {
        float[] result = new float[data.length];
        for (int i = 0; i < data.length; i++) {
            result[i] = data[i];
        }
        return result;
    }

    private String formatTime(long milliseconds) {
        long seconds = milliseconds / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;

        seconds %= 60;
        minutes %= 60;

        return timeFormat.format(hours) + ":" + timeFormat.format(minutes) + ":" + timeFormat.format(seconds);
    }

    private int calculateHeight() {
        int height = 50;

        if (showSessionTime.getValue()) height += getLineHeight() + 4;
        if (showInteractions.getValue()) height += (getLineHeight() + 4) * 2;
        if (showModuleUsage.getValue() && !moduleUsageCount.isEmpty()) {
            height += (getLineHeight() + 4) * (Math.min(3, moduleUsageCount.size()) + 1) + 4;
        }
        if (showTPSGraph.getValue()) height += 50;
        if (showPingGraph.getValue()) height += 50;

        return height;
    }

    private int getLineHeight() {
        if (customFont.getValue()) {
            return (int) (FontRenderers.ui.getFontHeight() * 0.9f);
        }
        return mc.textRenderer.fontHeight;
    }
}
