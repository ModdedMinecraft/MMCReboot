package net.moddedminecraft.mmcreboot.commands;

import net.moddedminecraft.mmcreboot.Config.Config;
import net.moddedminecraft.mmcreboot.Config.Messages;
import net.moddedminecraft.mmcreboot.Main;
import org.spongepowered.api.command.CommandExecutor;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.exception.CommandException;
import org.spongepowered.api.command.parameter.CommandContext;

public class RebootTime implements CommandExecutor {

    private final Main plugin;
    public RebootTime(Main instance) {
        plugin = instance;
    }

    @Override
    public CommandResult execute(CommandContext context) throws CommandException {
        if(!plugin.tasksScheduled) {
            throw new CommandException(plugin.fromLegacy(Messages.getErrorNoTaskScheduled()));
        }

        if(Config.restartType.equalsIgnoreCase("fixed") || (Config.restartInterval > 0 && plugin.isRestarting && (plugin.nextRealTimeRestart > Config.restartInterval || plugin.nextRealTimeRestart == 0))) {
            double timeLeft = (Config.restartInterval * 3600) - ((double)(System.currentTimeMillis() - plugin.startTimestamp) / 1000);
            int hours = (int)(timeLeft / 3600);
            int minutes = (int)((timeLeft - hours * 3600) / 60);
            int seconds = (int)timeLeft % 60;

            plugin.sendMessage(context.cause().audience(), Messages.getRestartMessageWithoutReason()
                    .replace("%hours%", String.valueOf(hours))
                    .replace("%minutes%", String.valueOf(minutes))
                    .replace("%seconds%", String.valueOf(seconds)));
            return CommandResult.success();
        } else if(Config.restartType.equalsIgnoreCase("realtime")) {
            double timeLeft = plugin.nextRealTimeRestart - ((double)(System.currentTimeMillis() - plugin.startTimestamp) / 1000);
            int hours = (int)(timeLeft / 3600);
            int minutes = (int)((timeLeft - hours * 3600) / 60);
            int seconds = (int)timeLeft % 60;

            plugin.sendMessage(context.cause().audience(), Messages.getRestartMessageWithoutReason()
                    .replace("%hours%", String.valueOf(hours))
                    .replace("%minutes%", String.valueOf(minutes))
                    .replace("%seconds%", String.valueOf(seconds)));
            return CommandResult.success();
        } else {
            throw new CommandException(plugin.fromLegacy(Messages.getErrorNoTaskScheduled()));
        }
    }
}
