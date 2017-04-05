package net.moddedminecraft.mmcreboot.commands;

import net.moddedminecraft.mmcreboot.Config.Config;
import net.moddedminecraft.mmcreboot.Main;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;

public class RebootTime implements CommandExecutor {

    private final Main plugin;
    public RebootTime(Main instance) {
        plugin = instance;
    }

    @Override
    public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {
        if(!Config.restartEnabled) {
            throw new CommandException(plugin.fromLegacy("&cThere is no auto-restart scheduled!"));
        }

        double timeLeft = (Config.restartInterval * 3600) - ((double)(System.currentTimeMillis() - plugin.startTimestamp) / 1000);
        int hours = (int)(timeLeft / 3600);
        int minutes = (int)((timeLeft - hours * 3600) / 60);
        int seconds = (int)timeLeft % 60;

        plugin.sendMessage(src, "&bThe server will be restarting in &f" + hours + "h" + minutes + "m" + seconds + "s");
        return CommandResult.success();
    }
}
