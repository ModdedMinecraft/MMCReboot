package net.moddedminecraft.mmcreboot.Tasks;

import net.moddedminecraft.mmcreboot.Config.Config;
import net.moddedminecraft.mmcreboot.Main;
import org.spongepowered.api.scheduler.Task;

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
                plugin.removeBossBar();
                plugin.isRestarting = false;
                plugin.setTPSRestarting(false);
                if (!Config.tpsRestartCancelMsg.isEmpty()) {
                    plugin.broadcastMessage("&f[&6Restart&f] " + Config.tpsRestartCancelMsg);
                }
            } else if (plugin.getTPS() < Config.tpsMinimum) {
                if (Config.restartUseCommand) {
                    Task.builder()
                            .execute(() -> {
                                plugin.cancelTasks();
                                plugin.removeScoreboard();
                                plugin.removeBossBar();
                                plugin.useCommandOnRestart();
                            }).submit(plugin);
                } else {
                    Task.builder()
                            .execute(t -> plugin.stopServer())
                            .submit(plugin);
                }
            }
        } else {
            if (Config.restartUseCommand) {
                Task.builder()
                        .execute(() -> {
                            plugin.cancelTasks();
                            plugin.removeScoreboard();
                            plugin.removeBossBar();
                            plugin.useCommandOnRestart();
                        }).submit(plugin);
            } else {
                Task.builder()
                        .execute(t -> plugin.stopServer())
                        .submit(plugin);
            }
        }
    }
}
