package dev.luminous.mod.commands.impl;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.luminous.core.impl.PresetManager;
import dev.luminous.mod.commands.Command;
import dev.luminous.mod.gui.notification.NotificationManager;
import net.minecraft.command.CommandSource;

import java.util.List;

public class PresetCommand extends Command {
    public PresetCommand() {
        super("Preset", "Manage config presets");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(literal("save")
            .then(argument("name", StringArgumentType.string())
                .executes(context -> {
                    String name = StringArgumentType.getString(context, "name");
                    PresetManager.savePreset(name);
                    return SINGLE_SUCCESS;
                })
            )
            .executes(context -> {
                sendMessage("§cUsage: " + getPrefix() + "preset save <name>");
                return SINGLE_SUCCESS;
            })
        );

        builder.then(literal("load")
            .then(argument("name", StringArgumentType.string())
                .suggests((context, suggestionsBuilder) -> {
                    List<String> presets = PresetManager.getPresets();
                    return CommandSource.suggestMatching(presets, suggestionsBuilder);
                })
                .executes(context -> {
                    String name = StringArgumentType.getString(context, "name");
                    PresetManager.loadPreset(name);
                    return SINGLE_SUCCESS;
                })
            )
            .executes(context -> {
                sendMessage("§cUsage: " + getPrefix() + "preset load <name>");
                return SINGLE_SUCCESS;
            })
        );

        builder.then(literal("delete")
            .then(argument("name", StringArgumentType.string())
                .suggests((context, suggestionsBuilder) -> {
                    List<String> presets = PresetManager.getPresets();
                    return CommandSource.suggestMatching(presets, suggestionsBuilder);
                })
                .executes(context -> {
                    String name = StringArgumentType.getString(context, "name");
                    PresetManager.deletePreset(name);
                    return SINGLE_SUCCESS;
                })
            )
            .executes(context -> {
                sendMessage("§cUsage: " + getPrefix() + "preset delete <name>");
                return SINGLE_SUCCESS;
            })
        );

        builder.then(literal("list")
            .executes(context -> {
                List<String> presets = PresetManager.getPresets();
                if (presets.isEmpty()) {
                    sendMessage("§7No presets found.");
                } else {
                    sendMessage("§aAvailable Presets:");
                    for (String preset : presets) {
                        sendMessage("  §7- §f" + preset);
                    }
                }
                return SINGLE_SUCCESS;
            })
        );

        builder.executes(context -> {
            sendMessage("§6Preset Commands:");
            sendMessage("§7" + getPrefix() + "preset save <name> §f- Save current config");
            sendMessage("§7" + getPrefix() + "preset load <name> §f- Load a preset");
            sendMessage("§7" + getPrefix() + "preset delete <name> §f- Delete a preset");
            sendMessage("§7" + getPrefix() + "preset list §f- List all presets");
            return SINGLE_SUCCESS;
        });
    }
}
