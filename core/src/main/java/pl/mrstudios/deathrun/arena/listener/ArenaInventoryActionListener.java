package pl.mrstudios.deathrun.arena.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerAttemptPickupItemEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerPickupArrowEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import pl.mrstudios.commons.inject.annotation.Inject;
import pl.mrstudios.deathrun.arena.ArenaManager;

import static org.bukkit.event.EventPriority.MONITOR;
import static org.bukkit.event.inventory.InventoryType.PLAYER;

public class ArenaInventoryActionListener implements Listener {

    private final ArenaManager arenaManager;
    private final Plugin plugin;

    @Inject
    public ArenaInventoryActionListener(
            @NotNull ArenaManager arenaManager,
            @NotNull Plugin plugin
    ) {
        this.arenaManager = arenaManager;
        this.plugin = plugin;
    }

    @EventHandler(priority = MONITOR)
    public void onInventoryClick(
            @NotNull InventoryClickEvent event
    ) {

        if (event.getClickedInventory() == null)
            return;

        if (event.getClickedInventory().getType() != PLAYER)
            return;

        event.setCancelled(true);

    }

    @EventHandler(priority = MONITOR)
    public void onItemDrop(
            @NotNull PlayerDropItemEvent event
    ) {
        event.setCancelled(true);

        if (this.arenaManager.runtimeForPlayer(event.getPlayer()) != null)
            this.plugin.getLogger().info("[DR-DBG] Prevented item drop for "
                + event.getPlayer().getName()
                + " item=" + event.getItemDrop().getItemStack().getType());
    }

    @EventHandler(priority = MONITOR)
    public void onPlayerItemSwap(
            @NotNull PlayerSwapHandItemsEvent event
    ) {
        event.setCancelled(true);
    }

    @EventHandler(priority = MONITOR)
    public void onPlayerArrowPickup(
            @NotNull PlayerPickupArrowEvent event
    ) {
        event.setCancelled(true);
    }

    @EventHandler(priority = MONITOR)
    public void onPlayerItemPickup(
            @NotNull PlayerAttemptPickupItemEvent event
    ) {
        event.setCancelled(true);
    }

}
