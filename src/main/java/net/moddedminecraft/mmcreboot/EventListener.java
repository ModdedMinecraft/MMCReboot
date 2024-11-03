package net.moddedminecraft.mmcreboot;

import net.kyori.adventure.text.Component;
import net.moddedminecraft.mmcreboot.Config.Config;
import net.moddedminecraft.mmcreboot.Config.Messages;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.network.ServerSideConnectionEvent;
import org.spongepowered.api.service.pagination.PaginationService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class EventListener {

    private final Main plugin;
    public EventListener(Main instance) {
        plugin = instance;
    }

    @Listener
    public void onPlayerLogin(ServerSideConnectionEvent.Join event) throws IOException {
        ServerPlayer player = event.player();
        if (plugin.voteStarted) {
            Sponge.asyncScheduler().executor(plugin.container).schedule(new Runnable() {

                public void run() {
                    PaginationService paginationService = Sponge.serviceProvider().provide(PaginationService.class).get();
                    List<Component> contents = new ArrayList<>();
                    if  (Config.timerUseVoteScoreboard) {
                        plugin.displayVotes();
                    }
                    for (String line : Messages.getRestartVoteBroadcastOnLogin()) {
                        String checkLine = line.replace("%config.timerminplayers%", String.valueOf(Config.timerMinplayers));
                        contents.add(plugin.fromLegacy(checkLine));
                    }

                    if (!contents.isEmpty()) {
                        paginationService.builder()
                                .title(plugin.fromLegacy("Restart"))
                                .contents(contents)
                                .padding(Component.text("="))
                                .sendTo(player);
                    }
                }
            },3 , TimeUnit.SECONDS);
        }
    }

}
