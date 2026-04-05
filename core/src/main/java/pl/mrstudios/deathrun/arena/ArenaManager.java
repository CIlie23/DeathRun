package pl.mrstudios.deathrun.arena;

import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pl.mrstudios.commons.bukkit.item.ItemBuilder;
import pl.mrstudios.deathrun.api.arena.enums.GameState;
import pl.mrstudios.deathrun.api.arena.event.arena.ArenaUserJoinedEvent;
import pl.mrstudios.deathrun.api.arena.event.arena.ArenaUserLeftEvent;
import pl.mrstudios.deathrun.api.arena.user.IUser;
import pl.mrstudios.deathrun.arena.user.User;
import pl.mrstudios.deathrun.config.Configuration;
import pl.mrstudios.deathrun.config.impl.MapConfiguration;

import java.util.*;

import static java.lang.Integer.MAX_VALUE;
import static java.lang.String.valueOf;
import static net.kyori.adventure.text.minimessage.MiniMessage.miniMessage;
import static org.bukkit.GameMode.ADVENTURE;
import static org.bukkit.Material.COMPASS;
import static org.bukkit.Material.RED_BED;
import static org.bukkit.inventory.ItemFlag.values;
import static org.bukkit.potion.PotionEffectType.NIGHT_VISION;
import static org.bukkit.potion.PotionEffectType.SATURATION;
import static pl.mrstudios.deathrun.api.arena.enums.GameState.STARTING;
import static pl.mrstudios.deathrun.api.arena.enums.GameState.WAITING;
import static pl.mrstudios.deathrun.api.arena.enums.GameState.PLAYING;
import static pl.mrstudios.deathrun.api.arena.enums.GameState.ENDING;

public class ArenaManager {

    private final Plugin plugin;
    private final Server server;
    private final BukkitAudiences audiences;
    private final Configuration configuration;

    private final Map<String, ArenaRuntime> runtimesByMapId = new LinkedHashMap<>();
    private final Map<UUID, String> playerMapIndex = new HashMap<>();

    public ArenaManager(
            @NotNull Plugin plugin,
            @NotNull Server server,
            @NotNull BukkitAudiences audiences,
            @NotNull Configuration configuration
    ) {
        this.plugin = plugin;
        this.server = server;
        this.audiences = audiences;
        this.configuration = configuration;
    }

    public void initialize() {
        this.runtimesByMapId.values().forEach((runtime) -> runtime.service().cancel());
        this.runtimesByMapId.clear();

        for (MapConfiguration.MapDefinition map : this.configuration.map().resolvedMaps()) {
            String mapId = this.mapId(map);
            Arena arena = new Arena(this.mapName(map));
            ArenaServiceRunnable service = new ArenaServiceRunnable(arena, map, this.plugin, this.server, this.audiences, this.configuration);
            service.runTaskTimer(this.plugin, 0, 20);
            this.runtimesByMapId.put(mapId, new ArenaRuntime(mapId, map, arena, service));
        }
    }

    public @NotNull Collection<ArenaRuntime> runtimes() {
        return this.runtimesByMapId.values();
    }

    public @Nullable ArenaRuntime runtimeForPlayer(
            @NotNull Player player
    ) {
        String mapId = this.playerMapIndex.get(player.getUniqueId());
        if (mapId == null)
            return null;

        return this.runtimesByMapId.get(mapId);
    }

    public @Nullable ArenaRuntime runtimeByMapId(
            @NotNull String mapId
    ) {
        return this.runtimesByMapId.get(mapId.toLowerCase(Locale.ROOT));
    }

    public @Nullable MapConfiguration.MapDefinition mapForPlayer(
            @NotNull Player player
    ) {
        ArenaRuntime runtime = this.runtimeForPlayer(player);
        return runtime == null ? null : runtime.map();
    }

    public @Nullable Arena arenaForPlayer(
            @NotNull Player player
    ) {
        ArenaRuntime runtime = this.runtimeForPlayer(player);
        return runtime == null ? null : runtime.arena();
    }

    public int playersInMap(
            @NotNull String mapId
    ) {
        ArenaRuntime runtime = this.runtimeByMapId(mapId);
        return runtime == null ? 0 : runtime.arena().getUsers().size();
    }

