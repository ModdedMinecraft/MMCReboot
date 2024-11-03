package net.moddedminecraft.mmcreboot.commands;

import net.moddedminecraft.mmcreboot.Config.Messages;
import net.moddedminecraft.mmcreboot.Main;
import org.spongepowered.api.command.CommandExecutor;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.exception.CommandException;
import org.spongepowered.api.command.parameter.CommandContext;

import java.util.Timer;
import java.util.TimerTask;

public class RebootNow implements CommandExecutor {

    private final Main plugin;
    public RebootNow(Main instance) {
        plugin = instance;
    }

    Timer nowTimer;

    @Override
    public CommandResult execute(CommandContext context) throws CommandException {
        plugin.rebootConfirm = true;
        plugin.sendMessage(context.cause().audience(), Messages.getRestartConfirmMessage());

        nowTimer = new Timer();
        nowTimer.schedule(new TimerTask() {
            public void run() {
                plugin.rebootConfirm = false;
                plugin.sendMessage(context.cause().audience(), Messages.getErrorTookTooLong());
            }
        }, (60 * 1000));
        return CommandResult.success();
    }
}
