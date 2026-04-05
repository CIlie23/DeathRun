package pl.mrstudios.deathrun.command;

import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.regions.Region;
import dev.rollczi.litecommands.annotations.argument.Arg;
import dev.rollczi.litecommands.annotations.command.Command;
import dev.rollczi.litecommands.annotations.context.Context;
import dev.rollczi.litecommands.annotations.execute.Execute;
import dev.rollczi.litecommands.annotations.permission.Permission;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.lingala.zip4j.ZipFile;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pl.mrstudios.commons.inject.annotation.Inject;
import pl.mrstudios.deathrun.api.arena.trap.ITrap;
import pl.mrstudios.deathrun.api.arena.user.enums.Role;
import pl.mrstudios.deathrun.arena.checkpoint.Checkpoint;
import pl.mrstudios.deathrun.arena.pad.TeleportPad;
import pl.mrstudios.deathrun.arena.trap.TrapRegistry;
import pl.mrstudios.deathrun.config.Configuration;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static com.sk89q.worldedit.bukkit.BukkitAdapter.adapt;
import static java.lang.String.join;
import static java.nio.file.Files.createDirectories;
import static java.nio.file.Files.createFile;
import static java.nio.file.Paths.get;
import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;
import static java.util.stream.Stream.of;
import static net.kyori.adventure.text.minimessage.MiniMessage.miniMessage;
import static org.bukkit.Material.*;
import static pl.mrstudios.deathrun.api.arena.user.enums.Role.DEATH;
import static pl.mrstudios.deathrun.api.arena.user.enums.Role.RUNNER;
import static pl.mrstudios.deathrun.util.ChannelUtil.connect;

@Command(
        name = "deathrun",
        aliases = { "dr" }
) @SuppressWarnings("unused")
@Permission("mrstudios.command.deathrun")
public class CommandDeathRun {

    private static final String PREFIX = "<gold>[DR]</gold> ";

    private final Plugin plugin;
    private final WorldEdit worldEdit;

    private final BukkitAudiences audiences;

    private final TrapRegistry  trapRegistry;
    private final Configuration configuration;

    @Inject
    public CommandDeathRun(
            @NotNull Plugin plugin,
            @NotNull WorldEdit worldEdit,
            @NotNull BukkitAudiences audiences,
            @NotNull TrapRegistry trapRegistry,
            @NotNull Configuration configuration
    ) {
        this.plugin = plugin;
        this.worldEdit = worldEdit;
        this.audiences = audiences;
        this.trapRegistry = trapRegistry;
        this.configuration = configuration;
    }

    @Execute
    public void noArguments(
            @Context Player player
    ) {
        String content = join("<br>", this.configuration.language().commandHelpMainLines)
            .replace("<version>", this.plugin.getDescription().getVersion());
        this.message(player, content);
    }

    @Execute(name = "help")
    public void help(
            @Context Player player
    ) {
        this.noArguments(player);
    }

    @Execute(name = "leave")
    @Permission("mrstudios.command.deathrun.leave")
    public void leave(
            @Context Player player
    ) {
        connect(this.plugin, player, this.configuration.plugin().server);
    }

    /* Setup Command */
    @Execute(name = "setup")
    @Permission("mrstudios.command.deathrun.setup")
    public void noArgumentsSetup(
            @Context Player player
    ) {

        if (!this.configuration.map().arenaSetupEnabled) {
            this.message(player, this.configuration.language().commandMessageSetupDisabled);
            return;
        }

        String content = join("<br>", this.configuration.language().commandHelpSetupLines)
            .replace("<version>", this.plugin.getDescription().getVersion());
        this.message(player, content);
    }

    @Execute(name = "setup help")
    @Permission("mrstudios.command.deathrun.setup")
    public void setupHelp(
            @Context Player player
    ) {
        this.noArgumentsSetup(player);
    }

