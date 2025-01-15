package org.figuramc.figura.server;

import com.google.gson.JsonObject;
import net.md_5.bungee.chat.ComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.figuramc.figura.server.packets.Packet;
import org.figuramc.figura.server.packets.handlers.c2s.C2SPacketHandler;
import org.figuramc.figura.server.utils.Identifier;
import org.figuramc.figura.server.utils.InputStreamByteBuf;
import org.figuramc.figura.server.utils.OutputStreamByteBuf;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.file.Path;
import java.util.UUID;
import java.util.logging.Level;

public class FiguraServerSpigot extends FiguraServer implements PluginMessageListener {
    private final FiguraSpigot parent;

    public FiguraServerSpigot(FiguraSpigot parent) {
        this.parent = parent;
    }

    @Override
    public Path getFiguraFolder() {
        return parent.getDataFolder().toPath();
    }

    @Override
    protected void sendPacketInternal(UUID receiver, Packet packet) {
        Player player = Bukkit.getPlayer(receiver);
        if (player == null) return;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        OutputStreamByteBuf buf = new OutputStreamByteBuf(baos);
        packet.write(buf);
        byte[] packetData = baos.toByteArray();
        logDebug("Sending packet %s".formatted(packet.getId()   ));
        player.sendPluginMessage(parent, packet.getId().toString(), packetData);
    }

    @Override
    public void onPluginMessageReceived(String s, Player player, byte[] bytes) {
        Identifier id = Identifier.parse(s);
        C2SPacketHandler<Packet> handler = getPacketHandler(id);
        logDebug("Got packet %s from %s".formatted(id, player.getName()));
        if (handler != null) {
            logDebug("Handling packet %s".formatted(id));
            ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
            InputStreamByteBuf buf = new InputStreamByteBuf(bais);
            Packet packet = handler.serialize(buf);
            handler.handle(player.getUniqueId(), packet);
        }
    }

    @Override
    public boolean getPermission(UUID player, String permission) {
        Player pl = parent.getServer().getPlayer(player);
        return pl != null && pl.hasPermission(permission);
    }

    @Override
    public void sendMessage(UUID receiver, JsonObject component) {
        Player pl = parent.getServer().getPlayer(receiver);
        if (pl != null) {
            pl.spigot().sendMessage(ComponentSerializer.parse(component.toString()));
        }
    }

    @Override
    public void logInfo(String text) {
        parent.getLogger().info(text);
    }

    @Override
    public void logError(String text) {
        parent.getLogger().log(Level.WARNING, text);
    }

    @Override
    public void logError(String text, Throwable err) {
        parent.getLogger().log(Level.WARNING, text, err);
    }

    @Override
    public void logDebug(String text) {
        if (FiguraSpigot.DEBUG) parent.getLogger().log(Level.INFO, "[DEBUG] %s".formatted(text));
    }
}
