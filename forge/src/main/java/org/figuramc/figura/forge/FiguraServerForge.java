package org.figuramc.figura.forge;

import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundCustomPayloadPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.server.permission.PermissionAPI;
import net.minecraftforge.server.permission.nodes.PermissionNode;
import org.figuramc.figura.server.FiguraModServer;
import org.figuramc.figura.server.packets.Packet;
import org.figuramc.figura.utils.FriendlyByteBufWrapper;

import java.util.UUID;

public class FiguraServerForge extends FiguraModServer {
    @Override
    protected void sendPacketInternal(UUID receiver, Packet packet) {
        ServerPlayer player = getServer().getPlayerList().getPlayer(receiver);
        if (player == null) return;
        var id = packet.getId();
        var resLoc = new ResourceLocation(id.namespace(), id.path());
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        packet.write(new FriendlyByteBufWrapper(buf));
        PacketDistributor.PLAYER.with(() -> player).send(new ClientboundCustomPayloadPacket(resLoc, buf));
    }

    @Override
    public boolean getPermission(UUID player, String permission) {
        ServerPlayer pl = getServer().getPlayerList().getPlayer(player);
        PermissionNode<Boolean> perm = FiguraForgePermissions.getPermission(permission);
        return pl != null && perm != null && PermissionAPI.getPermission(pl, perm);
    }
}