    public void reloadRuntime(
            @NotNull String mapId
    ) {
        String normalizedMapId = mapId.toLowerCase(Locale.ROOT);
        ArenaRuntime previous = this.runtimesByMapId.remove(normalizedMapId);
        if (previous != null)
            previous.service().cancel();

        MapConfiguration.MapDefinition map = this.configuration.map().getMapById(normalizedMapId);
        if (map == null)
            return;

        Arena arena = new Arena(this.mapName(map));
        ArenaServiceRunnable service = new ArenaServiceRunnable(arena, map, this.plugin, this.server, this.audiences, this.configuration);
        service.runTaskTimer(this.plugin, 0, 20);
        this.runtimesByMapId.put(normalizedMapId, new ArenaRuntime(normalizedMapId, map, arena, service));
    }

    public @NotNull JoinResult joinMap(
            @NotNull Player player,
            @NotNull String mapId
    ) {
        ArenaRuntime runtime = this.runtimeByMapId(mapId);
        if (runtime == null)
            return JoinResult.MAP_UNAVAILABLE;

        if (!this.isMapConfigured(runtime.map()))
            return JoinResult.MAP_NOT_READY;

        if (runtime.arena().getGameState() != WAITING && runtime.arena().getGameState() != STARTING)
            return JoinResult.MATCH_IN_PROGRESS;

        int maxPlayers = this.maxPlayers(runtime.map());
        if (runtime.arena().getUsers().size() >= maxPlayers)
            return JoinResult.MAP_FULL;

        ArenaRuntime previousRuntime = this.runtimeForPlayer(player);
        if (previousRuntime != null && previousRuntime.mapId().equalsIgnoreCase(runtime.mapId()))
            return JoinResult.ALREADY_IN_MAP;

        this.leaveCurrentMap(player, true);

        User user = new User(player);
        runtime.arena().getUsers().add(user);
        this.playerMapIndex.put(player.getUniqueId(), runtime.mapId());

        this.preparePlayerForWaiting(player, runtime.map());

        if (runtime.arena().getSidebar() != null)
            runtime.arena().getSidebar().addViewer(player);

        runtime.arena().getUsers().stream()
                .map(IUser::asBukkit)
                .filter(Objects::nonNull)
                .forEach((target) -> this.audiences.player(target).sendMessage(miniMessage().deserialize(
                        this.configuration.language().chatMessageArenaPlayerJoined
                                .replace("<player>", player.getDisplayName())
                                .replace("<currentPlayers>", valueOf(runtime.arena().getUsers().size()))
                                .replace("<maxPlayers>", valueOf(maxPlayers))
                )));

        this.server.getPluginManager().callEvent(new ArenaUserJoinedEvent(user, runtime.arena()));
        return JoinResult.JOINED;
    }

    public @NotNull ForceStartResult forceStartMap(
            @NotNull String mapId
    ) {
        ArenaRuntime runtime = this.runtimeByMapId(mapId);
        if (runtime == null)
            return ForceStartResult.MAP_UNAVAILABLE;

        if (runtime.arena().getGameState() == PLAYING || runtime.arena().getGameState() == ENDING)
            return ForceStartResult.MATCH_ALREADY_RUNNING;

        if (runtime.arena().getUsers().isEmpty())
            return ForceStartResult.NO_PLAYERS;

        return runtime.service().requestForceStart()
                ? ForceStartResult.STARTED
                : ForceStartResult.MATCH_ALREADY_RUNNING;
    }

    public @NotNull ForceStopResult forceStopMap(
            @NotNull String mapId
    ) {
        ArenaRuntime runtime = this.runtimeByMapId(mapId);
        if (runtime == null)
            return ForceStopResult.MAP_UNAVAILABLE;

        return runtime.service().requestStop()
                ? ForceStopResult.STOPPED
                : ForceStopResult.ALREADY_WAITING;
    }

