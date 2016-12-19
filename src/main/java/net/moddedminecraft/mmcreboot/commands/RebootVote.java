package net.moddedminecraft.mmcreboot.commands;

import net.moddedminecraft.mmcreboot.Config;
import net.moddedminecraft.mmcreboot.Main;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.entity.living.player.Player;

import java.util.Optional;
import java.util.Timer;
import java.util.TimerTask;

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
            if (op.equals("on") && src.hasPermission("mmcreboot.reboot.toggle.vote")) {
                Config.voteEnabled = true;
                Config.config.getNode("voting", "enabled").setValue("true");
                return CommandResult.success();
            }
            if (op.equals("off") && src.hasPermission("mmcreboot.reboot.toggle.vote")) {
                Config.voteEnabled = false;
                Config.config.getNode("voting", "enabled").setValue("false");
                return CommandResult.success();
            }
            if (op.equals("yes")) {
                if (plugin.hasVoted.contains(src)) {
                    throw new CommandException(plugin.fromLegacy("&4You have already voted!"));
                } else if (plugin.voteStarted) {
                    plugin.yesVotes += 1;
                    if (src instanceof Player) {
                        plugin.hasVoted.add((Player) src);
                    }
                    plugin.displayVotes();
                    plugin.sendMessage(src, "You Voted Yes!");
                    return CommandResult.success();
                } else {
                    throw new CommandException(plugin.fromLegacy("&4There is no vote running at the moment"));
                }
            }
            if (op.equals("no")) {
                if (plugin.hasVoted.contains(src)) {
                    throw new CommandException(plugin.fromLegacy("&4You have already voted!"));
                } else if (plugin.voteStarted) {
                    plugin.noVotes += 1;
                    if (src instanceof Player) {
                        plugin.hasVoted.add((Player) src);
                    }
                    plugin.displayVotes();
                    plugin.sendMessage(src, "You Voted No!");
                    return CommandResult.success();

                } else {
                    throw new CommandException(plugin.fromLegacy("&4There is no vote running at the moment"));
                }
            }
        } else {
            double timeLeft = (Config.restartInterval * 3600) - ((double) (System.currentTimeMillis() - plugin.startTimestamp) / 1000);
            int hours = (int) (timeLeft / 3600);
            int minutes = (int) ((timeLeft - hours * 3600) / 60);

            if (!src.hasPermission("mmcreboot.reboot.bypass") && Sponge.getServer().getOnlinePlayers().size() < Config.timerMinplayers) {
                throw new CommandException(plugin.fromLegacy("&4There must be a minimum of " + Config.timerMinplayers + " players online to start a vote"));
            } else {
                if (plugin.isRestarting && minutes <= 10) {
                    throw new CommandException(plugin.fromLegacy("&4The server is already restarting!"));
                } else {
                    if (!src.hasPermission("mmcreboot.reboot.bypass") && plugin.justStarted) {
                        throw new CommandException(plugin.fromLegacy("&4The server needs to be online for " + Config.timerStartvote + " minutes before starting a vote!"));
                    } else {
                        if (!src.hasPermission("mmcreboot.reboot.bypass") && !Config.voteEnabled) {
                            throw new CommandException(plugin.fromLegacy("&4Voting to restart is disabled"));
                        } else {
                            if (plugin.cdTimer == 1) {
                                throw new CommandException(plugin.fromLegacy("&4You need to wait " + Config.timerRevote + " minutes before starting another vote!"));
                            } else if (plugin.hasVoted.contains(src)) {
                                throw new CommandException(plugin.fromLegacy("&4You have already voted!"));
                            } else {
                                if (!src.hasPermission("mmcreboot.reboot.bypass") && !src.hasPermission("mmcreboot.reboot.vote")) {
                                    throw new CommandException(plugin.fromLegacy("&4You don't have permission to do this!"));
                                } else {
                                    if (plugin.voteStarted) {
                                        throw new CommandException(plugin.fromLegacy("&4A vote is already running"));
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


                                    plugin.broadcastMessage("&3---------- Restart ----------");
                                    plugin.broadcastMessage("&a" + src.getName() + " &bhas voted that the server should be restarted");
                                    plugin.broadcastMessage("&6Type &a/reboot vote yes &6if you agree");
                                    plugin.broadcastMessage("&6Type &c/reboot vote no &6if you do not agree");
                                    plugin.broadcastMessage("&6If there are more yes votes than no, The server will be restarted! (minimum of 5)");
                                    plugin.broadcastMessage("&bYou have &a90 &bseconds to vote!");
                                    plugin.broadcastMessage("&3----------------------------");

                                    Timer voteTimer = new Timer();
                                    voteTimer.schedule(new TimerTask() {
                                        public void run() {
                                            if ((plugin.yesVotes > plugin.noVotes) && plugin.voteCancel == 0 && plugin.yesVotes >= 5) {
                                                plugin.removeScoreboard();
                                                plugin.yesVotes = 0;
                                                plugin.cdTimer = 1;
                                                plugin.voteStarted = false;
                                                plugin.hasVoted.clear();
                                                plugin.isRestarting = true;
                                                Config.restartInterval = (Config.timerVotepassed + 1) / 3600.0;
                                                plugin.logger.info("[MMCReboot] scheduling restart tasks...");
                                                plugin.usingReason = 1;
                                                plugin.reason = "Players have voted to restart the server.";
                                                plugin.scheduleTasks();
                                            } else {
                                                if (plugin.voteCancel == 0) {
                                                    plugin.broadcastMessage("&f[&6Restart&f] &3The server will not be restarted. Not enough people have voted.");
                                                }
                                                plugin.yesVotes = 0;
                                                plugin.cdTimer = 1;
                                                plugin.voteCancel = 0;
                                                plugin.voteStarted = false;
                                                plugin.usingReason = 0;
                                                plugin.removeScoreboard();
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
                                }

                            }
                        }
                    }
                }
            }
        }
        return CommandResult.success();
    }
}