    @Execute(name = "setup addcheckpoint")
    @Permission("mrstudios.command.deathrun.setup")
    public void addCheckpoint(
            @Context Player player
    ) {

        if (!this.configuration.map().arenaSetupEnabled) {
            this.message(player, this.configuration.language().commandMessageSetupDisabled);
            return;
        }

        this.configuration.map().arenaCheckpoints.add(
                new Checkpoint(this.configuration.map().arenaCheckpoints.size(), player.getLocation().toCenterLocation(), this.locations(player))
        );

        this.message(player, this.configuration.language().commandMessageCheckpointAdded);

    }

    @Execute(name = "setup addspawn")
    @Permission("mrstudios.command.deathrun.setup")
    public void addSpawn(
            @Context Player player,
            @Arg("role") Role role
    ) {

        if (!this.configuration.map().arenaSetupEnabled) {
            this.message(player, this.configuration.language().commandMessageSetupDisabled);
            return;
        }

        switch (role) {

            case RUNNER ->
                    this.configuration.map().arenaRunnerSpawnLocations.add(player.getLocation().toCenterLocation());

            case DEATH ->
                    this.configuration.map().arenaDeathSpawnLocations.add(player.getLocation().toCenterLocation());

            default ->
                    this.message(player, this.configuration.language().commandMessageRoleInvalid);

        }

        if (role != DEATH && role != RUNNER)
            return;

        this.message(player, this.configuration.language().commandMessageRoleSpawnAdded.replace("<role>", role.name()));

    }

    @Execute(name = "setup addtrap")
    @Permission("mrstudios.command.deathrun.setup")
    public void addTrap(
            @Context Player player,
            @Arg("type") String type
    ) throws Exception {
        this.trap(player, type, (Object) null);
    }

    @Execute(name = "setup addtrap")
    @Permission("mrstudios.command.deathrun.setup")
    public void addTrap(
            @Context Player player,
            @Arg("type") String type,
            @Arg("material") Material material
    ) throws Exception {
        this.trap(player, type, material);
    }

    @Execute(name = "setup addtrap")
    @Permission("mrstudios.command.deathrun.setup")
    public void addTrap(
            @Context Player player,
            @Arg("type") String type,
            @Arg("particle") Particle particle
    ) throws Exception {
        this.trap(player, type, particle, 5, 0.25);
    }

    @Execute(name = "setup addtrap")
    @Permission("mrstudios.command.deathrun.setup")
    public void addTrap(
            @Context Player player,
            @Arg("type") String type,
            @Arg("particle") Particle particle,
            @Arg("count") int count,
            @Arg("offset") double offset
    ) throws Exception {
        this.trap(player, type, particle, count, offset);
    }

    @Execute(name = "setup setname")
    @Permission("mrstudios.command.deathrun.setup")
    public void setName(
            @Context Player player,
            @Arg("name") String name
    ) {

        if (!this.configuration.map().arenaSetupEnabled) {
            this.message(player, this.configuration.language().commandMessageSetupDisabled);
            return;
        }

        this.configuration.map().arenaName = name;
        this.message(player, this.configuration.language().commandMessageArenaNameSet.replace("<name>", name));

    }

    @Execute(name = "setup setstartbarrier")
    @Permission("mrstudios.command.deathrun.setup")
    public void setStartBarrier(
            @Context Player player
    ) {
        this.setStartBarrier(player, null);
    }

    @Execute(name = "setup setstartbarrier")
    @Permission("mrstudios.command.deathrun.setup")
    public void setStartBarrier(
            @Context Player player,
            @Arg("material") Material material
    ) {

        if (!this.configuration.map().arenaSetupEnabled) {
            this.message(player, this.configuration.language().commandMessageSetupDisabled);
            return;
        }

        List<Location> locations = this.locations(player);

        if (material != null)
            locations.removeIf((location) -> !location.getBlock().getType().equals(material));

        this.configuration.map().arenaStartBarrierBlocks = locations;
        this.message(player, this.configuration.language().commandMessageStartBarrierSet);

    }

    @Execute(name = "setup setwaitinglobby")
    @Permission("mrstudios.command.deathrun.setup")
    public void setWaitingLobby(
            @Context Player player
    ) {

        if (!this.configuration.map().arenaSetupEnabled) {
            this.message(player, this.configuration.language().commandMessageSetupDisabled);
            return;
        }

        this.configuration.map().arenaWaitingLobbyLocation = player.getLocation().toCenterLocation();
        this.message(player, this.configuration.language().commandMessageWaitingLobbySet);

    }

