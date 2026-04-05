package pl.mrstudios.deathrun.command.handler;

import dev.rollczi.litecommands.handler.result.ResultHandlerChain;
import dev.rollczi.litecommands.invalidusage.InvalidUsage;
import dev.rollczi.litecommands.invalidusage.InvalidUsageHandler;
import dev.rollczi.litecommands.invocation.Invocation;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import pl.mrstudios.commons.inject.annotation.Inject;

import static net.kyori.adventure.text.minimessage.MiniMessage.miniMessage;

public class InvalidCommandUsageHandler implements InvalidUsageHandler<CommandSender> {

    private static final String PREFIX = "<gold>[DR]</gold> ";

    private final BukkitAudiences audiences;

    @Inject
    public InvalidCommandUsageHandler(
            @NotNull BukkitAudiences audiences
    ) {
        this.audiences = audiences;
    }

    @Override
    public void handle(
            @NotNull Invocation<CommandSender> invocation,
            @NotNull InvalidUsage<CommandSender> result,
            @NotNull ResultHandlerChain<CommandSender> chain
    ) {
        String usage = usageFor(invocation);
        this.audiences.sender(invocation.sender()).sendMessage(miniMessage().deserialize(PREFIX + usage));
    }

    private @NotNull String usageFor(@NotNull Invocation<CommandSender> invocation) {
        if (invocation.arguments().asList().isEmpty()) {
            return "<red>Invalid usage. <gray>Try: <white>/deathrun <dark_gray>| <white>/deathrun maps <dark_gray>| <white>/deathrun leave <dark_gray>| <white>/deathrun setup";
        }

        String first = invocation.arguments().asList().get(0).toLowerCase();
        if (!"setup".equals(first)) {
            return "<red>Unknown subcommand. <gray>Use <white>/deathrun maps<gray>, <white>/deathrun leave <gray>or <white>/deathrun setup";
        }

        if (invocation.arguments().asList().size() == 1) {
            return "<gray>Setup commands: <white>/deathrun setup help";
        }

        return "<red>Invalid setup usage. <gray>Try: <white>/deathrun setup setname <name><dark_gray>, <white>setwaitinglobby<dark_gray>, <white>setstartbarrier (material)<dark_gray>, <white>addspawn <death/runner><dark_gray>, <white>addtrap <type> (args)<dark_gray>, <white>addcheckpoint<dark_gray>, <white>addteleport<dark_gray>, <white>save";
    }

}
