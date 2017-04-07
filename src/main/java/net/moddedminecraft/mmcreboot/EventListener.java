package net.moddedminecraft.mmcreboot;

import net.moddedminecraft.mmcreboot.Config.Config;
import net.moddedminecraft.mmcreboot.Config.Messages;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.filter.cause.Root;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import org.spongepowered.api.service.pagination.PaginationService;
import org.spongepowered.api.text.Text;

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
    public void onPlayerLogin(ClientConnectionEvent.Join event, @Root Player player) throws IOException, ObjectMappingException {
        if (plugin.voteStarted) {
            Sponge.getScheduler().createTaskBuilder().execute(new Runnable() {

                public void run() {
                    PaginationService paginationService = Sponge.getServiceManager().provide(PaginationService.class).get();
                    List<Text> contents = new ArrayList<>();
                    plugin.displayVotes();
                    for (String line : Messages.getRestartVoteBroadcastOnLogin()) {
                        contents.add(plugin.fromLegacy(line.replace("%config.timerminplayers%", String.valueOf(Config.timerMinplayers))));
                    }

                    if (!contents.isEmpty()) {
                        paginationService.builder()
                                .title(plugin.fromLegacy("Restart"))
                                .contents(contents)
                                .padding(Text.of("="))
                                .sendTo(player);
                    }
                }
            }).delay(10, TimeUnit.SECONDS).name("mmcreboot-s-sendVoteOnLogin").submit(this);
        }
    }

}
