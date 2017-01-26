package net.moddedminecraft.mmcreboot.Tasks;

import net.moddedminecraft.mmcreboot.Config;
import net.moddedminecraft.mmcreboot.Main;

import java.util.TimerTask;

public class ShutdownTask extends TimerTask {

    private final Main plugin;

    public ShutdownTask(Main instance) {
        plugin = instance;
    }

    @Override
    public void run() {
        if (Config.tpsEnabled) {
            if (plugin.getTPS() < Config.tpsMinimum && Config.tpsRestartCancel) {
                if (Config.restartUseCommand) {
                    plugin.cancelTasks();
                    plugin.removeScoreboard();
                    plugin.useCommandOnRestart();
                    if (!Config.tpsRestartCancelMsg.isEmpty()) {
                        plugin.broadcastMessage("&f[&6Restart&f] " + Config.tpsRestartCancelMsg);
                    }
                } else {
                    plugin.stopServer();
                }
            } else {
                plugin.cancelTasks();
                plugin.removeScoreboard();
                plugin.isRestarting = false;
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
