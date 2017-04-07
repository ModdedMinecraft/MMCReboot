package net.moddedminecraft.mmcreboot.commands;

import net.moddedminecraft.mmcreboot.Config.Messages;
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
        contents.add(plugin.fromLegacy(Messages.getHelpHelp()));
        if (sender.hasPermission(Permissions.COMMAND_NOW)) contents.add(plugin.fromLegacy(Messages.getHelpNow()));
        if (sender.hasPermission(Permissions.COMMAND_START)) contents.add(plugin.fromLegacy(Messages.getHelpStart()));
        if (sender.hasPermission(Permissions.COMMAND_CANCEL)) contents.add(plugin.fromLegacy(Messages.getHelpCancel()));
        if (sender.hasPermission(Permissions.COMMAND_VOTE)) contents.add(plugin.fromLegacy(Messages.getHelpVote()));
        if (sender.hasPermission(Permissions.COMMAND_TIME)) contents.add(plugin.fromLegacy(Messages.getHelpTime()));
        contents.add(plugin.fromLegacy(Messages.getHelpVoteYea()));
        contents.add(plugin.fromLegacy(Messages.getHelpVoteNo()));

        paginationService.builder()
                .title(plugin.fromLegacy("&6MMCReboot Help"))
                .contents(contents)
                .header(plugin.fromLegacy(Messages.getHelpHeader()))
                .padding(Text.of("="))
                .sendTo(sender);
    }
}
