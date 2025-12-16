package dev.luminous.mod.gui.clickgui;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class ColorPreset {
    private static final List<Color> PRESETS = new ArrayList<>();
    private static final List<Color> HISTORY = new ArrayList<>();
    private static final int MAX_HISTORY = 10;

    static {
        PRESETS.add(new Color(255, 255, 255));
        PRESETS.add(new Color(0, 0, 0));
        PRESETS.add(new Color(231, 76, 60));
        PRESETS.add(new Color(46, 204, 113));
        PRESETS.add(new Color(52, 152, 219));
        PRESETS.add(new Color(155, 89, 182));
        PRESETS.add(new Color(241, 196, 15));
        PRESETS.add(new Color(230, 126, 34));
        PRESETS.add(new Color(149, 165, 166));
        PRESETS.add(new Color(52, 73, 94));
        PRESETS.add(new Color(0, 180, 255));
        PRESETS.add(new Color(138, 43, 226));
        PRESETS.add(new Color(255, 20, 147));
        PRESETS.add(new Color(0, 255, 127));
        PRESETS.add(new Color(255, 215, 0));
        PRESETS.add(new Color(255, 69, 0));
    }

    public static List<Color> getPresets() {
        return new ArrayList<>(PRESETS);
    }

    public static List<Color> getHistory() {
        return new ArrayList<>(HISTORY);
    }

    public static void addToHistory(Color color) {
        if (color == null) return;

        for (Color historyColor : HISTORY) {
            if (historyColor.getRGB() == color.getRGB()) {
                return;
            }
        }

        if (HISTORY.size() >= MAX_HISTORY) {
            HISTORY.remove(0);
        }
        HISTORY.add(color);
    }

    public static void clearHistory() {
        HISTORY.clear();
    }

    public static class ColorPalette {
        public static Color[] getRainbow() {
            return new Color[]{
                new Color(255, 0, 0),
                new Color(255, 127, 0),
                new Color(255, 255, 0),
                new Color(0, 255, 0),
                new Color(0, 0, 255),
                new Color(75, 0, 130),
                new Color(148, 0, 211)
            };
        }

        public static Color[] getPastel() {
            return new Color[]{
                new Color(255, 179, 186),
                new Color(255, 223, 186),
                new Color(255, 255, 186),
                new Color(186, 255, 201),
                new Color(186, 225, 255),
                new Color(220, 198, 224),
                new Color(255, 198, 255)
            };
        }

        public static Color[] getDark() {
            return new Color[]{
                new Color(44, 62, 80),
                new Color(52, 73, 94),
                new Color(44, 44, 44),
                new Color(33, 33, 33),
                new Color(22, 22, 22),
                new Color(11, 11, 11),
                new Color(0, 0, 0)
            };
        }

        public static Color[] getNeon() {
            return new Color[]{
                new Color(255, 0, 255),
                new Color(0, 255, 255),
                new Color(255, 255, 0),
                new Color(0, 255, 0),
                new Color(255, 0, 0),
                new Color(0, 100, 255),
                new Color(255, 100, 0)
            };
        }
    }
}