    @Execute(name = "setup addteleport")
    @Permission("mrstudios.command.deathrun.setup")
    public void addTeleportPad(
            @Context Player player
    ) {

        if (!this.configuration.map().arenaSetupEnabled) {
            this.message(player, this.configuration.language().commandMessageSetupDisabled);
            return;
        }

        this.configuration.map().teleportPads.add(new TeleportPad(locations(player).get(0), player.getLocation().toCenterLocation().add(0, -0.5, 0)));
        this.message(player, this.configuration.language().commandMessageTeleportPadAdded);

    }

    @Execute(name = "setup save")
    @Permission("mrstudios.command.deathrun.setup")
    public void save(
            @Context Player player
    ) {

        if (!this.configuration.map().arenaSetupEnabled) {
            this.message(player, this.configuration.language().commandMessageSetupDisabled);
            return;
        }

        this.configuration.map().arenaSetupEnabled = false;
        this.configuration.map().save();

        Path path = get(this.plugin.getDataFolder().toString(), "backup/", player.getWorld().getName() + ".zip");

        try {
            createDirectories(path.getParent());
            createFile(path);
        } catch (@NotNull Exception exception) {
            throw new RuntimeException("Unable to save world backup due to an exception.", exception);
        }

        try (ZipFile zipFile = new ZipFile(path.toString())) {
            zipFile.addFolder(player.getWorld().getWorldFolder());
        } catch (@NotNull Exception ignored) {}

        this.message(player, this.configuration.language().commandMessageSaveSuccess);

    }

    protected void message(
            @NotNull Player player, String message,
            @Nullable Object... args
    ) {
        String content = (args != null && args.length > 0)
            ? java.lang.String.format(message, args)
            : message;
        this.audiences.player(player).sendMessage(miniMessage().deserialize(content));
    }

    protected List<Location> locations(@NotNull Player player) {

        try {

            List<Location> locations = new ArrayList<>();
            LocalSession session = this.worldEdit.getSessionManager().findByName(player.getName());

            assert session != null;
            Region region = session.getSelection(session.getSelectionWorld());

            region.forEach((vector) -> locations.add(adapt(player.getWorld(), vector)));

            return locations;

        } catch (@NotNull Exception ignored) {}

        return emptyList();

    }

    protected void trap(
            @NotNull Player player,
            @NotNull String type,
            @Nullable Object... objects
    ) throws Exception {

        if (!this.configuration.map().arenaSetupEnabled) {
            this.message(player, this.configuration.language().commandMessageSetupDisabled);
            return;
        }

        Block target = player.getTargetBlock(null, 250);
        List<Location> locations = this.locations(player);
        Class<? extends ITrap> trapClass = this.trapRegistry.get(type.toUpperCase());

        if (of(
                STONE_BUTTON,
                OAK_BUTTON,
                ACACIA_BUTTON,
                BIRCH_BUTTON,
                CRIMSON_BUTTON,
                JUNGLE_BUTTON,
                SPRUCE_BUTTON,
                WARPED_BUTTON,
                POLISHED_BLACKSTONE_BUTTON,
                DARK_OAK_BUTTON
        ).noneMatch((button) -> target.getType().equals(button))) {
            this.message(player, this.configuration.language().commandMessageTrapLookAtButton);
            return;
        }

        if (trapClass == null) {
            this.message(player, this.configuration.language().commandMessageTrapNotExists.replace("<type>", type.toUpperCase()));
            return;
        }

        ITrap trap = trapClass.getDeclaredConstructor().newInstance();

        trap.setButton(target.getLocation());
        trap.setLocations(trap.filter(locations, objects));
        ofNullable(objects).ifPresent(trap::setExtra);

        this.configuration.map().arenaTraps.add(trap);
        this.message(player, this.configuration.language().commandMessageTrapAdded.replace("<type>", type.toUpperCase()));

    }

}
