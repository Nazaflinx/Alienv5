package dev.luminous.mod.modules.impl.combat;

import dev.luminous.api.events.eventbus.EventHandler;
import dev.luminous.api.events.impl.PacketEvent;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;

public class Reach extends Module {
    public static Reach INSTANCE;

    private final SliderSetting range = add(new SliderSetting("Range", 3.5, 3.0, 6.0, 0.1));
    private final SliderSetting wallRange = add(new SliderSetting("WallRange", 3.0, 3.0, 6.0, 0.1));

    public Reach() {
        super("Reach", "Extends your attack range", Category.Combat);
        setChinese("扩展范围");
        INSTANCE = this;
    }

    @Override
    public String getInfo() {
        return String.format("%.1f", range.getValue());
    }

    public double getRange() {
        return range.getValue();
    }

    public double getWallRange() {
        return wallRange.getValue();
    }

    @EventHandler
    public void onPacketSend(PacketEvent.Send event) {
        if (event.getPacket() instanceof PlayerInteractEntityC2SPacket) {
            if (!isValidReach()) {
                event.cancel();
            }
        }
    }

    private boolean isValidReach() {
        if (mc.targetedEntity == null) return true;
        double distance = mc.player.squaredDistanceTo(mc.targetedEntity);
        double maxReach = range.getValue() * range.getValue();
        return distance <= maxReach;
    }
}