    public boolean leaveCurrentMap(
            @NotNull Player player,
            boolean notifyArena
    ) {
        ArenaRuntime runtime = this.runtimeForPlayer(player);
        if (runtime == null)
            return false;

        IUser user = runtime.arena().getUser(player);
        if (user == null) {
            this.playerMapIndex.remove(player.getUniqueId());
            return false;
        }

        runtime.arena().getUsers().remove(user);
        this.playerMapIndex.remove(player.getUniqueId());

        if (runtime.arena().getSidebar() != null)
            runtime.arena().getSidebar().removeViewer(player);

        if (notifyArena && (runtime.arena().getGameState() == WAITING || runtime.arena().getGameState() == STARTING)) {
            int maxPlayers = this.maxPlayers(runtime.map());
            runtime.arena().getUsers().stream()
                    .map(IUser::asBukkit)
                    .filter(Objects::nonNull)
                    .forEach((target) -> this.audiences.player(target).sendMessage(miniMessage().deserialize(
                            this.configuration.language().chatMessageArenaPlayerLeft
                                    .replace("<player>", player.getDisplayName())
                                    .replace("<currentPlayers>", valueOf(runtime.arena().getUsers().size()))
                                    .replace("<maxPlayers>", valueOf(maxPlayers))
                    )));
        }

        this.server.getPluginManager().callEvent(new ArenaUserLeftEvent(user, runtime.arena()));
        return true;
    }

    public void preparePlayerForLobbyTools(
            @NotNull Player player
    ) {
        player.getInventory().setItem(
                0,
                new ItemBuilder(COMPASS)
                        .name(miniMessage().deserialize(this.configuration.language().arenaItemMapSelectorName))
                        .itemFlags(values())
                        .build()
        );
    }

    public @Nullable Arena primaryArena() {
        return this.runtimesByMapId.values().stream()
                .findFirst()
                .map(ArenaRuntime::arena)
                .orElse(null);
    }

    public int maxPlayers(
            @NotNull MapConfiguration.MapDefinition map
    ) {
        return map.arenaRunnerSpawnLocations.size() + map.arenaDeathSpawnLocations.size();
    }

    public boolean isMapConfigured(
            @NotNull MapConfiguration.MapDefinition map
    ) {
        return !map.arenaSetupEnabled
                && map.arenaWaitingLobbyLocation != null
                && !map.arenaRunnerSpawnLocations.isEmpty()
                && !map.arenaDeathSpawnLocations.isEmpty()
                && !map.arenaCheckpoints.isEmpty();
    }

    private void preparePlayerForWaiting(
            @NotNull Player player,
            @NotNull MapConfiguration.MapDefinition map
    ) {
        player.getActivePotionEffects().stream()
                .map(PotionEffect::getType)
                .forEach(player::removePotionEffect);

        player.getInventory().clear();
        player.setGameMode(ADVENTURE);
        player.setAllowFlight(false);

        if (map.arenaWaitingLobbyLocation != null)
            player.teleport(map.arenaWaitingLobbyLocation);

        player.addPotionEffect(new PotionEffect(SATURATION, MAX_VALUE, 1, false, false, false));
        player.addPotionEffect(new PotionEffect(NIGHT_VISION, MAX_VALUE, 1, false, false, false));

        this.preparePlayerForLobbyTools(player);
        player.getInventory().setItem(
                8,
                new ItemBuilder(RED_BED)
                        .name(miniMessage().deserialize(this.configuration.language().arenaItemLeaveName))
                        .itemFlags(values())
                        .build()
        );
    }

    private @NotNull String mapId(
            @NotNull MapConfiguration.MapDefinition map
    ) {
        if (map.id != null && !map.id.isBlank())
            return map.id.toLowerCase(Locale.ROOT);

        if (map.name == null || map.name.isBlank())
            return "default";

        return map.name.toLowerCase(Locale.ROOT).replace(" ", "-");
    }

    private @NotNull String mapName(
            @NotNull MapConfiguration.MapDefinition map
    ) {
        return (map.name == null || map.name.isBlank()) ? this.mapId(map) : map.name;
    }

    public enum JoinResult {
        JOINED,
        ALREADY_IN_MAP,
        MAP_UNAVAILABLE,
        MAP_NOT_READY,
        MAP_FULL,
        MATCH_IN_PROGRESS
    }

    public enum ForceStartResult {
        STARTED,
        MAP_UNAVAILABLE,
        NO_PLAYERS,
        MATCH_ALREADY_RUNNING
    }

    public enum ForceStopResult {
        STOPPED,
        MAP_UNAVAILABLE,
        ALREADY_WAITING
    }

    public record ArenaRuntime(
            @NotNull String mapId,
            @NotNull MapConfiguration.MapDefinition map,
            @NotNull Arena arena,
            @NotNull ArenaServiceRunnable service
    ) {}

}
