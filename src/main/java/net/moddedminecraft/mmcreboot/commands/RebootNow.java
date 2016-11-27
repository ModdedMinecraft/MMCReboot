package net.moddedminecraft.mmcreboot.commands;

import net.moddedminecraft.mmcreboot.Main;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;

import java.util.Timer;
import java.util.TimerTask;

public class RebootNow implements CommandExecutor {

    private final Main plugin;
    public RebootNow(Main instance) {
        plugin = instance;
    }

    Timer nowTimer;

    @Override
    public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {
        plugin.rebootConfirm = 1;
        plugin.sendMessage(src, "&cPlease type: &6/Reboot Confirm &cif you are sure you want to do this.");

        nowTimer = new Timer();
        nowTimer.schedule(new TimerTask() {
            public void run() {
                plugin.rebootConfirm = 0;
                plugin.sendMessage(src, "&cYou took too long to confirm the reboot.");
            }
        }, (60 * 1000));
        return CommandResult.success();
    }
}
