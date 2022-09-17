package net.moddedminecraft.mmcreboot;

import net.moddedminecraft.mmcreboot.Config.Config;
import net.moddedminecraft.mmcreboot.Config.Messages;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.filter.cause.Root;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import org.spongepowered.api.service.pagination.PaginationList;
import org.spongepowered.api.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class EventListener {

    private final Main plugin;
    public EventListener(Main instance) {
        plugin = instance;
    }

    @Listener
    public void onPlayerLogin(ClientConnectionEvent.Join event, @Root Player player) {
        if (plugin.voteStarted) {
            Sponge.getScheduler().createTaskBuilder().execute(() -> {
                    List<Text> contents = new ArrayList<>();
                    if  (Config.timerUseVoteScoreboard) {
                        plugin.displayVotes();
                    }
                    for (String line : Messages.getRestartVoteBroadcastOnLogin()) {
                        String checkLine = line.replace("%config.timerminplayers%", String.valueOf(Config.timerMinplayers));
                        contents.add(plugin.fromLegacy(checkLine));
                    }

                    if (!contents.isEmpty()) {
                        PaginationList.builder()
                                .title(plugin.fromLegacy("Restart"))
                                .contents(contents)
                                .padding(Text.of("="))
                                .sendTo(player);
                    }
            }).delay(3, TimeUnit.SECONDS).name("mmcreboot-s-sendVoteOnLogin").submit(plugin);
        }
    }

}
