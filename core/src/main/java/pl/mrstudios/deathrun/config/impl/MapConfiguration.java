package pl.mrstudios.deathrun.config.impl;

import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.annotation.Header;
import eu.okaeri.configs.annotation.Names;
import org.bukkit.Location;
import org.bukkit.Material;
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
    public List<Material> arenaStartBarrierRestoreMaterials = new ArrayList<>();

    /* Setup Status */
    public boolean arenaSetupEnabled = true;

    public List<MapDefinition> resolvedMaps() {
        if (!this.maps.isEmpty()) {
            this.maps.forEach((map) -> {
                if (map.id == null || map.id.isBlank())
                    map.id = this.normalizedMapId(map.name);
            });
            return this.maps;
        }

        MapDefinition legacyMap = new MapDefinition();
        legacyMap.id = this.normalizedMapId(this.arenaName);
        legacyMap.name = this.arenaName;
        legacyMap.world = this.arenaWaitingLobbyLocation == null ? "" : this.arenaWaitingLobbyLocation.getWorld().getName();
        legacyMap.arenaWaitingLobbyLocation = this.arenaWaitingLobbyLocation;
        legacyMap.arenaRunnerSpawnLocations = this.arenaRunnerSpawnLocations;
        legacyMap.arenaDeathSpawnLocations = this.arenaDeathSpawnLocations;
        legacyMap.arenaTraps = this.arenaTraps;
        legacyMap.arenaCheckpoints = this.arenaCheckpoints;
        legacyMap.teleportPads = this.teleportPads;
        legacyMap.arenaStartBarrierBlocks = this.arenaStartBarrierBlocks;
        legacyMap.arenaStartBarrierRestoreMaterials = this.arenaStartBarrierRestoreMaterials;
        legacyMap.arenaSetupEnabled = this.arenaSetupEnabled;
        return List.of(legacyMap);
    }

    public MapDefinition getMapById(String id) {
        return this.resolvedMaps().stream()
                .filter((map) -> this.normalizedMapId(map.id).equalsIgnoreCase(this.normalizedMapId(id)))
                .findFirst()
                .orElse(null);
    }

    public void ensureMapsMutable() {
        if (!this.maps.isEmpty()) {
            this.maps.forEach((map) -> map.id = this.normalizedMapId(map.id));
            return;
        }

        MapDefinition legacy = this.resolvedMaps().get(0);
        MapDefinition migrated = new MapDefinition();

        migrated.id = this.normalizedMapId(legacy.id);
        migrated.name = legacy.name;
        migrated.world = legacy.world;
        migrated.arenaWaitingLobbyLocation = legacy.arenaWaitingLobbyLocation;
        migrated.arenaRunnerSpawnLocations = new ArrayList<>(legacy.arenaRunnerSpawnLocations);
        migrated.arenaDeathSpawnLocations = new ArrayList<>(legacy.arenaDeathSpawnLocations);
        migrated.arenaTraps = new ArrayList<>(legacy.arenaTraps);
        migrated.arenaCheckpoints = new ArrayList<>(legacy.arenaCheckpoints);
        migrated.teleportPads = new ArrayList<>(legacy.teleportPads);
        migrated.arenaStartBarrierBlocks = new ArrayList<>(legacy.arenaStartBarrierBlocks);
        migrated.arenaStartBarrierRestoreMaterials = new ArrayList<>(legacy.arenaStartBarrierRestoreMaterials);
        migrated.arenaSetupEnabled = legacy.arenaSetupEnabled;

        this.maps.add(migrated);
    }

    public String normalizedMapId(String source) {
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
        public List<Material> arenaStartBarrierRestoreMaterials = new ArrayList<>();

        public boolean arenaSetupEnabled = false;

    }

}
