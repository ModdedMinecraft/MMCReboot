package net.moddedminecraft.mmcreboot.commands;

import net.moddedminecraft.mmcreboot.Config.Config;
import net.moddedminecraft.mmcreboot.Config.Messages;
import net.moddedminecraft.mmcreboot.Config.Permissions;
import net.moddedminecraft.mmcreboot.Main;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.service.pagination.PaginationService;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.channel.MessageChannel;

import java.util.*;

public class RebootVote implements CommandExecutor {

    private final Main plugin;
    public RebootVote(Main instance) {
        plugin = instance;
    }

    @Override
    public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {
        Optional<String> optional = args.getOne("optional");

        if (optional.isPresent()) {
            String op = optional.get();
            switch (op) {
                case "on":
                    if (src.hasPermission(Permissions.TOGGLE_VOTE)) {
                        Config.voteEnabled = true;
                        Config.config.getNode("voting", "enabled").setValue("true");
                        return CommandResult.success();
                    } else {
                        return CommandResult.empty();
                    }

                case "off":
                    if (src.hasPermission(Permissions.TOGGLE_VOTE)) {
                        Config.voteEnabled = false;
                        Config.config.getNode("voting", "enabled").setValue("false");
                        return CommandResult.success();
                    } else {
                        return CommandResult.empty();
                    }

                case "yes":
                    if (plugin.hasVoted.contains(src)) {
                        throw new CommandException(plugin.fromLegacy(Messages.getErrorAlreadyVoted()));
                    }
                    if (plugin.voteStarted) {
                        plugin.yesVotes += 1;
                        if (src instanceof Player) {
                            plugin.hasVoted.add((Player) src);
                        }
                        plugin.displayVotes();
                        plugin.sendMessage(src, Messages.getVotedYes());
                        return CommandResult.success();
                    } else {
                        throw new CommandException(plugin.fromLegacy(Messages.getErrorNoVoteRunning()));
                    }

                case "no":
                    if (plugin.hasVoted.contains(src)) {
                        throw new CommandException(plugin.fromLegacy(Messages.getErrorAlreadyVoted()));
                    }
                    if (plugin.voteStarted) {
                        plugin.noVotes += 1;
                        if (src instanceof Player) {
                            plugin.hasVoted.add((Player) src);
                        }
                        plugin.displayVotes();
                        plugin.sendMessage(src, Messages.getVotedNo());
                        return CommandResult.success();

                    } else {
                        throw new CommandException(plugin.fromLegacy(Messages.getErrorNoVoteRunning()));
                    }

                default:
                    return CommandResult.empty();
                    //break;

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

            if (!src.hasPermission(Permissions.BYPASS) && !src.hasPermission(Permissions.COMMAND_VOTE)) {
                throw new CommandException(plugin.fromLegacy(Messages.getErrorNoPermission()));
            }
            if (!src.hasPermission(Permissions.BYPASS) && !Config.voteEnabled) {
                throw new CommandException(plugin.fromLegacy(Messages.getErrorVoteToRestartDisabled()));
            }
            if (plugin.hasVoted.contains(src)) {
                throw new CommandException(plugin.fromLegacy(Messages.getErrorAlreadyVoted()));
            }
            if (plugin.voteStarted) {
                throw new CommandException(plugin.fromLegacy(Messages.getErrorVoteAlreadyRunning()));
            }
            if (!src.hasPermission(Permissions.BYPASS) && plugin.justStarted) {
                throw new CommandException(plugin.fromLegacy(Messages.getErrorNotOnlineLongEnough()));
            }
            if (!src.hasPermission(Permissions.BYPASS) && Sponge.getServer().getOnlinePlayers().size() < Config.timerMinplayers) {
                throw new CommandException(plugin.fromLegacy(Messages.getErrorMinPlayers()));
            }
            if (plugin.isRestarting && timeLeft != 0 && (hours == 0 && minutes <= 10)) {
                throw new CommandException(plugin.fromLegacy(Messages.getErrorAlreadyRestarting()));
            }
            if (plugin.cdTimer == 1) {
                throw new CommandException(plugin.fromLegacy(Messages.getErrorWaitTime()));
            }


            if (src instanceof Player) {
                Player player = (Player) src;
                plugin.voteStarted = true;
                plugin.voteCancel = 0;
                plugin.hasVoted.add(player);
                plugin.yesVotes += 1;
                plugin.noVotes = 0;
                plugin.voteSeconds = 90;
                plugin.displayVotes();
            } else {
                plugin.voteStarted = true;
                plugin.displayVotes();
            }

            PaginationService paginationService = Sponge.getServiceManager().provide(PaginationService.class).get();
            List<Text> contents = new ArrayList<>();
            List<String> broadcast = Messages.getRestartVoteBroadcast();
            if (broadcast != null) {
                for (String line : broadcast) {
                    String checkLine = line.replace("%playername$", src.getName()).replace("%config.timerminplayers%", String.valueOf(Config.timerMinplayers));
                    contents.add(plugin.fromLegacy(checkLine));
                }
            }

            if (!contents.isEmpty()) {
                paginationService.builder()
                        .title(plugin.fromLegacy("Restart"))
                        .contents(contents)
                        .padding(Text.of("="))
                        .sendTo(MessageChannel.TO_ALL.getMembers());
            }

            Timer voteTimer = new Timer();
            voteTimer.schedule(new TimerTask() {
                public void run() {
                    int Online = Sponge.getServer().getOnlinePlayers().size();
                    float percentage = plugin.yesVotes/Online *100;

                    if ((plugin.yesVotes > plugin.noVotes) && (plugin.voteCancel == 0) && (plugin.yesVotes >= Config.timerMinplayers) && (percentage >= Config.timerVotepercent)) {

                        plugin.removeScoreboard();
                        plugin.removeBossBar();
                        plugin.yesVotes = 0;
                        plugin.cdTimer = 1;
                        plugin.voteStarted = false;
                        plugin.hasVoted.clear();
                        plugin.isRestarting = true;
                        Config.restartInterval = (Config.timerVotepassed + 1) / 3600.0;
                        plugin.logger.info("[MMCReboot] scheduling restart tasks...");
                        plugin.reason = Messages.getRestartPassed();
                        plugin.scheduleTasks();
                    } else {
                        if (plugin.voteCancel == 0) {
                            plugin.broadcastMessage("&f[&6Restart&f] " + Messages.getRestartVoteNotEnoughVoted());
                        }
                        plugin.yesVotes = 0;
                        plugin.cdTimer = 1;
                        plugin.voteCancel = 0;
                        plugin.voteStarted = false;
                        plugin.removeScoreboard();
                        plugin.removeBossBar();
                        plugin.hasVoted.clear();
                        Timer voteTimer = new Timer();
                        voteTimer.schedule(new TimerTask() {
                            public void run() {
                                plugin.cdTimer = 0;
                            }
                        }, (long) (Config.timerRevote * 60000.0));
                    }
                }
            }, 90000);
            return CommandResult.success();
        }
    }
}
