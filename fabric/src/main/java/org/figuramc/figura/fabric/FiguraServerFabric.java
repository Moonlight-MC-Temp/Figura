package org.figuramc.figura.fabric;

import io.netty.buffer.Unpooled;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.figuramc.figura.commands.fabric.FiguraServerCommandsFabric;
import org.figuramc.figura.server.FiguraModServer;
import org.figuramc.figura.server.packets.Packet;
import org.figuramc.figura.server.packets.handlers.c2s.C2SPacketHandler;
import org.figuramc.figura.utils.FriendlyByteBufWrapper;

import java.util.UUID;

public class FiguraServerFabric extends FiguraModServer implements DedicatedServerModInitializer {
    @Override
    public void onInitializeServer() {
        init();
        forEachHandler((id, handler) -> {
            var resLoc = new ResourceLocation(id.namespace(), id.path());
            ServerPlayNetworking.registerGlobalReceiver(resLoc, new FabricServerHandler<>(handler));
        });
        FiguraServerCommandsFabric.init();
    }

    @Override
    protected void sendPacketInternal(UUID receiver, Packet packet) {
        ServerPlayer player = getServer().getPlayerList().getPlayer(receiver);
        if (player != null) {
            var id = packet.getId();
            var resLoc = new ResourceLocation(id.namespace(), id.path());
            FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
            packet.write(new FriendlyByteBufWrapper(buf));
            ServerPlayNetworking.send(player, resLoc, buf);
        }
    }

    @Override
    public boolean getPermission(UUID uuid, String permission) {
        ServerPlayer player = getServer().getPlayerList().getPlayer(uuid);
        return player != null && Permissions.check(player, permission);
    }

    private static class FabricServerHandler<P extends Packet> implements ServerPlayNetworking.PlayChannelHandler {
        private final C2SPacketHandler<P> parent;

        private FabricServerHandler(C2SPacketHandler<P> parent) {
            this.parent = parent;
        }

        @Override
        public void receive(MinecraftServer server, ServerPlayer player, ServerGamePacketListenerImpl handler, FriendlyByteBuf buf, PacketSender responseSender) {
            P packet = parent.serialize(new FriendlyByteBufWrapper(buf));
            parent.handle(player.getUUID(), packet);
        }
    }
}
