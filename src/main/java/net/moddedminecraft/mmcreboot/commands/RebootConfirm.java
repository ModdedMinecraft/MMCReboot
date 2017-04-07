package net.moddedminecraft.mmcreboot.commands;

import net.moddedminecraft.mmcreboot.Config.Messages;
import net.moddedminecraft.mmcreboot.Main;
import org.spongepowered.api.command.CommandException;
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
    public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {
        if (plugin.rebootConfirm == 1) {
            plugin.sendMessage(src, Messages.getRestartConfirm());
            plugin.stopServer();
            return CommandResult.success();
        } else {
            throw new CommandException(plugin.fromLegacy(Messages.getErrorNothingToConfirm()));
        }
    }
}
