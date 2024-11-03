package net.moddedminecraft.mmcreboot.commands;

import net.moddedminecraft.mmcreboot.Config.Config;
import net.moddedminecraft.mmcreboot.Config.Messages;
import net.moddedminecraft.mmcreboot.Main;
import org.spongepowered.api.command.CommandExecutor;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.exception.CommandException;
import org.spongepowered.api.command.parameter.CommandContext;
import org.spongepowered.api.command.parameter.Parameter;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;

import java.util.Optional;

public class RebootCMD implements CommandExecutor {

    private final Main plugin;


    public RebootCMD(Main instance) {
        plugin = instance;
    }

    @Override
    public CommandResult execute(CommandContext context) throws CommandException {

        Parameter.Value<String> formatParameter = Parameter.string().key("h/m/s").build();
        Parameter.Value<Integer> timeParameter = Parameter.integerNumber().key("time").build();
        Parameter.Value<String> reasonParameter = Parameter.string().key("reason").build();

        final String format = context.requireOne(formatParameter);
        final Integer time = context.requireOne(timeParameter);
        final Optional<String> reasonOp = context.one(reasonParameter);

        String name = "Console";

        if (context.cause().root() instanceof ServerPlayer) {
            ServerPlayer player = (ServerPlayer) context.cause().root();
            name = player.name();
        }

        double restartTime;

        plugin.reason = reasonOp.orElse(null);

        switch (format) {
            case "h":
                restartTime = time * 3600;
                break;
            case "m":
                restartTime = (time * 60) + 1;
                break;
            case "s":
                restartTime = time;
                break;
            default:
                plugin.sendMessage(context.cause().audience(), Messages.getRestartFormatMessage());
                throw new CommandException(plugin.fromLegacy(Messages.getErrorInvalidTimescale()));
        }

        plugin.logger.info("[MMCReboot] " + name + " is setting a new restart time...");

        if(plugin.tasksScheduled) {
            plugin.cancelTasks();
        }

        Config.restartInterval = restartTime / 3600.0;

        plugin.logger.info("[MMCReboot] scheduling restart tasks...");
        plugin.removeScoreboard();
        plugin.removeBossBar();
        plugin.scheduleTasks();
        plugin.isRestarting = true;

        if (restartTime <= 300 && Config.timerUseScoreboard) {
            plugin.displayRestart(Config.restartInterval * 3600);
        }

        double timeLeft = (Config.restartInterval * 3600) - ((double)(System.currentTimeMillis() - plugin.startTimestamp) / 1000);
        int hours = (int)(timeLeft / 3600);
        int minutes = (int)((timeLeft - hours * 3600) / 60);
        int seconds = (int)timeLeft % 60;

        if (reasonOp.isPresent()) {
            plugin.sendMessage(context.cause().audience(), Messages.getRestartMessageWithReason()
                    .replace("%hours%", String.valueOf(hours))
                    .replace("%minutes%", String.valueOf(minutes))
                    .replace("%seconds%", String.valueOf(seconds)));
            plugin.sendMessage(context.cause().audience(), "&6" + plugin.reason);
        } else {
            plugin.sendMessage(context.cause().audience(), Messages.getRestartMessageWithoutReason()
                    .replace("%hours%", String.valueOf(hours))
                    .replace("%minutes%", String.valueOf(minutes))
                    .replace("%seconds%", String.valueOf(seconds)));
        }

        return CommandResult.success();
    }
}
