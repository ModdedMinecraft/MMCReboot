package net.moddedminecraft.mmcreboot.Config;

import net.moddedminecraft.mmcreboot.Main;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.hocon.HoconConfigurationLoader;
import org.spongepowered.configurate.loader.ConfigurationLoader;
import org.spongepowered.configurate.serialize.SerializationException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

public class Messages {

    private static Main plugin;

    private Path defaultMessage;

    private static ConfigurationLoader<CommentedConfigurationNode> messageLoader;
    private static CommentedConfigurationNode messages;

    public Messages(Main main) throws IOException {
        plugin = main;
        String language = Config.language;
        defaultMessage = plugin.configDir.resolve("localization/messages_" + language + ".conf");
        if (Files.notExists(defaultMessage)) {
            plugin.logger.warn("Localization was not found");
        }
        messageLoader = HoconConfigurationLoader.builder().path(defaultMessage).build();
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
    private static String restartPassed = "Players have voted to restart the server.";
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






    private void messageCheck() throws IOException {

        //sidebar
        sidebarTitle = check(messages.node("sidebar", "vote", "title"), sidebarTitle).getString();
        sidebarYes = check(messages.node("sidebar", "vote", "yes"), sidebarYes).getString();
        sidebarNo = check(messages.node("sidebar", "vote", "no"), sidebarNo).getString();
        sidebarTimeleft = check(messages.node("sidebar", "restart", "time-left"), sidebarTimeleft).getString();
        sidebarRestartTimerTitle = check(messages.node("sidebar", "restart", "title"), sidebarRestartTimerTitle).getString();

        //error
        errorAlreadyVoted = check(messages.node("error", "already-voted"), errorAlreadyVoted).getString();
        errorNoVoteRunning = check(messages.node("error", "no-vote-running"), errorNoVoteRunning).getString();
        errorVoteToRestartDisabled = check(messages.node("error", "vote-restart-disabled"), errorVoteToRestartDisabled).getString();
        errorVoteAlreadyRunning = check(messages.node("error", "vote-already-running"), errorVoteAlreadyRunning).getString();
        errorNotOnlineLongEnough = check(messages.node("error", "not-online-long-enough"), errorNotOnlineLongEnough).getString();
        errorMinPlayers = check(messages.node("error", "min-players"), errorMinPlayers).getString();
        errorAlreadyRestarting = check(messages.node("error", "already-restarting"), errorAlreadyRestarting).getString();
        errorWaitTime = check(messages.node("error", "wait-time"), errorWaitTime).getString();
        errorNoPermission = check(messages.node("error", "no-permission"), errorNoPermission).getString();
        errorNoTaskScheduled = check(messages.node("error", "no-task-scheduled"), errorNoTaskScheduled).getString();
        errorTookTooLong = check(messages.node("error", "took-too-long"), errorTookTooLong).getString();
        errorInvalidTimescale = check(messages.node("error", "invalid-time-scale"), errorInvalidTimescale).getString();
        errorNothingToConfirm = check(messages.node("error", "nothing-to-confirm"), errorNothingToConfirm).getString();

        //general
        restartCancel = check(messages.node("general", "restart-canceled"), restartCancel).getString();
        restartPassed = check(messages.node("general", "restart-passed"), restartPassed).getString();
        restartVoteNotEnoughVoted = check(messages.node("general", "not-enough-voted"), restartVoteNotEnoughVoted).getString();
        votedYes = check(messages.node("general", "voted-yes"), votedYes).getString();
        votedNo = check(messages.node("general", "voted-no"), votedNo).getString();
        restartMessageWithReason = check(messages.node("general", "restart-with-reason"), restartMessageWithReason).getString();
        restartMessageWithoutReason = check(messages.node("general", "restart-no-reason"), restartMessageWithoutReason).getString();
        restartFormatMessage = check(messages.node("general", "restart-format"), restartFormatMessage).getString();
        restartConfirm = check(messages.node("general", "restart-confirmed"), restartConfirm).getString();
        restartConfirmMessage = check(messages.node("general", "confirm-restart"), restartConfirmMessage).getString();

        //vote notification
        restartVoteBroadcast = checkList(messages.node("vote-notification", "after-command"), restartVoteBroadcastDefault).getList(String.class);
        restartVoteBroadcastOnLogin = checkList(messages.node("vote-notification", "on-login"), restartVoteBroadcastOnLoginDefault).getList(String.class);

        //restart notification
        restartNotificationMinutes = check(messages.node("restart-notification", "more-than-1-minute-remaining"), restartNotificationMinutes).getString();
        restartNotificationMinute = check(messages.node("restart-notification", "only-1-minute-remaining"), restartNotificationMinute).getString();
        restartNotificationSeconds = check(messages.node("restart-notification", "less-than-1-minute-remaining"), restartNotificationSeconds).getString();

        //help
        helpHeader = check(messages.node("help", "header"), helpHeader).getString();
        helpHelp = check(messages.node("help", "help"), helpHelp).getString();
        helpNow = check(messages.node("help", "now"), helpNow).getString();
        helpStart = check(messages.node("help", "start"), helpStart).getString();
        helpCancel = check(messages.node("help", "cancel"), helpCancel).getString();
        helpVote = check(messages.node("help", "vote"), helpVote).getString();
        helpTime = check(messages.node("help", "time"), helpTime).getString();
        helpVoteYea = check(messages.node("help", "vote-yes"), helpVoteYea).getString();
        helpVoteNo = check(messages.node("help", "vote-no"), helpVoteNo).getString();

        messageLoader.save(messages);
    }

    private void checkLangAssetFiles() throws IOException {
        if (!Files.isDirectory(plugin.configDir.resolve("localization"))) {
            Files.createDirectory(plugin.configDir.resolve("localization"));
        }
        String[] assets = {
                "messages_EN.conf",
                "messages_RU.conf",
                "messages_ZH-CN.conf"
        };
        for (String asset : assets) {
            if (!Files.exists(plugin.configDir.resolve("localization/" +asset))) {
                InputStream stream = Main.class.getClassLoader().getResourceAsStream(asset);
                if (stream != null) {
                    Files.copy(stream, plugin.configDir.resolve("localization/" +asset));
                }

            }
        }
    }

    private CommentedConfigurationNode check(CommentedConfigurationNode node, Object defaultValue) throws SerializationException {
        if (node.virtual()) {
            node.set(defaultValue);
        }
        return node;
    }

    private CommentedConfigurationNode checkList(CommentedConfigurationNode node, String[] defaultValue) throws SerializationException {
        if (node.virtual()) {
            node.set(Arrays.asList(defaultValue));
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

