package net.moddedminecraft.mmcreboot.commands;

import net.moddedminecraft.mmcreboot.Config.Messages;
import net.moddedminecraft.mmcreboot.Main;
import org.spongepowered.api.command.CommandExecutor;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.exception.CommandException;
import org.spongepowered.api.command.parameter.CommandContext;

import java.util.Timer;
import java.util.TimerTask;

public class RebootCancel implements CommandExecutor {

    private final Main plugin;
    public RebootCancel(Main instance) {
        plugin = instance;
    }

    @Override
    public CommandResult execute(CommandContext context) throws CommandException {
        plugin.voteCancel = true;
        Timer voteCancelimer = new Timer();
        voteCancelimer.schedule(new TimerTask() {
            public void run() {
                plugin.voteCancel = false;
            }
        }, (long) (15 * 60000.0));
        plugin.cancelTasks();
        plugin.removeScoreboard();
        plugin.removeBossBar();
        plugin.isRestarting = false;
        plugin.sendMessage(context.cause().audience(), Messages.getRestartCancel());
        return CommandResult.success();
    }
}
