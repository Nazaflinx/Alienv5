package dev.luminous.mod.modules.impl.client;

import dev.luminous.mod.gui.font.FontRenderers;
import dev.luminous.mod.modules.settings.impl.EnumSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import dev.luminous.mod.modules.Module;

public class FontSetting extends Module {
    public static FontSetting INSTANCE;
    public final EnumSetting<FontStyle> fontStyle = add(new EnumSetting<>("Style", FontStyle.Embedded));
    public final SliderSetting size = add(new SliderSetting("Size", 8, 1, 15, 1));
    public final SliderSetting yOffset = add(new SliderSetting("Offset", 0, -5, 15, 0.1));
    public FontSetting() {
        super("Font", Category.Client);
        setChinese("字体设置");
        INSTANCE = this;
        fontStyle.injectTask(this::reloadFont);
        size.injectTask(this::reloadFont);
    }

    @Override
    public void enable() {
        reloadFont();
    }

    private void reloadFont() {
        FontStyle style = fontStyle.getValue();
        float fontSize = size.getValueFloat();
        if (style.usesEmbedded()) {
            FontRenderers.applyRenderer(() -> FontRenderers.createDefault(fontSize, "font"));
        } else {
            FontRenderers.applyRenderer(() -> FontRenderers.createSystem(style.getFamilyName(), fontSize));
        }
    }

    public enum FontStyle {
        Embedded("font"),
        Tahoma("Tahoma"),
        Arial("Arial");

        private final String familyName;

        FontStyle(String familyName) {
            this.familyName = familyName;
        }

        public boolean usesEmbedded() {
            return this == Embedded;
        }

        public String getFamilyName() {
            return familyName;
        }
    }
}
