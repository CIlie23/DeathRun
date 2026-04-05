package pl.mrstudios.deathrun.arena.listener;

import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import org.bukkit.Server;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import pl.mrstudios.commons.bukkit.item.ItemBuilder;
import pl.mrstudios.commons.inject.annotation.Inject;
import pl.mrstudios.deathrun.api.arena.event.user.UserArenaCheckpointEvent;
import pl.mrstudios.deathrun.api.arena.event.user.UserArenaFinishedEvent;
import pl.mrstudios.deathrun.api.arena.user.IUser;
import pl.mrstudios.deathrun.arena.Arena;
import pl.mrstudios.deathrun.arena.ArenaManager;
import pl.mrstudios.deathrun.config.Configuration;
import pl.mrstudios.deathrun.config.impl.MapConfiguration;

import java.util.Objects;

import static java.lang.String.valueOf;
import static java.time.Duration.ofMillis;
import static java.time.Duration.ofSeconds;
import static java.util.Optional.ofNullable;
import static net.kyori.adventure.text.minimessage.MiniMessage.miniMessage;
import static net.kyori.adventure.title.Title.Times.times;
import static net.kyori.adventure.title.Title.title;
import static org.bukkit.GameMode.ADVENTURE;
import static org.bukkit.Material.NETHER_PORTAL;
import static org.bukkit.Material.RED_BED;
import static org.bukkit.event.EventPriority.MONITOR;
import static org.bukkit.inventory.ItemFlag.values;
import static pl.mrstudios.deathrun.api.arena.user.enums.Role.RUNNER;
import static pl.mrstudios.deathrun.api.arena.user.enums.Role.SPECTATOR;

public class ArenaCheckpointReachedListener implements Listener {

        private final ArenaManager arenaManager;
    private final Plugin plugin;
    private final Server server;
    private final BukkitAudiences audiences;
    private final Configuration configuration;

    @Inject
    public ArenaCheckpointReachedListener(
                        @NotNull ArenaManager arenaManager,
            @NotNull Plugin plugin,
            @NotNull Server server,
            @NotNull BukkitAudiences audiences,
            @NotNull Configuration configuration
    ) {
                this.arenaManager = arenaManager;
        this.plugin = plugin;
        this.server = server;
        this.audiences = audiences;
        this.configuration = configuration;
    }

    @SuppressWarnings("deprecation")
    @EventHandler(priority = MONITOR)
    public void onPlayerMove(
            @NotNull PlayerMoveEvent event
    ) {

        if (event.getTo().getBlock().getType() != NETHER_PORTAL)
            return;

        if (
                event.getFrom().getBlockX() == event.getTo().getBlockX()
                        && event.getFrom().getBlockY() == event.getTo().getBlockY()
                        && event.getFrom().getBlockZ() == event.getTo().getBlockZ()
                        && event.getFrom().getPitch() != event.getTo().getPitch()
                        && event.getFrom().getYaw() != event.getTo().getYaw()
        ) return;

        Arena arena = this.arenaManager.arenaForPlayer(event.getPlayer());
        MapConfiguration.MapDefinition map = this.arenaManager.mapForPlayer(event.getPlayer());
        if (arena == null || map == null)
            return;

        map.arenaCheckpoints.stream()
                .filter((checkpoint) -> checkpoint.locations().stream().anyMatch(
                        (location) -> location.getBlockX() == event.getTo().getBlockX() && location.getBlockY() == event.getTo().getBlockY() && location.getBlockZ() == event.getTo().getBlockZ()
                )).findFirst()
                .ifPresent((checkpoint) -> ofNullable(arena.getUser(event.getPlayer()))
                                .filter((user) -> user.getRole() == RUNNER)
                                .filter((user) -> user.getCheckpoint().id() < checkpoint.id())
                                .ifPresent((user) -> {

                                    UserArenaCheckpointEvent userArenaCheckpointEvent = new UserArenaCheckpointEvent(user, checkpoint);

                                    this.server.getPluginManager().callEvent(userArenaCheckpointEvent);
                                    if (event.isCancelled())
                                        return;

                                    user.setCheckpoint(checkpoint);
                                    this.audiences.player(event.getPlayer()).showTitle(
                                            title(
                                                    miniMessage().deserialize(
                                                            this.configuration.language().arenaCheckpointTitle
                                                                    .replace("<checkpoint>", valueOf(checkpoint.id()))
                                                    ),
                                                    miniMessage().deserialize(
                                                            this.configuration.language().arenaCheckpointSubtitle
                                                                    .replace("<checkpoint>", valueOf(checkpoint.id()))
                                                    ),
                                                    times(ofMillis(250), ofSeconds(3), ofMillis(250))
                                            )
                                    );

                                    event.getPlayer().playSound(event.getPlayer().getLocation(), this.configuration.plugin().arenaSoundCheckpointReached, 1, 1);
                                                                        if (!checkpoint.id().equals(map.arenaCheckpoints.get(map.arenaCheckpoints.size() - 1).id()))
                                        return;

                                                                        arena.setFinishedRuns(arena.getFinishedRuns() + 1);

                                                                        int position = arena.getFinishedRuns(), time = arena.getElapsedTime();
                                    this.server.getPluginManager().callEvent(
                                            new UserArenaFinishedEvent(user, time, position)
                                    );

                                    if (position == 1)
                                                                                if (arena.getRemainingTime() >= 60)
                                                                                        arena.setRemainingTime(60);

                                    this.audiences.player(event.getPlayer()).showTitle(
                                            title(
                                                    miniMessage().deserialize(
                                                            this.configuration.language().arenaFinishTitle
                                                                    .replace("<position>", valueOf(position))
                                                                    .replace("<seconds>", valueOf(time))
                                                    ),
                                                    miniMessage().deserialize(
                                                            this.configuration.language().arenaFinishSubtitle
                                                                    .replace("<position>", valueOf(position))
                                                                    .replace("<seconds>", valueOf(time))
                                                    ),
                                                    times(ofMillis(250), ofSeconds(3), ofMillis(250))
                                            )
                                    );

                                    user.setRole(SPECTATOR);
                                    event.getPlayer().teleport(map.arenaCheckpoints.get(0).spawn());
                                    arena.getUsers().stream()
                                            .map(IUser::asBukkit)
                                            .filter(Objects::nonNull)
                                            .forEach((target) -> this.audiences.player(target).sendMessage(miniMessage().deserialize(
                                                    this.configuration.language().chatMessageArenaPlayerFinished
                                                            .replace("<player>", event.getPlayer().getDisplayName())
                                                            .replace("<seconds>", valueOf(time))
                                                            .replace("<finishPosition>", valueOf(position))
                                            )));

                                    this.configuration.language().chatMessageGameEndSpectator.stream()
                                            .map(miniMessage()::deserialize)
                                            .forEach((component) -> this.audiences.player(event.getPlayer()).sendMessage(component));

                                    event.getPlayer().setAllowFlight(true);
                                    event.getPlayer().setGameMode(ADVENTURE);
                                    arena.getUsers()
                                            .stream().map(IUser::asBukkit)
                                            .filter(Objects::nonNull).forEach(
                                                    (target) -> target.hidePlayer(this.plugin, event.getPlayer())
                                            );

                                    event.getPlayer().getInventory().clear();
                                    event.getPlayer().getInventory().setItem(
                                            8, new ItemBuilder(RED_BED)
                                                    .name(miniMessage().deserialize(this.configuration.language().arenaItemLeaveName))
                                                    .itemFlags(values())
                                                    .build()
                                    );

                                })
                );

    }

}
