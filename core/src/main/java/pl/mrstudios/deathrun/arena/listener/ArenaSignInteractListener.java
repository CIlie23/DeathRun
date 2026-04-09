package pl.mrstudios.deathrun.arena.listener;

import org.bukkit.ChatColor;
import org.bukkit.block.Sign;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.jetbrains.annotations.NotNull;
import pl.mrstudios.commons.inject.annotation.Inject;
import pl.mrstudios.deathrun.arena.ArenaManager;
import pl.mrstudios.deathrun.arena.sign.SignManager;
import pl.mrstudios.deathrun.arena.sign.SignManager.QueueSign;

import static org.bukkit.event.EventPriority.HIGHEST;
import static org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK;

public class ArenaSignInteractListener implements Listener {

    private final ArenaManager arenaManager;
    private final SignManager signManager;

    @Inject
    public ArenaSignInteractListener(
            @NotNull ArenaManager arenaManager,
            @NotNull SignManager signManager
    ) {
        this.arenaManager = arenaManager;
        this.signManager = signManager;
    }

    @EventHandler(priority = HIGHEST)
    public void onPlayerInteract(
            @NotNull PlayerInteractEvent event
    ) {
        if (event.getAction() != RIGHT_CLICK_BLOCK || event.getClickedBlock() == null)
            return;

        if (!(event.getClickedBlock().getState() instanceof Sign))
            return;

        QueueSign queueSign = this.signManager.signAt(event.getClickedBlock());
        if (queueSign == null)
            return;

        if (!event.getPlayer().hasPermission("deathrun.signs.use")) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(ChatColor.RED + "You don't have permission to use DeathRun signs.");
            return;
        }

        event.setCancelled(true);

        switch (queueSign.type()) {

            case JOIN -> {
                if (queueSign.mapId() != null && this.arenaManager.isMapLockedForEditing(queueSign.mapId())) {
                    event.getPlayer().sendMessage(ChatColor.RED + "This map is currently unavailable as it is being edited.");
                    return;
                }

                if (queueSign.mapId() == null || !this.signManager.queuePlayerToMap(event.getPlayer(), queueSign.mapId())) {
                    event.getPlayer().sendMessage(ChatColor.RED + "This map is currently not joinable.");
                    return;
                }

                int queued = this.signManager.queuedPlayersCount(queueSign.mapId());
                ArenaManager.ArenaRuntime runtime = this.arenaManager.runtimeByMapId(queueSign.mapId());
                int ready = runtime == null ? queued : (runtime.arena().getUsers().size() + queued);
                int required = runtime == null
                        ? 0
                    : runtime.service().requiredPlayersToStartForDisplay();
                event.getPlayer().sendMessage(ChatColor.GREEN + "Joined queue for map " + queueSign.mapId() + ". "
                    + ChatColor.GRAY + "(" + ready + "/" + required + " ready)");
            }

            case AUTOJOIN -> {
                if (!this.signManager.queuePlayerToBestMap(event.getPlayer())) {
                    event.getPlayer().sendMessage(ChatColor.RED + "No map available for auto-join.");
                    return;
                }

                event.getPlayer().sendMessage(ChatColor.GREEN + "Joined the best available queue and moved to map lobby.");
            }

            case LEAVE -> {
                boolean leftQueue = this.signManager.leaveQueue(event.getPlayer());
                boolean leftMap = this.arenaManager.leaveCurrentMap(event.getPlayer(), true);
                if (!leftQueue && !leftMap) {
                    event.getPlayer().sendMessage(ChatColor.GRAY + "You're not in any DeathRun queue.");
                    return;
                }

                this.arenaManager.returnPlayerToHub(event.getPlayer());
                event.getPlayer().sendMessage(ChatColor.YELLOW + "You have left the match and returned to the Hub.");
            }

        }
    }

}
