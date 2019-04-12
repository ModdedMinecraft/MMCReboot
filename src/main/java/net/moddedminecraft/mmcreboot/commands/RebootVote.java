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
import org.spongepowered.api.service.pagination.PaginationList;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.channel.MessageChannel;

import java.util.*;

public class RebootVote implements CommandExecutor {

    private final String noPermission = "&cYou do not have permission to use this command!";
    private final String commandSuccess = "&fReboot vote has successfully been {toggle}";

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
                            src.sendMessage(plugin.fromLegacy(commandSuccess.replace("{toggle}", "&aenabled")));
                            return CommandResult.success();
                        } else {
                            throw new CommandException(plugin.fromLegacy(noPermission));
                        }

                    case "off":
                        if (src.hasPermission(Permissions.TOGGLE_VOTE)) {
                            Config.voteEnabled = false;
                            Config.config.getNode("voting", "enabled").setValue("false");
                            src.sendMessage(plugin.fromLegacy(commandSuccess.replace("{toggle}", "&cdisabled")));
                            return CommandResult.success();
                        } else {
                            throw new CommandException(plugin.fromLegacy(noPermission));
                        }
                    case "yes":
                        if (src instanceof Player) {
                            Player player = (Player) src;
                            if (plugin.hasVoted.contains(player.getUniqueId())) {
                                throw new CommandException(plugin.fromLegacy(Messages.getErrorAlreadyVoted()));
                            }
                            if (plugin.voteStarted) {
                                plugin.yesVotes += 1;
                                plugin.hasVoted.add(((Player) src).getUniqueId());
                                plugin.displayVotes();
                                plugin.sendMessage(src, Messages.getVotedYes());
                                return CommandResult.success();
                            } else {
                                throw new CommandException(plugin.fromLegacy(Messages.getErrorNoVoteRunning()));
                            }
                        } else {
                            throw new CommandException(plugin.fromLegacy("&cYou must be a player to vote!"));
                        }
                    case "no":
                        if (src instanceof Player) {
                            Player player = (Player) src;
                            if (plugin.hasVoted.contains(player.getUniqueId())) {
                                throw new CommandException(plugin.fromLegacy(Messages.getErrorAlreadyVoted()));
                            }
                            if (plugin.voteStarted) {
                                plugin.noVotes += 1;
                                plugin.hasVoted.add(((Player) src).getUniqueId());
                                plugin.displayVotes();
                                plugin.sendMessage(src, Messages.getVotedNo());
                                return CommandResult.success();

                            } else {
                                throw new CommandException(plugin.fromLegacy(Messages.getErrorNoVoteRunning()));
                            }
                        } else {
                            throw new CommandException(plugin.fromLegacy("&cYou must be a player to vote!"));
                        }
                    case "cancel":
                        if (plugin.voteStarted && src.hasPermission(Permissions.CANCEL_VOTE)) {
                            plugin.removeScoreboard();
                            plugin.removeBossBar();
                            plugin.yesVotes = 0;
                            plugin.cdTimer = true;
                            plugin.voteStarted = false;
                            plugin.hasVoted.clear();
                            src.sendMessage(plugin.fromLegacy("Reboot vote has been successfully &ccancelled"));
                            return CommandResult.success();
                        }
                    default:
                        throw new CommandException(plugin.fromLegacy("&cInvalid Argument!"));
                }
            } else {
                double timeLeft = 0;
                if (Config.restartInterval > 0) {
                    timeLeft = (Config.restartInterval * 3600) - ((double) (System.currentTimeMillis() - plugin.startTimestamp) / 1000);
                } else if (plugin.nextRealTimeRestart > 0) {
                    timeLeft = (plugin.nextRealTimeRestart) - ((double) (System.currentTimeMillis() - plugin.startTimestamp) / 1000);
                }
                int hours = (int) (timeLeft / 3600);
                int minutes = (int) ((timeLeft - hours * 3600) / 60);
                int onlinePlayers = Sponge.getServer().getOnlinePlayers().size();

                if (!src.hasPermission(Permissions.BYPASS)) {
                    if (!src.hasPermission(Permissions.COMMAND_VOTE)) {
                        throw new CommandException(plugin.fromLegacy(Messages.getErrorNoPermission()));
                    }
                    if (!Config.voteEnabled) {
                        throw new CommandException(plugin.fromLegacy(Messages.getErrorVoteToRestartDisabled()));
                    }
                    if (plugin.justStarted) {
                        throw new CommandException(plugin.fromLegacy(Messages.getErrorNotOnlineLongEnough()));
                    }
                    if (onlinePlayers < Config.timerMinplayers) {
                        throw new CommandException(plugin.fromLegacy(Messages.getErrorMinPlayers()));
                    }
                }

                if (src instanceof Player && plugin.hasVoted.contains(((Player) src).getUniqueId())) {
                    throw new CommandException(plugin.fromLegacy(Messages.getErrorAlreadyVoted()));
                }
                if (plugin.voteStarted) {
                    throw new CommandException(plugin.fromLegacy(Messages.getErrorVoteAlreadyRunning()));
                }
                if (plugin.isRestarting && timeLeft != 0 && (hours == 0 && minutes <= 10)) {
                    throw new CommandException(plugin.fromLegacy(Messages.getErrorAlreadyRestarting()));
                }
                if (plugin.cdTimer) {
                    throw new CommandException(plugin.fromLegacy(Messages.getErrorWaitTime()));
                }


                if (src instanceof Player) {
                    Player player = (Player) src;
                    plugin.voteStarted = true;
                    plugin.voteCancel = false;
                    plugin.hasVoted.add(player.getUniqueId());
                    plugin.yesVotes += 1;
                    plugin.noVotes = 0;
                    plugin.voteSeconds = 90;
                    plugin.displayVotes();
                } else {
                    plugin.voteStarted = true;
                    plugin.displayVotes();
                }

                List<Text> contents = new ArrayList<>();

                List<String> broadcast = Messages.getRestartVoteBroadcast();
                if (broadcast != null) {
                    for (String line : broadcast) {
                        String checkLine = line.replace("%playername$", src.getName()).replace("%config.timerminplayers%", String.valueOf(Config.timerMinplayers));
                        contents.add(plugin.fromLegacy(checkLine));
                    }
                }

                if (!contents.isEmpty()) {
                    PaginationList.builder()
                            .title(plugin.fromLegacy("Restart"))
                            .contents(contents)
                            .padding(Text.of("="))
                            .sendTo(MessageChannel.TO_ALL.getMembers());
                }

                Timer voteTimer = new Timer();
                voteTimer.schedule(new TimerTask() {
                    public void run() {
                        int Online = Sponge.getServer().getOnlinePlayers().size();
                        int percentage = plugin.yesVotes / Online * 100;

                        final String prefix = "&f[&6Restart&f] ";

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
                                plugin.broadcastMessage(prefix + Messages.getRestartVoteNotEnoughVoted());
                            }
                            plugin.voteCancel = false;
                        }

                        plugin.removeScoreboard();
                        plugin.removeBossBar();
                        plugin.yesVotes = 0;
                        plugin.cdTimer = true;
                        plugin.voteStarted = false;
                        plugin.hasVoted.clear();
                        Timer voteTimer = new Timer();
                        voteTimer.schedule(new TimerTask() {
                            public void run() {
                                plugin.cdTimer = false;
                            }
                        }, (long) (Config.timerRevote * 60000.0));
                    }
                }, 90000);
                return CommandResult.success();
            }
    }
}
