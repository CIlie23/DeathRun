package pl.mrstudios.deathrun.config.impl;

import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.annotation.Header;
import eu.okaeri.configs.annotation.Names;
import org.bukkit.Location;
import pl.mrstudios.deathrun.api.arena.trap.ITrap;
import pl.mrstudios.deathrun.arena.checkpoint.Checkpoint;
import pl.mrstudios.deathrun.arena.pad.TeleportPad;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static eu.okaeri.configs.annotation.NameModifier.TO_LOWER_CASE;
import static eu.okaeri.configs.annotation.NameStrategy.HYPHEN_CASE;

@Header({
        " ",
        "--------------------------------------------------------------------------",
        "                                INFORMATION",
        "--------------------------------------------------------------------------",
        " ",
        " Please dont modify this file, any modifications may cause problems with",
        " plugin, modify only if you know what you are doing.",
        " ",
        "--------------------------------------------------------------------------",
        " "
}) @SuppressWarnings("deprecation")
@Names(strategy = HYPHEN_CASE, modifier = TO_LOWER_CASE)
public class MapConfiguration extends OkaeriConfig {

    public List<MapDefinition> maps = new ArrayList<>();

    public String arenaName;

    /* Spawns */
    public Location arenaWaitingLobbyLocation;

    public List<Location> arenaRunnerSpawnLocations = new ArrayList<>();;
    public List<Location> arenaDeathSpawnLocations = new ArrayList<>();;

    /* Traps */
    public List<ITrap> arenaTraps = new ArrayList<>();;

    /* Checkpoints */
    public List<Checkpoint> arenaCheckpoints = new ArrayList<>();;

    /* Misc */
    public List<TeleportPad> teleportPads = new ArrayList<>();
    public List<Location> arenaStartBarrierBlocks = new ArrayList<>();;

    /* Setup Status */
    public boolean arenaSetupEnabled = true;

    public List<MapDefinition> resolvedMaps() {
        if (!this.maps.isEmpty()) {
            this.maps.forEach((map) -> {
                if (map.id == null || map.id.isBlank())
                    map.id = this.mapIdFor(map.name);
            });
            return this.maps;
        }

        MapDefinition legacyMap = new MapDefinition();
        legacyMap.id = this.mapIdFor(this.arenaName);
        legacyMap.name = this.arenaName;
        legacyMap.world = this.arenaWaitingLobbyLocation == null ? "" : this.arenaWaitingLobbyLocation.getWorld().getName();
        legacyMap.arenaWaitingLobbyLocation = this.arenaWaitingLobbyLocation;
        legacyMap.arenaRunnerSpawnLocations = this.arenaRunnerSpawnLocations;
        legacyMap.arenaDeathSpawnLocations = this.arenaDeathSpawnLocations;
        legacyMap.arenaTraps = this.arenaTraps;
        legacyMap.arenaCheckpoints = this.arenaCheckpoints;
        legacyMap.teleportPads = this.teleportPads;
        legacyMap.arenaStartBarrierBlocks = this.arenaStartBarrierBlocks;
        legacyMap.arenaSetupEnabled = this.arenaSetupEnabled;
        return List.of(legacyMap);
    }

    public MapDefinition getMapById(String id) {
        return this.resolvedMaps().stream()
                .filter((map) -> this.mapIdFor(map.id).equalsIgnoreCase(this.mapIdFor(id)))
                .findFirst()
                .orElse(null);
    }

    private String mapIdFor(String source) {
        if (source == null || source.isBlank())
            return "default";

        return source.toLowerCase(Locale.ROOT).replace(" ", "-");
    }

    @SuppressWarnings("deprecation")
    @Names(strategy = HYPHEN_CASE, modifier = TO_LOWER_CASE)
    public static class MapDefinition extends OkaeriConfig {

        public String id;
        public String name;
        public String world;

        public Location arenaWaitingLobbyLocation;

        public List<Location> arenaRunnerSpawnLocations = new ArrayList<>();
        public List<Location> arenaDeathSpawnLocations = new ArrayList<>();

        public List<ITrap> arenaTraps = new ArrayList<>();

        public List<Checkpoint> arenaCheckpoints = new ArrayList<>();

        public List<TeleportPad> teleportPads = new ArrayList<>();
        public List<Location> arenaStartBarrierBlocks = new ArrayList<>();

        public boolean arenaSetupEnabled = false;

    }

}
