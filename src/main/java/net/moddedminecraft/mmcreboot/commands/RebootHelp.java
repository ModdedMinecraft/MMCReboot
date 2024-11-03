package net.moddedminecraft.mmcreboot.commands;

import net.kyori.adventure.text.Component;
import net.moddedminecraft.mmcreboot.Config.Messages;
import net.moddedminecraft.mmcreboot.Config.Permissions;
import net.moddedminecraft.mmcreboot.Main;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandExecutor;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.exception.CommandException;
import org.spongepowered.api.command.parameter.CommandContext;
import org.spongepowered.api.service.pagination.PaginationService;

import java.util.ArrayList;
import java.util.List;

public class RebootHelp implements CommandExecutor {

    private final Main plugin;
    public RebootHelp(Main instance) {
        plugin = instance;
    }

    @Override
    public CommandResult execute(CommandContext context) throws CommandException {
        showHelp(context);
        return CommandResult.success();
    }

    void showHelp(CommandContext context) {
        PaginationService paginationService = Sponge.serviceProvider().provide(PaginationService.class).get();

        List<Component> contents = new ArrayList<>();
        contents.add(plugin.fromLegacy(Messages.getHelpHelp()));
        if (context.hasPermission(Permissions.COMMAND_NOW)) contents.add(plugin.fromLegacy(Messages.getHelpNow()));
        if (context.hasPermission(Permissions.COMMAND_START)) contents.add(plugin.fromLegacy(Messages.getHelpStart()));
        if (context.hasPermission(Permissions.COMMAND_CANCEL)) contents.add(plugin.fromLegacy(Messages.getHelpCancel()));
        if (context.hasPermission(Permissions.COMMAND_VOTE)) contents.add(plugin.fromLegacy(Messages.getHelpVote()));
        if (context.hasPermission(Permissions.COMMAND_TIME)) contents.add(plugin.fromLegacy(Messages.getHelpTime()));
        contents.add(plugin.fromLegacy(Messages.getHelpVoteYea()));
        contents.add(plugin.fromLegacy(Messages.getHelpVoteNo()));

        paginationService.builder()
                .title(plugin.fromLegacy("&6MMCReboot Help"))
                .contents(contents)
                .header(plugin.fromLegacy(Messages.getHelpHeader()))
                .padding(Component.text("="))
                .sendTo(context.cause().audience());
    }
}
