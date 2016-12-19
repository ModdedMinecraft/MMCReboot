package net.moddedminecraft.mmcreboot;

import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.filter.cause.Root;
import org.spongepowered.api.event.network.ClientConnectionEvent;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class EventListener {

    private final Main plugin;
    public EventListener(Main instance) {
        plugin = instance;
    }

    @Listener
    public void onPlayerLogin(ClientConnectionEvent.Join event, @Root Player player) throws IOException, ObjectMappingException {
        if (plugin.voteStarted) {
            Sponge.getScheduler().createTaskBuilder().execute(new Runnable() {
                public void run() {
                    plugin.displayVotes();
                    plugin.sendMessage(player, "&f[&6Restart-Vote&f] &3There is a vote to restart the server.");
                    plugin.sendMessage(player, "&6Type &a/reboot vote yes &6if you agree");
                    plugin.sendMessage(player, "&6Type &c/reboot vote no &6if you do not agree");
                    plugin.sendMessage(player, "&6If there are more yes votes than no, The server will be restarted! (minimum of " + Config.timerMinplayers + ")");
                }
            }).delay(10, TimeUnit.SECONDS).name("mmcreboot-s-sendVoteOnLogin").submit(this);
        }
    }

}
