package net.moddedminecraft.mmcreboot.Config;

import com.google.common.reflect.TypeToken;
import net.moddedminecraft.mmcreboot.Main;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.hocon.HoconConfigurationLoader;
import ninja.leaping.configurate.loader.ConfigurationLoader;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import org.spongepowered.api.Sponge;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

public class Messages {

    private static Main plugin;

    private Path defaultMessage;

    private static ConfigurationLoader<CommentedConfigurationNode> messageLoader;
    private static CommentedConfigurationNode messages;

    public Messages(Main main) throws IOException, ObjectMappingException {
        plugin = main;
        String language = Config.language;
        defaultMessage = plugin.configDir.resolve("localization/messages_" + language + ".conf");
        if (Files.notExists(defaultMessage)) {
            plugin.logger.warn("Localization was not found");
        }
        messageLoader = HoconConfigurationLoader.builder().setPath(defaultMessage).build();
        messages = messageLoader.load();
        checkLangAssetFiles();
        messageCheck();
    }

    private static String[] restartVoteBroadcastDefault = {
            "&a%playername$ &bhas voted that the server should be restarted",
            "&6Type &a/reboot vote yes &6if you agree",
            "&6Type &c/reboot vote no &6if you do not agree",
            "&6If there are more yes votes than no, The server will be restarted! (minimum of %config.timerminplayers%)",
            "&bYou have &a90 &bseconds to vote!"
    };

    private static String[] restartVoteBroadcastOnLoginDefault = {
            "&3There is a vote to restart the server.",
            "&6Type &a/reboot vote yes &6if you agree",
            "&6Type &c/reboot vote no &6if you do not agree",
            "&6If there are more yes votes than no, The server will be restarted! (minimum of %config.timerminplayers%)"
    };

    private static String chatprefix = "&f[&6MMCReboot&f] ";

    //sidebar
    private static String sidebarTitle = "Restart Vote";
    private static String sidebarYes = "Yes";
    private static String sidebarNo = "No";
    private static String sidebarTimeleft = "Time Left";
    private static String sidebarRestartTimerTitle = "Restart Timer";

    //error
    private static String errorAlreadyVoted = "&4You have already voted!";
    private static String errorNoVoteRunning = "&4There is no vote running at the moment";
    private static String errorVoteToRestartDisabled = "&4Voting to restart is disabled";
    private static String errorVoteAlreadyRunning = "&4A vote is already running";
    private static String errorNotOnlineLongEnough = "&4The server needs to be online for %config.timerstartvote% minutes before starting a vote!";
    private static String errorMinPlayers = "&4There must be a minimum of %config.timerminplayers% players online to start a vote";
    private static String errorAlreadyRestarting = "&4The server is already restarting!";
    private static String errorWaitTime = "&4You need to wait %config.timerrevote% minutes before starting another vote!";
    private static String errorNoPermission = "&4You don't have permission to do this!";
    private static String errorNoTaskScheduled = "&cThere is no restart scheduled!";
    private static String errorTookTooLong = "&cYou took too long to confirm the reboot.";
    private static String errorInvalidTimescale = "&cInvalid time scale!";
    private static String errorNothingToConfirm = "&cThere is nothing to confirm.";

    //general
    private static String restartCancel = "&3Restarts have been canceled.";
    private static String restartPassed = "&aPlayers have voted to restart the server.";
    private static String restartVoteNotEnoughVoted = "&3The server will not be restarted. Not enough people have voted.";
    private static String votedYes = "You Voted Yes!";
    private static String votedNo = "You Voted No!";
    private static String restartMessageWithReason = "&3The server will now be restarting in &f%hours%h%minutes%m%seconds%s &3with the reason:";
    private static String restartMessageWithoutReason = "&3The server will now be restarting in &f%hours%h%minutes%m%seconds%s";
    private static String restartFormatMessage = "&bUse 'h' for time in hours, 'm' for minutes and 's' for seconds";
    private static String restartConfirm = "&cOk, you asked for it!";
    private static String restartConfirmMessage = "&cPlease type: &6/Reboot Confirm &cif you are sure you want to do this.";

    //vote notification
    private static List<String> restartVoteBroadcast;
    private static List<String> restartVoteBroadcastOnLogin;

    //restart notification
    private static String restartNotificationMinutes = "&bThe server will be restarting in &f%minutes%:%seconds% &bminutes";
    private static String restartNotificationMinute = "&bThe server will be restarting in &f%minutes% &bminute";
    private static String restartNotificationSeconds = "&bThe server will be restarting in &f%seconds% &bseconds";

    //help
    private static String helpHeader = "&3[] = required  () = optional";
    private static String helpHelp = "&3/reboot &bhelp - &7shows this help";
    private static String helpNow = "&3/reboot &bnow - &7restarts the server instantly";
    private static String helpStart = "&3/reboot start &7[&bh&7|&bm&7|&bs&7] &7[&btime&7] &7(&breason&7) &b- &7restart the server after a given time";
    private static String helpCancel = "&3/reboot &bcancel - &7cancel any current restart timer";
    private static String helpVote = "&3/reboot &bvote - &7starts a vote to restart the server";
    private static String helpTime = "&3/reboot &btime - &7informs you how much time is left before restarting";
    private static String helpVoteYea = "&3/reboot &bvote yes - &7vote yes to restart the server";
    private static String helpVoteNo = "&3/reboot &bvote no - &7vote no to restart the server";






