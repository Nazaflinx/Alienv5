package dev.luminous.mod.gui.clickgui;

import dev.luminous.api.utils.Wrapper;
import dev.luminous.api.utils.math.Animation;
import dev.luminous.api.utils.render.Render2DUtil;
import dev.luminous.api.utils.render.TextUtil;
import dev.luminous.mod.modules.impl.client.ClickGui;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class Tooltip implements Wrapper {
    private String title;
    private List<String> description;
    private int x;
    private int y;
    private Animation animation = new Animation();
    private boolean shouldShow = false;
    private long hoverStartTime = 0;
    private static final long HOVER_DELAY = 500;

    public Tooltip() {
        this.description = new ArrayList<>();
    }

    public void setContent(String title, String description) {
        this.title = title;
        this.description = wrapText(description, 200);
    }

    public void setContent(String title, List<String> description) {
        this.title = title;
        this.description = description;
    }

    public void startHover(int x, int y) {
        if (hoverStartTime == 0) {
            hoverStartTime = System.currentTimeMillis();
        }
        this.x = x;
        this.y = y;
    }

    public void endHover() {
        shouldShow = false;
        hoverStartTime = 0;
    }

    public void update() {
        if (hoverStartTime > 0 && System.currentTimeMillis() - hoverStartTime > HOVER_DELAY) {
            shouldShow = true;
        }
    }

    public void draw(DrawContext drawContext) {
        update();

        if (title == null || description.isEmpty()) return;

        double alpha = animation.get(shouldShow ? 1.0 : 0.0);
        if (alpha < 0.01) return;

        MatrixStack matrixStack = drawContext.getMatrices();

        int maxWidth = (int) TextUtil.getWidth(title);
        for (String line : description) {
            maxWidth = Math.max(maxWidth, (int) TextUtil.getWidth(line));
        }

        int width = maxWidth + 16;
        int height = (int) (TextUtil.getHeight() * (description.size() + 1) + 14);

        int screenWidth = mc.getWindow().getScaledWidth();
        int screenHeight = mc.getWindow().getScaledHeight();

        int drawX = x + 10;
        int drawY = y + 10;

        if (drawX + width > screenWidth) {
            drawX = x - width - 10;
        }
        if (drawY + height > screenHeight) {
            drawY = y - height - 10;
        }

        matrixStack.push();
        matrixStack.translate(drawX, drawY, 0);
        matrixStack.scale((float) alpha, (float) alpha, 1);
        matrixStack.translate(-drawX, -drawY, 0);

        Color bgColor = new Color(20, 20, 30, (int) (240 * alpha));
        Color borderColor = new Color(
            ClickGui.INSTANCE.color.getValue().getRed(),
            ClickGui.INSTANCE.color.getValue().getGreen(),
            ClickGui.INSTANCE.color.getValue().getBlue(),
            (int) (200 * alpha)
        );

        Render2DUtil.drawRound(matrixStack, drawX, drawY, width, height, 4, bgColor);
        Render2DUtil.drawRoundOutline(matrixStack, drawX, drawY, width, height, 4, 2f, borderColor);

        int textColor = new Color(255, 255, 255, (int) (255 * alpha)).getRGB();
        int descColor = new Color(200, 200, 200, (int) (200 * alpha)).getRGB();

        TextUtil.drawString(drawContext, title, drawX + 8, drawY + 6, textColor);

        float yOffset = (float) (drawY + 6 + TextUtil.getHeight() + 4);
        for (String line : description) {
            TextUtil.drawStringWithScale(drawContext, line, drawX + 8, yOffset,
                new Color(descColor, true), 0.9f);
            yOffset += TextUtil.getHeight() * 0.9f + 2;
        }

        matrixStack.pop();
    }

    private List<String> wrapText(String text, int maxWidth) {
        List<String> lines = new ArrayList<>();
        String[] words = text.split(" ");
        StringBuilder currentLine = new StringBuilder();

        for (String word : words) {
            String testLine = currentLine.length() == 0 ? word : currentLine + " " + word;
            if (TextUtil.getWidth(testLine) <= maxWidth) {
                if (currentLine.length() > 0) {
                    currentLine.append(" ");
                }
                currentLine.append(word);
            } else {
                if (currentLine.length() > 0) {
                    lines.add(currentLine.toString());
                    currentLine = new StringBuilder(word);
                } else {
                    lines.add(word);
                }
            }
        }

        if (currentLine.length() > 0) {
            lines.add(currentLine.toString());
        }

        return lines;
    }

    public boolean isShowing() {
        return shouldShow;
    }
}
