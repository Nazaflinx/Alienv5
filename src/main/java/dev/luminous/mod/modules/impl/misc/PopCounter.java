package dev.luminous.mod.modules.impl.misc;

import dev.luminous.Alien;
import dev.luminous.mod.modules.impl.client.ClientSetting;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.EnumSetting;
import dev.luminous.api.events.eventbus.EventHandler;
import dev.luminous.api.events.impl.DeathEvent;
import dev.luminous.api.events.impl.TotemEvent;
import dev.luminous.core.impl.CommandManager;
import dev.luminous.mod.modules.Module;
import net.minecraft.entity.player.PlayerEntity;

public class PopCounter extends Module {

    public static PopCounter INSTANCE;

    public final EnumSetting<Mode> mode = add(new EnumSetting<>("Mode", Mode.Both));
    public final BooleanSetting unPop = add(new BooleanSetting("Dead", true));
    public final BooleanSetting coords = add(new BooleanSetting("Coords", false));
    public final BooleanSetting distance = add(new BooleanSetting("Distance", true));
    public final BooleanSetting friend = add(new BooleanSetting("Friend", true));

    public enum Mode {
        Pop,
        Death,
        Both
    }

    public PopCounter() {
        super("PopCounter", "Counts players totem pops", Category.Misc);
        setChinese("图腾计数器");
        INSTANCE = this;
    }

    @EventHandler
    public void onPlayerDeath(DeathEvent event) {
        if (mode.getValue() == Mode.Pop) return;

        PlayerEntity player = event.getPlayer();
        if (!friend.getValue() && Alien.FRIEND.isFriend(player)) return;

        String extraInfo = getExtraInfo(player);

        if (Alien.POP.popContainer.containsKey(player.getName().getString())) {
            int l_Count = Alien.POP.popContainer.get(player.getName().getString());
            String totemText = l_Count == 1 ? "totem" : "totems";

            if (player.equals(mc.player)) {
                sendMessage("§fYou §cdied §rafter popping §c" + l_Count + " §r" + totemText + extraInfo, player.getId());
            } else {
                sendMessage("§f" + player.getName().getString() + " §cdied §rafter popping §c" + l_Count + " §r" + totemText + extraInfo, player.getId());
            }
        } else if (unPop.getValue()) {
            if (player.equals(mc.player)) {
                sendMessage("§fYou §cdied" + extraInfo, player.getId());
            } else {
                sendMessage("§f" + player.getName().getString() + " §cdied" + extraInfo, player.getId());
            }
        }
    }

    @EventHandler
    public void onTotem(TotemEvent event) {
        if (mode.getValue() == Mode.Death) return;

        PlayerEntity player = event.getPlayer();
        if (!friend.getValue() && Alien.FRIEND.isFriend(player)) return;

        String extraInfo = getExtraInfo(player);

        int l_Count = 1;
        if (Alien.POP.popContainer.containsKey(player.getName().getString())) {
            l_Count = Alien.POP.popContainer.get(player.getName().getString());
        }

        String totemText = l_Count == 1 ? "totem" : "totems";
        String verb = l_Count == 1 ? "popped" : "has popped";

        if (player.equals(mc.player)) {
            sendMessage("§fYou §epopped §a" + l_Count + " §r" + totemText + extraInfo, player.getId());
        } else {
            sendMessage("§f" + player.getName().getString() + " §e" + verb + " §a" + l_Count + " §r" + totemText + extraInfo, player.getId());
        }
    }

    private String getExtraInfo(PlayerEntity player) {
        StringBuilder info = new StringBuilder();

        if (coords.getValue()) {
            info.append(" §7[")
                .append(player.getBlockX()).append(", ")
                .append(player.getBlockY()).append(", ")
                .append(player.getBlockZ()).append("]");
        }

        if (distance.getValue() && !player.equals(mc.player)) {
            double dist = mc.player.distanceTo(player);
            info.append(" §7(").append(String.format("%.1f", dist)).append("m)");
        }

        return info.toString();
    }
    
    public void sendMessage(String message, int id) {
        if (!nullCheck()) {
            if (ClientSetting.INSTANCE.messageStyle.getValue() == ClientSetting.Style.Moon) {
                CommandManager.sendChatMessageWidthId("§f[" + "§3" + getName() + "§f] " + message, id);
                return;
            }
            CommandManager.sendChatMessageWidthId(message, id);//"§6[!] " + message, id);
        }
    }
}

