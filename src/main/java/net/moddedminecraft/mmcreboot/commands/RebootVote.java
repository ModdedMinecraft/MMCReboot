package net.moddedminecraft.mmcreboot.commands;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.moddedminecraft.mmcreboot.Config.Config;
import net.moddedminecraft.mmcreboot.Config.Messages;
import net.moddedminecraft.mmcreboot.Config.Permissions;
import net.moddedminecraft.mmcreboot.Main;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandExecutor;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.exception.CommandException;
import org.spongepowered.api.command.parameter.CommandContext;
import org.spongepowered.api.command.parameter.Parameter;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.service.pagination.PaginationService;
import org.spongepowered.configurate.serialize.SerializationException;

import java.util.*;

public class RebootVote implements CommandExecutor {

    private final Main plugin;
    public RebootVote(Main instance) {
        plugin = instance;
    }

    @Override
    public CommandResult execute(CommandContext context) throws CommandException {
        Parameter.Value<String> answerParameter = Parameter.string().key("answer").build();
        final Optional<String> answerOp = context.one(answerParameter);

        Audience audience = context.cause().audience();
        String playerName = "";
        if (context.cause().root() instanceof ServerPlayer) {
            ServerPlayer player = (ServerPlayer) context.cause().root();
            playerName = player.name();
        }

        if (answerOp.isPresent()) {
            String answer = answerOp.get();
            switch (answer) {
                case "on":
                    if (context.hasPermission(Permissions.TOGGLE_VOTE)) {
                        Config.voteEnabled = true;
                        try {
                            Config.config.node("voting", "enabled").set("true");
                        } catch (SerializationException e) {
                            throw new RuntimeException(e);
                        }
                        return CommandResult.success();
                    } else {
                        return CommandResult.error(Component.text("You do not have permission to use this command"));
                    }

                case "off":
                    if (context.hasPermission(Permissions.TOGGLE_VOTE)) {
                        Config.voteEnabled = false;
                        try {
                            Config.config.node("voting", "enabled").set("false");
                        } catch (SerializationException e) {
                            throw new RuntimeException(e);
                        }
                        return CommandResult.success();
                    } else {
                        return CommandResult.error(Component.text("You do not have permission to use this command"));
                    }

                case "yes":
                    if (plugin.hasVoted.contains(playerName)) {
                        throw new CommandException(plugin.fromLegacy(Messages.getErrorAlreadyVoted()));
                    }
                    if (plugin.voteStarted) {
                        plugin.yesVotes += 1;
                        if (context.cause().root() instanceof ServerPlayer) {
                            plugin.hasVoted.add(playerName);
                        }
                        plugin.displayVotes();
                        plugin.sendMessage(audience, Messages.getVotedYes());
                        return CommandResult.success();
                    } else {
                        throw new CommandException(plugin.fromLegacy(Messages.getErrorNoVoteRunning()));
                    }

                case "no":
                    if (plugin.hasVoted.contains(playerName)) {
                        throw new CommandException(plugin.fromLegacy(Messages.getErrorAlreadyVoted()));
                    }
                    if (plugin.voteStarted) {
                        plugin.noVotes += 1;
                        if (context.cause().root() instanceof ServerPlayer) {
                            plugin.hasVoted.add(playerName);
                        }
                        plugin.displayVotes();
                        plugin.sendMessage(audience, Messages.getVotedNo());
                        return CommandResult.success();

                    } else {
                        throw new CommandException(plugin.fromLegacy(Messages.getErrorNoVoteRunning()));
                    }

                default:
                    return CommandResult.error(Component.text("please enter a valid answer"));

            }
        } else {
            double timeLeft = 0;
            if (Config.restartInterval > 0) {
                timeLeft = (Config.restartInterval * 3600) - ((double) (System.currentTimeMillis() - plugin.startTimestamp) / 1000);
            } else if (plugin.nextRealTimeRestart > 0){
                timeLeft = (plugin.nextRealTimeRestart) - ((double) (System.currentTimeMillis() - plugin.startTimestamp) / 1000);
            }
            int hours = (int) (timeLeft / 3600);
            int minutes = (int) ((timeLeft - hours * 3600) / 60);

            if (!context.hasPermission(Permissions.BYPASS) && !context.hasPermission(Permissions.COMMAND_VOTE)) {
                throw new CommandException(plugin.fromLegacy(Messages.getErrorNoPermission()));
            }
            if (!context.hasPermission(Permissions.BYPASS) && !Config.voteEnabled) {
                throw new CommandException(plugin.fromLegacy(Messages.getErrorVoteToRestartDisabled()));
            }
            if (plugin.hasVoted.contains(playerName)) {
                throw new CommandException(plugin.fromLegacy(Messages.getErrorAlreadyVoted()));
            }
            if (plugin.voteStarted) {
                throw new CommandException(plugin.fromLegacy(Messages.getErrorVoteAlreadyRunning()));
            }
            if (!context.hasPermission(Permissions.BYPASS) && plugin.justStarted) {
                throw new CommandException(plugin.fromLegacy(Messages.getErrorNotOnlineLongEnough()));
            }
            if (!context.hasPermission(Permissions.BYPASS) && Sponge.server().onlinePlayers().size() < Config.timerMinplayers) {
                throw new CommandException(plugin.fromLegacy(Messages.getErrorMinPlayers()));
            }
            if (plugin.isRestarting && timeLeft != 0 && (hours == 0 && minutes <= 10)) {
                throw new CommandException(plugin.fromLegacy(Messages.getErrorAlreadyRestarting()));
            }
            if (plugin.cdTimer) {
                throw new CommandException(plugin.fromLegacy(Messages.getErrorWaitTime()));
            }


            if (context.cause().root() instanceof ServerPlayer) {
                plugin.voteStarted = true;
                plugin.voteCancel = false;
                plugin.hasVoted.add(playerName);
                plugin.yesVotes += 1;
                plugin.noVotes = 0;
                plugin.voteSeconds = 90;
                plugin.displayVotes();
            } else {
                plugin.voteStarted = true;
                plugin.displayVotes();
            }

            PaginationService paginationService = Sponge.serviceProvider().provide(PaginationService.class).get();
            List<Component> contents = new ArrayList<>();
            List<String> broadcast = Messages.getRestartVoteBroadcast();
            if (broadcast != null) {
                for (String line : broadcast) {
                    String checkLine = line.replace("%playername$", playerName).replace("%config.timerminplayers%", String.valueOf(Config.timerMinplayers));
                    contents.add(plugin.fromLegacy(checkLine));
                }
            }

            if (!contents.isEmpty()) {
                paginationService.builder()
                        .title(plugin.fromLegacy("Restart"))
                        .contents(contents)
                        .padding(Component.text("="))
                        .sendTo(Sponge.server().broadcastAudience());
            }

            Timer voteTimer = new Timer();
            voteTimer.schedule(new TimerTask() {
                public void run() {
                    int Online = Sponge.server().onlinePlayers().size();
                    int percentage = plugin.yesVotes/Online *100;

                    boolean yesAboveNo = plugin.yesVotes > plugin.noVotes;
                    boolean yesAboveMin = plugin.yesVotes >= Config.timerMinplayers;
                    boolean requiredPercent = percentage >= Config.timerVotepercent;

                    if (yesAboveNo && yesAboveMin && !plugin.voteCancel && requiredPercent) {
                        plugin.isRestarting = true;
                        Config.restartInterval = (Config.timerVotepassed + 1) / 3600.0;
                        plugin.logger.info("[MMCReboot] scheduling restart tasks...");
                        plugin.reason = Messages.getRestartPassed();
                        plugin.scheduleTasks();
                    } else {
                        if (!plugin.voteCancel) {
                            plugin.broadcastMessage("&f[&6Restart&f] " + Messages.getRestartVoteNotEnoughVoted());
                        }
                        plugin.voteCancel = false;
                        Timer voteTimer = new Timer();
                        voteTimer.schedule(new TimerTask() {
                            public void run() {
                                plugin.cdTimer = false;
                            }
                        }, (long) (Config.timerRevote * 60000.0));
                    }
                    plugin.removeScoreboard();
                    plugin.removeBossBar();
                    plugin.yesVotes = 0;
                    plugin.cdTimer = true;
                    plugin.voteStarted = false;
                    plugin.hasVoted.clear();
                }
            }, 90000);
            return CommandResult.success();
        }
    }
}
