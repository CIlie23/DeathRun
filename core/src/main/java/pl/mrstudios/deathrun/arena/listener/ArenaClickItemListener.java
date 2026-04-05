package pl.mrstudios.deathrun.arena.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import pl.mrstudios.commons.inject.annotation.Inject;
import pl.mrstudios.deathrun.arena.ArenaManager;
import pl.mrstudios.deathrun.arena.selector.MapSelectorService;
import pl.mrstudios.deathrun.config.Configuration;

import static org.bukkit.Material.COMPASS;
import static org.bukkit.Material.RED_BED;
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
    @EventHandler(priority = EventPriority.MONITOR)
    public void onStepOnBlockEffect(
            @NotNull PlayerInteractEvent event
    ) {

        if (event.getHand() != EquipmentSlot.HAND)
            return;

        if (event.getAction() != RIGHT_CLICK_BLOCK && event.getAction() != RIGHT_CLICK_AIR)
            return;

        if (event.getItem() == null)
            return;

        if (event.getItem().getType() == COMPASS) {
            this.mapSelectorService.open(event.getPlayer());
            return;
        }

        if (event.getItem().getType() != RED_BED)
            return;

        this.arenaManager.leaveCurrentMap(event.getPlayer(), true);

        connect(plugin, event.getPlayer(), this.configuration.plugin().server);

    }

}
