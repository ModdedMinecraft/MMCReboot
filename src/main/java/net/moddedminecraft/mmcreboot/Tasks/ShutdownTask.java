package net.moddedminecraft.mmcreboot.Tasks;

import net.moddedminecraft.mmcreboot.Config.Config;
import net.moddedminecraft.mmcreboot.Main;

import java.util.TimerTask;

public class ShutdownTask extends TimerTask {

    private final Main plugin;

    public ShutdownTask(Main instance) {
        plugin = instance;
    }

    @Override
    public void run() {
        if (plugin.getTPSRestarting()) {
            if (plugin.getTPS() >= Config.tpsMinimum && Config.tpsRestartCancel) {
                plugin.cancelTasks();
                plugin.removeScoreboard();
                plugin.isRestarting = false;
                plugin.setTPSRestarting(false);
                if (!Config.tpsRestartCancelMsg.isEmpty()) {
                    plugin.broadcastMessage("&f[&6Restart&f] " + Config.tpsRestartCancelMsg);
                }
            } else if (plugin.getTPS() < Config.tpsMinimum) {
                if (Config.restartUseCommand) {
                    plugin.cancelTasks();
                    plugin.removeScoreboard();
                    plugin.useCommandOnRestart();
                } else {
                    plugin.stopServer();
                }
            }
        } else {
            if (Config.restartUseCommand) {
                plugin.cancelTasks();
                plugin.removeScoreboard();
                plugin.useCommandOnRestart();
            } else {
                plugin.stopServer();
            }
        }
    }
}
