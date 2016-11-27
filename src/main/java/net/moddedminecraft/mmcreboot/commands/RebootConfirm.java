package net.moddedminecraft.mmcreboot.commands;

import net.moddedminecraft.mmcreboot.Main;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;

public class RebootConfirm implements CommandExecutor {

    private final Main plugin;
    public RebootConfirm(Main instance) {
        plugin = instance;
    }

    @Override
    public CommandResult execute(CommandSource src, CommandContext args) {
        if (plugin.rebootConfirm == 1) {
            plugin.sendMessage(src, "&cOk, you asked for it!");
            plugin.stopServer();
            return CommandResult.success();
        } else {
            plugin.sendMessage(src, "&cThere is nothing to confirm.");
            return CommandResult.success();
        }
    }
}
