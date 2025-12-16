package dev.luminous.mod.commands.impl;

import dev.luminous.Alien;
import dev.luminous.core.impl.CommandManager;
import dev.luminous.core.impl.PresetManager;
import dev.luminous.mod.commands.Command;

import java.util.List;

public class PresetCommand extends Command {
    public PresetCommand() {
        super("Preset", "<save|load|delete|list> <name>");
    }

    @Override
    public void runCommand(String[] args) {
        if (args.length == 0) {
            sendUsage();
            return;
        }

        String action = args[0].toLowerCase();

        switch (action) {
            case "save":
                if (args.length < 2) {
                    CommandManager.sendChatMessage("§cUsage: " + Alien.PREFIX + "preset save <name>");
                    return;
                }
                PresetManager.savePreset(args[1]);
                break;

            case "load":
                if (args.length < 2) {
                    CommandManager.sendChatMessage("§cUsage: " + Alien.PREFIX + "preset load <name>");
                    return;
                }
                PresetManager.loadPreset(args[1]);
                break;

            case "delete":
                if (args.length < 2) {
                    CommandManager.sendChatMessage("§cUsage: " + Alien.PREFIX + "preset delete <name>");
                    return;
                }
                PresetManager.deletePreset(args[1]);
                break;

            case "list":
                List<String> presets = PresetManager.getPresets();
                if (presets.isEmpty()) {
                    CommandManager.sendChatMessage("§7No presets found.");
                } else {
                    CommandManager.sendChatMessage("§aAvailable Presets:");
                    for (String preset : presets) {
                        CommandManager.sendChatMessage("  §7- §f" + preset);
                    }
                }
                break;

            default:
                sendUsage();
                break;
        }
    }

    @Override
    public String[] getAutocorrect(int count, List<String> seperated) {
        if (count == 1) {
            return new String[]{"save", "load", "delete", "list"};
        } else if (count == 2 && seperated.size() > 1 &&
                   (seperated.get(1).equalsIgnoreCase("load") ||
                    seperated.get(1).equalsIgnoreCase("delete"))) {
            List<String> presets = PresetManager.getPresets();
            return presets.toArray(new String[0]);
        }
        return null;
    }
}
