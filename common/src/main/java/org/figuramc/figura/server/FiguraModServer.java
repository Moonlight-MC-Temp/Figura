package org.figuramc.figura.server;

import com.google.gson.JsonObject;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundCustomPayloadPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.figuramc.figura.server.packets.Packet;
import org.figuramc.figura.utils.FriendlyByteBufWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.UUID;

public abstract class FiguraModServer extends FiguraServer {
    public static final String MOD_ID = "figura";
    public static final Logger LOGGER = LoggerFactory.getLogger("Figura");
    private MinecraftServer server;

    @Override
    public Path getFiguraFolder() {
        return Path.of("fsb");
    }

    @Override
    public void logInfo(String text) {
        LOGGER.info(text);
    }

    @Override
    public void logError(String text) {
        LOGGER.error(text);
    }

    @Override
    public void logError(String text, Throwable err) {
        LOGGER.error(text, err);
    }

    @Override
    public void logDebug(String text) {
        LOGGER.debug(text);
    }

    public static FiguraModServer getInstance() {
        return (FiguraModServer) INSTANCE;
    }

    @Override
    public void sendMessage(UUID receiver, JsonObject component) {
        ServerPlayer player = getServer().getPlayerList().getPlayer(receiver);
        if (player != null) player.sendSystemMessage(Component.Serializer.fromJson(component));
    }

    protected MinecraftServer getServer() {
        return server;
    }

    @Override
    public void close() {
        server = null;
        super.close();
    }

    public final void finishInitialization(MinecraftServer server) {
        if (this.server != null) throw new IllegalStateException("Server already initialized");
        this.server = server;
    }
}
