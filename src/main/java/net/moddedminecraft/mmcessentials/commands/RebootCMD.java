package net.moddedminecraft.mmcessentials.commands;

import net.moddedminecraft.mmcessentials.Main;
import net.moddedminecraft.mmcessentials.Util;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.text.format.TextColors;

import java.util.Optional;

public class RebootCMD implements CommandExecutor {

    private final Main plugin;
    public RebootCMD(Main instance) {
        plugin = instance;
    }

    @Override
    public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {
        String timeValue = args.<String>getOne("h/m/s").get();
        int timeAmount = args.<Integer>getAll("time").size();
        Optional<String> reasonOP = args.<String>getOne("reason");
        double restartTime = 0;

        if (!reasonOP.isPresent()) {
            plugin.reason = null;
        } else {
            plugin.reason = reasonOP.get();
        }

        if(timeValue.equalsIgnoreCase("h")) {
            restartTime = timeAmount * 3600;
        } else if(timeValue.equalsIgnoreCase("m")) {
            restartTime = (timeAmount * 60) + 1;
        } else if(timeValue.equalsIgnoreCase("s")) {
            restartTime = timeAmount;
        } else {
            Util.sendMessage(src, "&cInvalid time scale!");
            Util.sendMessage(src, "&bUse 'h' for time in hours, 'm' for minutes and 's' for seconds");
        }

        /*if (src instanceof Player) {
            if (src.hasPermission("mmcessentials.RebootCMD.limit")) {
                if (restartTime <= 59) {
                    Util.sendMessage(src, "&cYou cannot set the timer lower than 60 seconds.");
                } else if (restartTime >= 302) {
                    Util.sendMessage(src, "&cYou cannot set the timer higher than 301 seconds.");
                }
            }
        }*/

        plugin.logger.info("[MMCEssentials] " + src.toString() + " is setting a new restart time...");

        if(plugin.restartEnabled) {
            plugin.cancelTasks();
        }

        plugin.restartInterval = restartTime / 3600.0;

        plugin.logger.info("[MMCEssentials] scheduling restart tasks...");
        plugin.removeScoreboard();
        plugin.scheduleTasks();
        plugin.isRestarting = true;

        if (restartTime <= 300) {
            plugin.displayRestart();
        }

        double timeLeft = (plugin.restartInterval * 3600) - ((double)(System.currentTimeMillis() - plugin.startTimestamp) / 1000);
        int hours = (int)(timeLeft / 3600);
        int minutes = (int)((timeLeft - hours * 3600) / 60);
        int seconds = (int)timeLeft % 60;

        if (reasonOP.isPresent()) {
            Util.sendMessage(src, "&3The server will now be restarting in &f"+ hours + "h" + minutes + "m" + seconds + "s &3with the reason:");
            Util.sendMessage(src, "&6" + reasonOP.get());
        } else {
            Util.sendMessage(src, "&3The server will now be restarting in &f" + TextColors.WHITE + hours + "h" + minutes + "m" + seconds + "s");
        }

        return CommandResult.success();
    }
}
