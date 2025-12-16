package dev.luminous.mod.modules.impl.client;

import dev.luminous.mod.gui.notification.NotificationManager;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.EnumSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import net.minecraft.client.gui.DrawContext;

public class Notifications extends Module {
    public static Notifications INSTANCE;

    private final BooleanSetting moduleToggle = add(new BooleanSetting("ModuleToggle", true));
    private final BooleanSetting combatNotifs = add(new BooleanSetting("CombatNotifs", true));
    private final BooleanSetting playerNotifs = add(new BooleanSetting("PlayerNotifs", true));
    private final BooleanSetting friendNotifs = add(new BooleanSetting("FriendNotifs", true));
    private final BooleanSetting playSound = add(new BooleanSetting("PlaySound", false));
    private final SliderSetting duration = add(new SliderSetting("Duration", 3000, 1000, 10000, 100));
    private final EnumSetting<AnimationMode> animation = add(new EnumSetting<>("Animation", AnimationMode.Slide));

    public Notifications() {
        super("Notifications", "Enhanced notification system", Category.Client);
        setChinese("通知系统");
        INSTANCE = this;
    }

    @Override
    public void onRender2D(DrawContext drawContext, float tickDelta) {
        NotificationManager.getInstance().draw(drawContext);
    }

    public boolean shouldShowModuleToggle() {
        return isOn() && moduleToggle.getValue();
    }

    public boolean shouldShowCombat() {
        return isOn() && combatNotifs.getValue();
    }

    public boolean shouldShowPlayer() {
        return isOn() && playerNotifs.getValue();
    }

    public boolean shouldShowFriend() {
        return isOn() && friendNotifs.getValue();
    }

    public boolean shouldPlaySound() {
        return isOn() && playSound.getValue();
    }

    public long getDuration() {
        return duration.getValueLong();
    }

    public enum AnimationMode {
        Slide,
        Fade,
        Scale,
        None
    }
}
