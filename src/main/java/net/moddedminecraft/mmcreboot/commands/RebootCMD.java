package net.moddedminecraft.mmcreboot.commands;

import net.moddedminecraft.mmcreboot.Config.Config;
import net.moddedminecraft.mmcreboot.Config.Messages;
import net.moddedminecraft.mmcreboot.Main;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.text.Text;

import java.util.Optional;

public class RebootCMD implements CommandExecutor {

    private final Main plugin;


    public RebootCMD(Main instance) {
        plugin = instance;
    }

    @Override
    public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {
        String timeValue = args.<String>getOne("h/m/s").get();
        int timeAmount = args.<Integer>getOne("time").get();
        Optional<String> reasonOP = args.getOne("reason");
        double restartTime;

        plugin.reason = reasonOP.orElse(null);

        switch (timeValue) {
            case "h":
                restartTime = timeAmount * 3600;
                break;
            case "m":
                restartTime = (timeAmount * 60) + 1;
                break;
            case "s":
                restartTime = timeAmount;
                break;
            default:
                plugin.sendMessage(src, Messages.getRestartFormatMessage());
                src.sendMessage(Text.of());
                throw new CommandException(plugin.fromLegacy(Messages.getErrorInvalidTimescale()));
        }

        plugin.logger.info("[MMCReboot] " + src.getName() + " is setting a new restart time...");

        if(plugin.tasksScheduled) {
            plugin.cancelTasks();
        }

        Config.restartInterval = restartTime / 3600.0;

        plugin.logger.info("[MMCReboot] scheduling restart tasks...");
        plugin.removeScoreboard();
        plugin.scheduleTasks();
        plugin.isRestarting = true;

        if (restartTime <= 300) {
            plugin.displayRestart();
        }

        double timeLeft = (Config.restartInterval * 3600) - ((double)(System.currentTimeMillis() - plugin.startTimestamp) / 1000);
        int hours = (int)(timeLeft / 3600);
        int minutes = (int)((timeLeft - hours * 3600) / 60);
        int seconds = (int)timeLeft % 60;

        if (reasonOP.isPresent()) {
            plugin.sendMessage(src, Messages.getRestartMessageWithReason()
                    .replace("%hours%", String.valueOf(hours))
                    .replace("%minutes%", String.valueOf(minutes))
                    .replace("%seconds%", String.valueOf(seconds)));
            plugin.sendMessage(src, "&6" + plugin.reason);
        } else {
            plugin.sendMessage(src, Messages.getRestartMessageWithoutReason()
                    .replace("%hours%", String.valueOf(hours))
                    .replace("%minutes%", String.valueOf(minutes))
                    .replace("%seconds%", String.valueOf(seconds)));
        }

        return CommandResult.success();
    }
}