    private void messageCheck() throws IOException, ObjectMappingException {

        //sidebar
        sidebarTitle = check(messages.getNode("sidebar", "vote", "title"), sidebarTitle).getString();
        sidebarYes = check(messages.getNode("sidebar", "vote", "yes"), sidebarYes).getString();
        sidebarNo = check(messages.getNode("sidebar", "vote", "no"), sidebarNo).getString();
        sidebarTimeleft = check(messages.getNode("sidebar", "restart", "time-left"), sidebarTimeleft).getString();
        sidebarRestartTimerTitle = check(messages.getNode("sidebar", "restart", "title"), sidebarRestartTimerTitle).getString();

        //error
        errorAlreadyVoted = check(messages.getNode("error", "already-voted"), errorAlreadyVoted).getString();
        errorNoVoteRunning = check(messages.getNode("error", "no-vote-running"), errorNoVoteRunning).getString();
        errorVoteToRestartDisabled = check(messages.getNode("error", "vote-restart-disabled"), errorVoteToRestartDisabled).getString();
        errorVoteAlreadyRunning = check(messages.getNode("error", "vote-already-running"), errorVoteAlreadyRunning).getString();
        errorNotOnlineLongEnough = check(messages.getNode("error", "not-online-long-enough"), errorNotOnlineLongEnough).getString();
        errorMinPlayers = check(messages.getNode("error", "min-players"), errorMinPlayers).getString();
        errorAlreadyRestarting = check(messages.getNode("error", "already-restarting"), errorAlreadyRestarting).getString();
        errorWaitTime = check(messages.getNode("error", "wait-time"), errorWaitTime).getString();
        errorNoPermission = check(messages.getNode("error", "no-permission"), errorNoPermission).getString();
        errorNoTaskScheduled = check(messages.getNode("error", "no-task-scheduled"), errorNoTaskScheduled).getString();
        errorTookTooLong = check(messages.getNode("error", "took-too-long"), errorTookTooLong).getString();
        errorInvalidTimescale = check(messages.getNode("error", "invalid-time-scale"), errorInvalidTimescale).getString();
        errorNothingToConfirm = check(messages.getNode("error", "nothing-to-confirm"), errorNothingToConfirm).getString();

        //general
        restartCancel = check(messages.getNode("general", "restart-canceled"), restartCancel).getString();
        restartPassed = check(messages.getNode("general", "restart-passed"), restartPassed).getString();
        restartVoteNotEnoughVoted = check(messages.getNode("general", "not-enough-voted"), restartVoteNotEnoughVoted).getString();
        votedYes = check(messages.getNode("general", "voted-yes"), votedYes).getString();
        votedNo = check(messages.getNode("general", "voted-no"), votedNo).getString();
        restartMessageWithReason = check(messages.getNode("general", "restart-with-reason"), restartMessageWithReason).getString();
        restartMessageWithoutReason = check(messages.getNode("general", "restart-no-reason"), restartMessageWithoutReason).getString();
        restartFormatMessage = check(messages.getNode("general", "restart-format"), restartFormatMessage).getString();
        restartConfirm = check(messages.getNode("general", "restart-confirmed"), restartConfirm).getString();
        restartConfirmMessage = check(messages.getNode("general", "confirm-restart"), restartConfirmMessage).getString();

        //vote notification
        restartVoteBroadcast = checkList(messages.getNode("vote-notification", "after-command"), restartVoteBroadcastDefault).getList(TypeToken.of(String.class));
        restartVoteBroadcastOnLogin = checkList(messages.getNode("vote-notification", "on-login"), restartVoteBroadcastOnLoginDefault).getList(TypeToken.of(String.class));

        //restart notification
        restartNotificationMinutes = check(messages.getNode("restart-notification", "more-than-1-minute-remaining"), restartNotificationMinutes).getString();
        restartNotificationMinute = check(messages.getNode("restart-notification", "only-1-minute-remaining"), restartNotificationMinute).getString();
        restartNotificationSeconds = check(messages.getNode("restart-notification", "less-than-1-minute-remaining"), restartNotificationSeconds).getString();

        //help
        helpHeader = check(messages.getNode("help", "header"), helpHeader).getString();
        helpHelp = check(messages.getNode("help", "help"), helpHelp).getString();
        helpNow = check(messages.getNode("help", "now"), helpNow).getString();
        helpStart = check(messages.getNode("help", "start"), helpStart).getString();
        helpCancel = check(messages.getNode("help", "cancel"), helpCancel).getString();
        helpVote = check(messages.getNode("help", "vote"), helpVote).getString();
        helpTime = check(messages.getNode("help", "time"), helpTime).getString();
        helpVoteYea = check(messages.getNode("help", "vote-yes"), helpVoteYea).getString();
        helpVoteNo = check(messages.getNode("help", "vote-no"), helpVoteNo).getString();

        messageLoader.save(messages);
    }

