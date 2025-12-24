package dev.luminous.mod.modules.impl.client;

import dev.luminous.Alien;
import dev.luminous.core.impl.GuiManager;
import dev.luminous.api.utils.math.Easing;
import dev.luminous.api.utils.math.FadeUtils;
import dev.luminous.mod.gui.clickgui.ClickGuiScreen;
import dev.luminous.mod.gui.clickgui.components.Component;
import dev.luminous.mod.gui.clickgui.components.impl.ModuleComponent;
import dev.luminous.mod.gui.clickgui.tabs.ClickGuiTab;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.ColorSetting;
import dev.luminous.mod.modules.settings.impl.EnumSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;

import java.awt.*;

public class ClickGui extends Module {
	public static ClickGui INSTANCE;
	private final EnumSetting<Pages> page = add(new EnumSetting<>("Page", Pages.General));
	public final EnumSetting<Type> uiType = add(new EnumSetting<>("UIType", Type.Old, () -> page.getValue() == Pages.Element));
	public final BooleanSetting activeBox = add(new BooleanSetting("ActiveBox", true, () -> page.getValue() == Pages.Element));
	public final BooleanSetting center = add(new BooleanSetting("Center", false, () -> page.getValue() == Pages.Element));
	public final ColorSetting bind = add(new ColorSetting("Bind", new Color(255, 255, 255), () -> page.getValue() == Pages.Element).injectBoolean(false));
	public final ColorSetting gear = add(new ColorSetting("Gear", new Color(255, 255, 255), () -> page.getValue() == Pages.Element).injectBoolean(true));

        public final BooleanSetting chinese = add(new BooleanSetting("Chinese", false, () -> page.getValue() == Pages.General));
        public final BooleanSetting font = add(new BooleanSetting("Font", true, () -> page.getValue() == Pages.General));
        public final BooleanSetting maxFill = add(new BooleanSetting("MaxFill", false, () -> page.getValue() == Pages.General));
        public final BooleanSetting sound = add(new BooleanSetting("Sound", true, () -> page.getValue() == Pages.General));
        public final BooleanSetting glass = add(new BooleanSetting("Glass", true, () -> page.getValue() == Pages.General));
        public final BooleanSetting shadow = add(new BooleanSetting("Shadow", true, () -> page.getValue() == Pages.General));
        public final SliderSetting cornerRadius = add(new SliderSetting("CornerRadius", 4, 0, 6, 1, () -> page.getValue() == Pages.General));
        public final SliderSetting height = add(new SliderSetting("Height", 15, 10, 20, 1, () -> page.getValue() == Pages.General));
        public final EnumSetting<Mode> mode = add(new EnumSetting<>("EnableAnim", Mode.Pull, () -> page.getValue() == Pages.General));
        public final SliderSetting animationTime = add(new SliderSetting("AnimationTime", 200, 0, 1000, 1, () -> page.getValue() == Pages.General));
        public final EnumSetting<Easing> ease = add(new EnumSetting<>("Ease", Easing.QuadInOut, () -> page.getValue() == Pages.General));

	public final ColorSetting color = add(new ColorSetting("Main", new Color(0, 180, 255, 200), () -> page.getValue() == Pages.Color));
	public final ColorSetting mainEnd = add(new ColorSetting("MainEnd", new Color(138, 43, 226, 200), () -> page.getValue() == Pages.Color).injectBoolean(true));
	public final ColorSetting mainHover = add(new ColorSetting("Hover", new Color(100, 220, 255, 220), () -> page.getValue() == Pages.Color));
	public final ColorSetting bar = add(new ColorSetting("Bar", new Color(30, 30, 50, 180), () -> page.getValue() == Pages.Color));
	public final ColorSetting barEnd = add(new ColorSetting("BarEnd", new Color(50, 50, 80, 180), () -> page.getValue() == Pages.Color).injectBoolean(true));
	public final ColorSetting disableText = add(new ColorSetting("DisableText", new Color(180, 180, 180, 255), () -> page.getValue() == Pages.Color));
	public final ColorSetting enableText = add(new ColorSetting("EnableText", new Color(255, 255, 255, 255), () -> page.getValue() == Pages.Color));
	public final ColorSetting enableTextS = add(new ColorSetting("EnableText2", new Color(0, 220, 255, 255), () -> page.getValue() == Pages.Color));
	public final ColorSetting module = add(new ColorSetting("Module", new Color(40, 40, 60, 200), () -> page.getValue() == Pages.Color));
	public final ColorSetting moduleHover = add(new ColorSetting("ModuleHover", new Color(60, 60, 90, 220), () -> page.getValue() == Pages.Color));
	public final ColorSetting setting = add(new ColorSetting("Setting", new Color(35, 35, 55, 180), () -> page.getValue() == Pages.Color));
	public final ColorSetting settingHover = add(new ColorSetting("SettingHover", new Color(55, 55, 85, 200), () -> page.getValue() == Pages.Color));
	public final ColorSetting background = add(new ColorSetting("Background", new Color(20, 20, 30, 230), () -> page.getValue() == Pages.Color));
	public ClickGui() {
		super("ClickGui", Category.Client);
		setChinese("菜单");
		INSTANCE = this;
	}

	public static final FadeUtils fade = new FadeUtils(300);

	@Override
	public void onUpdate() {
		if (chinese.getValue()) {
			font.setValue(false);
		}
		if (!(mc.currentScreen instanceof ClickGuiScreen)) {
			disable();
		}
	}

	int lastHeight;
	@Override
	public void onEnable() {
		//size = scale.getValue();
		if (lastHeight != height.getValueInt()) {
			for (ClickGuiTab tab : Alien.GUI.tabs) {
				for (Component component : tab.getChildren()) {
					if (component instanceof ModuleComponent moduleComponent) {
						for (Component settingComponent : moduleComponent.getSettingsList()) {
							settingComponent.setHeight(height.getValueInt());
							settingComponent.defaultHeight = height.getValueInt();
						}
					}
					component.setHeight(height.getValueInt());
					component.defaultHeight = height.getValueInt();
				}
			}
			lastHeight = height.getValueInt();
		}
		fade.reset();
		if (nullCheck()) {
			disable();
			return;
		}
		mc.setScreen(GuiManager.clickGui);
	}

	@Override
	public void onDisable() {
		if (mc.currentScreen instanceof ClickGuiScreen) {
			mc.setScreen(null);
		}
	}

	public enum Mode {
		Scale, Pull, None
	}

	private enum Pages {
		General,
		Color,
		Element
	}

	public enum Type {
		Old,
		New
	}
}