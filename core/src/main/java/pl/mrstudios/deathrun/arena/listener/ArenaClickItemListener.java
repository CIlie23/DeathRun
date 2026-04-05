package pl.mrstudios.deathrun.arena.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import pl.mrstudios.commons.inject.annotation.Inject;
import pl.mrstudios.deathrun.arena.ArenaManager;
import pl.mrstudios.deathrun.arena.selector.MapSelectorService;
import pl.mrstudios.deathrun.config.Configuration;

import static org.bukkit.Material.COMPASS;
import static org.bukkit.Material.AIR;
import static org.bukkit.event.block.Action.RIGHT_CLICK_AIR;
import static org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK;
import static pl.mrstudios.deathrun.util.ChannelUtil.connect;

public class ArenaClickItemListener implements Listener {

    private final Plugin plugin;
    private final ArenaManager arenaManager;
    private final MapSelectorService mapSelectorService;
    private final Configuration configuration;

    @Inject
    public ArenaClickItemListener(
            @NotNull Plugin plugin,
            @NotNull ArenaManager arenaManager,
            @NotNull MapSelectorService mapSelectorService,
            @NotNull Configuration configuration
    ) {
        this.plugin = plugin;
        this.arenaManager = arenaManager;
        this.mapSelectorService = mapSelectorService;
        this.configuration = configuration;
    }

    @SuppressWarnings("deprecation")
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onStepOnBlockEffect(
            @NotNull PlayerInteractEvent event
    ) {

        if (event.getAction() != RIGHT_CLICK_BLOCK && event.getAction() != RIGHT_CLICK_AIR)
            return;

        ItemStack usedItem = this.resolveUsedItem(event);
        if (usedItem == null || usedItem.getType() == AIR)
            return;

        if (event.getHand() == EquipmentSlot.OFF_HAND) {
            ItemStack mainHand = event.getPlayer().getInventory().getItemInMainHand();
            if (mainHand != null && mainHand.getType() != AIR)
                return;
        }

        Material usedType = usedItem.getType();

        if (usedType == COMPASS || usedType == Material.RECOVERY_COMPASS) {
            event.setCancelled(true);
            this.mapSelectorService.open(event.getPlayer());
            return;
        }

        if (!usedType.name().endsWith("_BED"))
            return;

        event.setCancelled(true);

        if (this.arenaManager.leaveCurrentMap(event.getPlayer(), true))
            connect(plugin, event.getPlayer(), this.configuration.plugin().server);

    }

    private ItemStack resolveUsedItem(
            @NotNull PlayerInteractEvent event
    ) {
        if (event.getItem() != null)
            return event.getItem();

        if (event.getHand() == EquipmentSlot.OFF_HAND)
            return event.getPlayer().getInventory().getItemInOffHand();

        return event.getPlayer().getInventory().getItemInMainHand();
    }

}