    private void checkLangAssetFiles() throws IOException {
        if (!Files.isDirectory(plugin.configDir.resolve("localization"))) {
            Files.createDirectory(plugin.configDir.resolve("localization"));
        }
        String[] assets = {
                "messages_EN.conf",
                "messages_RU.conf"
        };
        for (String asset : assets) {
            if (!Files.exists(plugin.configDir.resolve("localization/" +asset))) {
                if (Sponge.getAssetManager().getAsset(plugin, asset).isPresent()) {
                    Sponge.getAssetManager().getAsset(plugin, asset).get().copyToFile(plugin.configDir.resolve("localization/" +asset));
                }
            }
        }
    }

    private CommentedConfigurationNode check(CommentedConfigurationNode node, Object defaultValue) {
        if (node.isVirtual()) {
            node.setValue(defaultValue);
        }
        return node;
    }

    private CommentedConfigurationNode checkList(CommentedConfigurationNode node, String[] defaultValue) {
        if (node.isVirtual()) {
            node.setValue(Arrays.asList(defaultValue));
        }
        return node;
    }

    public static String getChatprefix() {
        return chatprefix;
    }

    public static String getRestartCancel() {
        return restartCancel;
    }

    public static String getSidebarTitle() {
        return sidebarTitle;
    }

    public static String getSidebarNo() {
        return sidebarNo;
    }

    public static String getSidebarYes() {
        return sidebarYes;
    }

    public static String getSidebarTimeleft() {
        return sidebarTimeleft;
    }

    public static String getSidebarRestartTimerTitle() {
        return sidebarRestartTimerTitle;
    }

    public static String getRestartMessageWithoutReason() {
        return restartMessageWithoutReason;
    }

    public static String getRestartMessageWithReason() {
        return restartMessageWithReason;
    }

    public static String getErrorInvalidTimescale() {
        return errorInvalidTimescale;
    }

    public static String getRestartFormatMessage() {
        return restartFormatMessage;
    }

    public static String getRestartConfirm() {
        return restartConfirm;
    }

    public static String getErrorNothingToConfirm() {
        return errorNothingToConfirm;
    }

    public static String getHelpCancel() {
        return helpCancel;
    }

    public static String getHelpHeader() {
        return helpHeader;
    }

    public static String getHelpHelp() {
        return helpHelp;
    }

    public static String getHelpVote() {
        return helpVote;
    }

    public static String getHelpNow() {
        return helpNow;
    }

    public static String getHelpStart() {
        return helpStart;
    }

    public static String getHelpTime() {
        return helpTime;
    }

    public static String getHelpVoteNo() {
        return helpVoteNo;
    }

    public static String getHelpVoteYea() {
        return helpVoteYea;
    }

    public static String getErrorTookTooLong() {
        return errorTookTooLong;
    }

    public static String getRestartConfirmMessage() {
        return restartConfirmMessage;
    }

    public static String getErrorNoTaskScheduled() {
        return errorNoTaskScheduled;
    }

    public static String getErrorNoPermission() {
        return errorNoPermission;
    }

    public static String getErrorAlreadyRestarting() {
        return errorAlreadyRestarting;
    }

    public static String getErrorAlreadyVoted() {
        return errorAlreadyVoted;
    }

    public static String getErrorMinPlayers() {
        return errorMinPlayers.replace("%config.timerminplayers%", String.valueOf(Config.timerMinplayers));
    }

    public static String getErrorNotOnlineLongEnough() {
        return errorNotOnlineLongEnough.replace("%config.timerstartvote%", String.valueOf(Config.timerStartvote));
    }

    public static String getErrorNoVoteRunning() {
        return errorNoVoteRunning;
    }

    public static String getErrorVoteAlreadyRunning() {
        return errorVoteAlreadyRunning;
    }

    public static String getErrorVoteToRestartDisabled() {
        return errorVoteToRestartDisabled;
    }

    public static String getErrorWaitTime() {
        return errorWaitTime.replace("%config.timerrevote%", String.valueOf(Config.timerRevote));
    }

    public static String getVotedNo() {
        return votedNo;
    }

    public static String getVotedYes() {
        return votedYes;
    }

    public static String getRestartVoteNotEnoughVoted() {
        return restartVoteNotEnoughVoted;
    }

    public static String getRestartPassed() {
        return restartPassed;
    }

    public static List<String> getRestartVoteBroadcast() {
        return restartVoteBroadcast;
    }

    public static List<String> getRestartVoteBroadcastOnLogin() {
        return restartVoteBroadcastOnLogin;
    }

    public static String getRestartNotificationMinute() {
        return restartNotificationMinute;
    }

    public static String getRestartNotificationMinutes() {
        return restartNotificationMinutes;
    }

    public static String getRestartNotificationSeconds() {
        return restartNotificationSeconds;
    }
}

