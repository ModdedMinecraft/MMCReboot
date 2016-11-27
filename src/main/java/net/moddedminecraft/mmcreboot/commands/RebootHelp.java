package net.moddedminecraft.mmcreboot.commands;

import net.moddedminecraft.mmcreboot.Helplist;
import net.moddedminecraft.mmcreboot.Main;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;

import java.util.ArrayList;

public class RebootHelp implements CommandExecutor {

    private final Main plugin;
    public RebootHelp(Main instance) {
        plugin = instance;
    }

    @Override
    public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {
        showHelp(src);
        return CommandResult.success();
    }

    void showHelp(CommandSource sender) {
        plugin.sendMessage(sender, "&f--- &3Reboot &bHelp &f---");

        ArrayList<Helplist> helpList = new ArrayList<Helplist>();

        helpList.add(new Helplist("&3[] = required  () = optional"));
        helpList.add(new Helplist("&3/reboot &bhelp - &7shows this help"));

        if (sender.hasPermission("mmcreboot.reboot.now")) {
            helpList.add(new Helplist("&3/reboot &bnow - &7restarts the server instantly"));
        } if (sender.hasPermission("mmcreboot.reboot.start")) {
            helpList.add(new Helplist("&3/reboot start &7[&bh&7|&bm&7|&bs&7] &f[time] (reason) - &7restart the server after a given time"));
        } if (sender.hasPermission("mmcreboot.reboot.cancel")) {
            helpList.add(new Helplist("&3/reboot &bcancel - &7cancel any current restart timer"));
        } if (sender.hasPermission("mmcreboot.reboot.vote")) {
            helpList.add(new Helplist("&3/reboot &bvote - &7starts a vote to restart the server"));
        }

        helpList.add(new Helplist("&3/reboot &btime - &7informs you how much time is left before restarting"));
        helpList.add(new Helplist("&3/reboot &bvote yes - &7vote yes to restart the server"));
        helpList.add(new Helplist("&3/reboot &bvote no - &7vote no to restart the server"));

        for(int i = 0; i < helpList.size(); i++) {
            plugin.sendMessage(sender, helpList.get(i).command);

        }
    }
}
