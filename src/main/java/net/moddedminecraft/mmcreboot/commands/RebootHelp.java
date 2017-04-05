package net.moddedminecraft.mmcreboot.commands;

import net.moddedminecraft.mmcreboot.Config.Permissions;
import net.moddedminecraft.mmcreboot.Main;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.service.pagination.PaginationService;
import org.spongepowered.api.text.Text;

import java.util.ArrayList;
import java.util.List;

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
        PaginationService paginationService = Sponge.getServiceManager().provide(PaginationService.class).get();

        List<Text> contents = new ArrayList<>();
        contents.add(plugin.fromLegacy("&3/reboot &bhelp - &7shows this help"));
        if (sender.hasPermission(Permissions.COMMAND_NOW)) contents.add(plugin.fromLegacy("&3/reboot &bnow - &7restarts the server instantly"));
        if (sender.hasPermission(Permissions.COMMAND_START)) contents.add(plugin.fromLegacy("&3/reboot start &7[&bh&7|&bm&7|&bs&7] &7[&btime&7] &7(&breason&7) &b- &7restart the server after a given time"));
        if (sender.hasPermission(Permissions.COMMAND_CANCEL)) contents.add(plugin.fromLegacy("&3/reboot &bcancel - &7cancel any current restart timer"));
        if (sender.hasPermission(Permissions.COMMAND_VOTE)) contents.add(plugin.fromLegacy("&3/reboot &bvote - &7starts a vote to restart the server"));
        if (sender.hasPermission(Permissions.COMMAND_TIME)) contents.add(plugin.fromLegacy("&3/reboot &btime - &7informs you how much time is left before restarting"));
        contents.add(plugin.fromLegacy("&3/reboot &bvote yes - &7vote yes to restart the server"));
        contents.add(plugin.fromLegacy("&3/reboot &bvote no - &7vote no to restart the server"));

        paginationService.builder()
                .title(plugin.fromLegacy("&6MMCReboot Help"))
                .contents(contents)
                .header(plugin.fromLegacy("&3[] = required  () = optional"))
                .padding(Text.of("="))
                .sendTo(sender);
    }
}
