package net.moddedminecraft.mmcreboot.commands;

import net.moddedminecraft.mmcreboot.Config;
import net.moddedminecraft.mmcreboot.Main;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;

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

        if (reasonOP.isPresent()) {
            plugin.reason = reasonOP.get();
        } else {
            plugin.reason = null;
        }

        if(timeValue.equals("h")) {
            restartTime = timeAmount * 3600;
        } else if(timeValue.equals("m")) {
            restartTime = (timeAmount * 60) + 1;
        } else if(timeValue.equals("s")) {
            restartTime = timeAmount;
        } else {
            plugin.sendMessage(src, "&cInvalid time scale!");
            plugin.sendMessage(src, "&bUse 'h' for time in hours, 'm' for minutes and 's' for seconds");
            return CommandResult.success();
        }

        plugin.logger.info("[MMCReboot] " + src.toString() + " is setting a new restart time...");

        if(Config.restartEnabled) {
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
            plugin.sendMessage(src, "&3The server will now be restarting in &f" + hours + "h" + minutes + "m" + seconds + "s &3with the reason:");
            plugin.sendMessage(src, "&6" + plugin.reason);
        } else {
            plugin.sendMessage(src, "&3The server will now be restarting in &f" + hours + "h" + minutes + "m" + seconds + "s");
        }

        return CommandResult.success();
    }
}
