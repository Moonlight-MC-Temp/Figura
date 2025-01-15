package org.figuramc.figura.server;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.tree.CommandNode;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.chat.BaseComponentSerializer;
import net.md_5.bungee.chat.ComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.figuramc.figura.server.commands.FiguraServerCommandSource;
import org.figuramc.figura.server.commands.FiguraServerCommands;
import org.figuramc.figura.server.packets.Packets;
import org.figuramc.figura.server.packets.Side;
import org.figuramc.figura.server.utils.ComponentUtils;
import org.figuramc.figura.server.utils.Identifier;

import java.util.*;
import java.util.concurrent.CompletableFuture;

import static org.figuramc.figura.server.SpigotUtils.call;

public class FiguraSpigot extends JavaPlugin implements Listener {
    private FiguraServerSpigot srv;
    private BukkitTask tickTask;
    public static final boolean DEBUG = Objects.equals(System.getProperty("figuraDebug"), "true");
    private final ArrayList<Identifier> outcomingPackets = new ArrayList<>();

    private final CommandDispatcher<FiguraServerCommandSource> dispatcher = new CommandDispatcher<>();

    @Override
    public void onEnable() {
        srv = new FiguraServerSpigot(this);
        var msg = getServer().getMessenger();
        Bukkit.getPluginManager().registerEvents(this, this);
        Packets.forEachPacket(((id, packetDescriptor) -> {
            Side side = packetDescriptor.side();
            if (side.sentBy(Side.SERVER)) {
                msg.registerOutgoingPluginChannel(this, id.toString());
                outcomingPackets.add(id);
            }
            if (side.sentBy(Side.CLIENT)) msg.registerIncomingPluginChannel(this, id.toString(), srv);
            srv.logDebug("Registered channel for %s".formatted(id));
        }));
        srv.init();
        tickTask = new BukkitTickRunnable().runTaskTimer(this, 0, 1);

        dispatcher.register(FiguraServerCommands.getCommand());
    }

    private String[] getCmd(String commandName, String[] commandArgs) {
        String[] cmdName = new String[1 + commandArgs.length];
        cmdName[0] = commandName;
        System.arraycopy(commandArgs, 0, cmdName, 1, commandArgs.length);
        return cmdName;
    }

    @SuppressWarnings("NullableProblems")
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player pl) {
            FiguraSpigotCommandSource source = new FiguraSpigotCommandSource(pl.getUniqueId());
            String[] commandComponents = getCmd(label, args);
            String commandText = String.join(" ", commandComponents);
            ParseResults<FiguraServerCommandSource> parseResult = dispatcher.parse(commandText, source);
            try {
                dispatcher.execute(parseResult);
            } catch (CommandSyntaxException e) {
                sender.spigot().sendMessage(getSyntaxError(e));
            }
        }
        return true;
    }

    @SuppressWarnings("NullableProblems")
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (sender instanceof Player pl) {
            FiguraSpigotCommandSource source = new FiguraSpigotCommandSource(pl.getUniqueId());
            String[] commandComponents = getCmd(alias, args);
            String commandText = String.join(" ", commandComponents);
            ParseResults<FiguraServerCommandSource> parseResult = dispatcher.parse(commandText, source);
            Suggestions suggestions = dispatcher.getCompletionSuggestions(parseResult).join();
            return suggestions.getList().stream().map(Suggestion::getText).toList();
        }
        return List.of();
    }

    @Override
    public void onDisable() {
        srv.close();
    }

    private static final Class<?>[] CHANNEL_ARGS = new Class[] {String.class};

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        var player = event.getPlayer();
        for (Identifier id: outcomingPackets) {
            call(player, "addChannel", CHANNEL_ARGS, id.toString());
            srv.logDebug("Registered %s for %s".formatted(id, player.getName()));
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        var player = event.getPlayer();
        for (Identifier id: outcomingPackets) {
            call(player, "removeChannel", CHANNEL_ARGS, id.toString());
            srv.logDebug("Unregistered %s for %s".formatted(id, player.getName()));
        }
        srv.userManager().onUserLeave(player.getUniqueId());
    }

    private BaseComponent[] getSyntaxError(CommandSyntaxException exception) {
        return ComponentSerializer.parse(ComponentUtils.text(exception.getMessage()).color("red").build().toString());
    }

    private class BukkitTickRunnable extends BukkitRunnable {

        @Override
        public void run() {
            srv.tick();
        }
    }
}
