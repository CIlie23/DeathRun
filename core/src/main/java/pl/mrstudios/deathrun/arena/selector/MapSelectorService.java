package pl.mrstudios.deathrun.arena.selector;

import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pl.mrstudios.deathrun.config.Configuration;
import pl.mrstudios.deathrun.config.impl.MapConfiguration;

import java.util.List;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.lang.String.valueOf;
import static net.kyori.adventure.text.minimessage.MiniMessage.miniMessage;

public class MapSelectorService {

    private final Plugin plugin;
    private final Configuration configuration;
    private final BukkitAudiences audiences;
    private final NamespacedKey mapIdKey;

    public MapSelectorService(
            @NotNull Plugin plugin,
            @NotNull Configuration configuration,
            @NotNull BukkitAudiences audiences
    ) {
        this.plugin = plugin;
        this.configuration = configuration;
        this.audiences = audiences;
        this.mapIdKey = new NamespacedKey(plugin, "selector-map-id");
    }

    public void open(
            @NotNull Player player
    ) {
        List<MapConfiguration.MapDefinition> maps = this.configuration.map().resolvedMaps();
        int size = min(max(((maps.size() + 8) / 9) * 9, 9), 54);

        Inventory inventory = Bukkit.createInventory(null, size, this.configuration.language().mapSelectorTitle);
        for (int i = 0; i < maps.size() && i < size; i++) {
            MapConfiguration.MapDefinition map = maps.get(i);
            inventory.setItem(i, this.mapItem(map, this.currentPlayersForMap(map), this.maxPlayersForMap(map)));
        }

        player.openInventory(inventory);
    }

    public boolean isSelectorInventory(
            @NotNull String title
    ) {
        return this.configuration.language().mapSelectorTitle.equals(title);
    }

    public void handleClick(
            @NotNull Player player,
            @Nullable ItemStack item
    ) {
        if (item == null)
            return;

        ItemMeta meta = item.getItemMeta();
        if (meta == null)
            return;

        String mapId = meta.getPersistentDataContainer().get(this.mapIdKey, PersistentDataType.STRING);
        if (mapId == null)
            return;

        MapConfiguration.MapDefinition map = this.configuration.map().getMapById(mapId);
        if (map == null || !this.isMapJoinable(map)) {
            this.audiences.player(player).sendMessage(miniMessage().deserialize(this.configuration.language().mapSelectorMapUnavailable));
            return;
        }

        if (map.arenaWaitingLobbyLocation != null)
            player.teleport(map.arenaWaitingLobbyLocation);

        this.audiences.player(player).sendMessage(miniMessage().deserialize(
                this.configuration.language().mapSelectorMapSelected.replace("<map>", map.name)
        ));
    }

    private boolean isMapJoinable(
            @NotNull MapConfiguration.MapDefinition map
    ) {
        return !map.arenaSetupEnabled && map.arenaWaitingLobbyLocation != null;
    }

    private int currentPlayersForMap(
            @NotNull MapConfiguration.MapDefinition map
    ) {
        if (map.world == null || map.world.isBlank())
            return 0;

        if (Bukkit.getWorld(map.world) == null)
            return 0;

        return Bukkit.getWorld(map.world).getPlayers().size();
    }

    private int maxPlayersForMap(
            @NotNull MapConfiguration.MapDefinition map
    ) {
        return map.arenaRunnerSpawnLocations.size() + map.arenaDeathSpawnLocations.size();
    }

    private @NotNull ItemStack mapItem(
            @NotNull MapConfiguration.MapDefinition map,
            int currentPlayers,
            int maxPlayers
    ) {
        ItemStack item = new ItemStack(this.itemMaterial(map));
        ItemMeta meta = item.getItemMeta();

        String mapName = map.name == null || map.name.isBlank() ? "Unnamed" : map.name;
        String worldName = map.world == null || map.world.isBlank() ? "unknown" : map.world;
        String status = this.mapStatus(map);

        meta.displayName(miniMessage().deserialize(
                this.configuration.language().mapSelectorMapName.replace("<name>", mapName)
        ));

        List<Component> lore = this.configuration.language().mapSelectorMapLore.stream()
                .map((line) -> line
                        .replace("<world>", worldName)
                        .replace("<players>", valueOf(currentPlayers))
                        .replace("<maxPlayers>", valueOf(maxPlayers))
                        .replace("<status>", status)
                )
                .map(miniMessage()::deserialize)
                .toList();

        meta.lore(lore);
        if (map.id != null)
            meta.getPersistentDataContainer().set(this.mapIdKey, PersistentDataType.STRING, map.id);

        item.setItemMeta(meta);
        return item;
    }

    private @NotNull String mapStatus(
            @NotNull MapConfiguration.MapDefinition map
    ) {
        if (map.arenaSetupEnabled)
            return this.configuration.language().mapSelectorStatusDisabled;

        if (map.arenaWaitingLobbyLocation == null)
            return this.configuration.language().mapSelectorStatusMissingLobby;

        return this.configuration.language().mapSelectorStatusAvailable;
    }

    private @NotNull Material itemMaterial(
            @NotNull MapConfiguration.MapDefinition map
    ) {
        if (map.arenaSetupEnabled)
            return Material.BARRIER;

        if (map.arenaWaitingLobbyLocation == null)
            return Material.YELLOW_CONCRETE;

        return Material.LIME_CONCRETE;
    }

}
