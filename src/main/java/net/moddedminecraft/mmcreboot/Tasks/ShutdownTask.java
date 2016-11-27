package net.moddedminecraft.mmcreboot.Tasks;

import net.moddedminecraft.mmcreboot.Main;

import java.util.TimerTask;

public class ShutdownTask extends TimerTask {

    private final Main plugin;

    public ShutdownTask(Main instance) {
        plugin = instance;
    }

    @Override
    public void run() {
        plugin.stopServer();

    }
}
