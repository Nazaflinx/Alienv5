package dev.luminous.mod.modules.impl.render;

import com.google.common.collect.Maps;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.ColorSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import dev.luminous.api.events.eventbus.EventHandler;
import dev.luminous.api.events.impl.PacketEvent;
import dev.luminous.core.impl.CommandManager;
import dev.luminous.api.utils.render.ColorUtil;
import dev.luminous.api.utils.render.Render3DUtil;
import dev.luminous.asm.accessors.IEntity;
import dev.luminous.mod.modules.Module;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerRemoveS2CPacket;
import net.minecraft.util.math.Vec3d;

import java.awt.*;
import java.util.Map;
import java.util.UUID;

public class LogoutSpots extends Module {
    private final ColorSetting color = add(new ColorSetting("Color", new Color(255, 255, 255, 100)));
    private final BooleanSetting box = add(new BooleanSetting("Box", true));
    private final BooleanSetting outline = add(new BooleanSetting("Outline", true));
    private final BooleanSetting text = add(new BooleanSetting("Text", true));
    private final BooleanSetting distance = add(new BooleanSetting("Distance", true));
    private final BooleanSetting coords = add(new BooleanSetting("Coords", false));
    private final BooleanSetting message = add(new BooleanSetting("Message", true));
    private final SliderSetting range = add(new SliderSetting("Range", 256, 16, 512));
    private final SliderSetting maxSpots = add(new SliderSetting("MaxSpots", 20, 1, 50));

    private final Map<UUID, PlayerEntity> playerCache = Maps.newConcurrentMap();
    private final Map<UUID, PlayerEntity> logoutCache = Maps.newConcurrentMap();

    public LogoutSpots() {
        super("LogoutSpots", Category.Render);
        setChinese("退出记录");
    }

    @EventHandler
    public void onPacketReceive(PacketEvent.Receive event) {
        if (event.getPacket() instanceof PlayerListS2CPacket packet) {
            if (packet.getActions().contains(PlayerListS2CPacket.Action.ADD_PLAYER)) {
                for (PlayerListS2CPacket.Entry addedPlayer : packet.getPlayerAdditionEntries()) {
                    for (UUID uuid : logoutCache.keySet()) {
                        if (!uuid.equals(addedPlayer.profile().getId())) continue;
                        PlayerEntity player = logoutCache.get(uuid);
                        if (message.getValue()) {
                            double dist = mc.player.distanceTo(player);
                            CommandManager.sendChatMessage("§f" + player.getName().getString() + " §alogged back §fat §e" + player.getBlockX() + ", " + player.getBlockY() + ", " + player.getBlockZ() + " §7(" + String.format("%.1f", dist) + "m)");
                        }
                        logoutCache.remove(uuid);
                    }
                }
            }
            playerCache.clear();
        } else if (event.getPacket() instanceof PlayerRemoveS2CPacket packet) {
            for (UUID uuid2 : packet.profileIds()) {
                for (UUID uuid : playerCache.keySet()) {
                    if (!uuid.equals(uuid2)) continue;
                    final PlayerEntity player = playerCache.get(uuid);
                    if (!logoutCache.containsKey(uuid)) {
                        if (logoutCache.size() >= maxSpots.getValue()) {
                            UUID oldestKey = logoutCache.keySet().iterator().next();
                            logoutCache.remove(oldestKey);
                        }
                        if (message.getValue()) {
                            double dist = mc.player.distanceTo(player);
                            CommandManager.sendChatMessage("§f" + player.getName().getString() + " §clogged out §fat §e" + player.getBlockX() + ", " + player.getBlockY() + ", " + player.getBlockZ() + " §7(" + String.format("%.1f", dist) + "m)");
                        }
                        logoutCache.put(uuid, player);
                    }
                }
            }
            playerCache.clear();
        }
    }

    @Override
    public void onEnable() {
        playerCache.clear();
        logoutCache.clear();
    }

    @Override
    public void onUpdate() {
        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == null || player.equals(mc.player)) continue;
            playerCache.put(player.getGameProfile().getId(), player);
        }
    }

    @Override
    public void onRender3D(MatrixStack matrixStack) {
        for (UUID uuid : logoutCache.keySet()) {
            final PlayerEntity data = logoutCache.get(uuid);
            if (data == null) continue;

            double dist = mc.player.distanceTo(data);
            if (dist > range.getValue()) continue;

            Render3DUtil.draw3DBox(matrixStack, ((IEntity) data).getDimensions().getBoxAt(data.getPos()), color.getValue(), outline.getValue(), box.getValue());
            if (text.getValue()) {
                String displayText = data.getName().getString();
                if (distance.getValue()) {
                    displayText += " §7(" + String.format("%.1f", dist) + "m)";
                }
                if (coords.getValue()) {
                    displayText += " §7[" + data.getBlockX() + ", " + data.getBlockY() + ", " + data.getBlockZ() + "]";
                }
                Render3DUtil.drawText3D(displayText, new Vec3d(data.getX(), ((IEntity) data).getDimensions().getBoxAt(data.getPos()).maxY + 0.5, data.getZ()), ColorUtil.injectAlpha(color.getValue(), 255));
            }
        }
    }
}