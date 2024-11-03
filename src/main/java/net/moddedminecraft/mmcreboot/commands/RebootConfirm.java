package net.moddedminecraft.mmcreboot.commands;

import net.moddedminecraft.mmcreboot.Config.Messages;
import net.moddedminecraft.mmcreboot.Main;
import org.spongepowered.api.command.CommandExecutor;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.exception.CommandException;
import org.spongepowered.api.command.parameter.CommandContext;

public class RebootConfirm implements CommandExecutor {

    private final Main plugin;
    public RebootConfirm(Main instance) {
        plugin = instance;
    }

    @Override
    public CommandResult execute(CommandContext context) throws CommandException {
        if (plugin.rebootConfirm) {
            plugin.sendMessage(context.cause().audience(), Messages.getRestartConfirm());
            plugin.stopServer();
            return CommandResult.success();
        } else {
            throw new CommandException(plugin.fromLegacy(Messages.getErrorNothingToConfirm()));
        }
    }
}
