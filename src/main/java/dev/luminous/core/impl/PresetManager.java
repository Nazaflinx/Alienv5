package dev.luminous.core.impl;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import dev.luminous.Alien;
import dev.luminous.api.utils.Wrapper;
import dev.luminous.mod.gui.notification.NotificationManager;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.settings.Setting;
import dev.luminous.mod.modules.settings.impl.*;

import java.awt.*;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;

public class PresetManager implements Wrapper {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File PRESETS_DIR = new File("Alien/presets");

    static {
        if (!PRESETS_DIR.exists()) {
            PRESETS_DIR.mkdirs();
        }
    }

    public static void savePreset(String name) {
        try {
            JsonObject root = new JsonObject();
            JsonObject modules = new JsonObject();

            for (Module module : Alien.MODULE.modules) {
                JsonObject moduleData = new JsonObject();
                moduleData.addProperty("enabled", module.isOn());

                JsonObject settings = new JsonObject();
                for (Setting setting : module.getSettings()) {
                    if (setting instanceof BooleanSetting boolSetting) {
                        settings.addProperty(setting.getName(), boolSetting.getValue());
                    } else if (setting instanceof SliderSetting sliderSetting) {
                        settings.addProperty(setting.getName(), sliderSetting.getValue());
                    } else if (setting instanceof EnumSetting<?> enumSetting) {
                        settings.addProperty(setting.getName(), enumSetting.getValue().toString());
                    } else if (setting instanceof StringSetting stringSetting) {
                        settings.addProperty(setting.getName(), stringSetting.getValue());
                    } else if (setting instanceof ColorSetting colorSetting) {
                        JsonObject colorData = new JsonObject();
                        Color color = colorSetting.getValue();
                        colorData.addProperty("r", color.getRed());
                        colorData.addProperty("g", color.getGreen());
                        colorData.addProperty("b", color.getBlue());
                        colorData.addProperty("a", color.getAlpha());
                        colorData.addProperty("boolValue", colorSetting.booleanValue);
                        settings.add(setting.getName(), colorData);
                    }
                }

                moduleData.add("settings", settings);
                modules.add(module.getName(), moduleData);
            }

            root.add("modules", modules);
            root.addProperty("timestamp", System.currentTimeMillis());

            File presetFile = new File(PRESETS_DIR, name + ".json");
            try (FileWriter writer = new FileWriter(presetFile)) {
                GSON.toJson(root, writer);
            }

            NotificationManager.getInstance().success("Preset Saved",
                "Preset '" + name + "' saved successfully");
        } catch (Exception e) {
            NotificationManager.getInstance().error("Save Failed",
                "Failed to save preset: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void loadPreset(String name) {
        try {
            File presetFile = new File(PRESETS_DIR, name + ".json");
            if (!presetFile.exists()) {
                NotificationManager.getInstance().error("Load Failed",
                    "Preset '" + name + "' not found");
                return;
            }

            JsonObject root;
            try (FileReader reader = new FileReader(presetFile)) {
                root = GSON.fromJson(reader, JsonObject.class);
            }

            JsonObject modules = root.getAsJsonObject("modules");
            if (modules == null) return;

            for (Module module : Alien.MODULE.modules) {
                JsonObject moduleData = modules.getAsJsonObject(module.getName());
                if (moduleData == null) continue;

                boolean enabled = moduleData.get("enabled").getAsBoolean();
                if (enabled != module.isOn()) {
                    module.toggle();
                }

                JsonObject settings = moduleData.getAsJsonObject("settings");
                if (settings == null) continue;

                for (Setting setting : module.getSettings()) {
                    if (!settings.has(setting.getName())) continue;

                    try {
                        if (setting instanceof BooleanSetting boolSetting) {
                            boolSetting.setValue(settings.get(setting.getName()).getAsBoolean());
                        } else if (setting instanceof SliderSetting sliderSetting) {
                            sliderSetting.setValue(settings.get(setting.getName()).getAsDouble());
                        } else if (setting instanceof EnumSetting<?> enumSetting) {
                            String enumValue = settings.get(setting.getName()).getAsString();
                            enumSetting.setValueByName(enumValue);
                        } else if (setting instanceof StringSetting stringSetting) {
                            stringSetting.setValue(settings.get(setting.getName()).getAsString());
                        } else if (setting instanceof ColorSetting colorSetting) {
                            JsonObject colorData = settings.getAsJsonObject(setting.getName());
                            int r = colorData.get("r").getAsInt();
                            int g = colorData.get("g").getAsInt();
                            int b = colorData.get("b").getAsInt();
                            int a = colorData.get("a").getAsInt();
                            colorSetting.setValue(new Color(r, g, b, a));
                            if (colorData.has("boolValue")) {
                                colorSetting.booleanValue = colorData.get("boolValue").getAsBoolean();
                            }
                        }
                    } catch (Exception e) {
                        System.err.println("Failed to load setting: " + setting.getName());
                    }
                }
            }

            NotificationManager.getInstance().success("Preset Loaded",
                "Preset '" + name + "' loaded successfully");
        } catch (Exception e) {
            NotificationManager.getInstance().error("Load Failed",
                "Failed to load preset: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void deletePreset(String name) {
        File presetFile = new File(PRESETS_DIR, name + ".json");
        if (presetFile.exists()) {
            if (presetFile.delete()) {
                NotificationManager.getInstance().success("Preset Deleted",
                    "Preset '" + name + "' deleted successfully");
            } else {
                NotificationManager.getInstance().error("Delete Failed",
                    "Failed to delete preset '" + name + "'");
            }
        }
    }

    public static List<String> getPresets() {
        List<String> presets = new ArrayList<>();
        File[] files = PRESETS_DIR.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.getName().endsWith(".json")) {
                    presets.add(file.getName().replace(".json", ""));
                }
            }
        }
        return presets;
    }

    public static boolean presetExists(String name) {
        return new File(PRESETS_DIR, name + ".json").exists();
    }
}
