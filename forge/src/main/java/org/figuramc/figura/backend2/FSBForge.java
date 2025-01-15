package org.figuramc.figura.backend2;

import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ServerboundCustomPayloadPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.PacketDistributor;
import org.figuramc.figura.server.packets.Packet;
import org.figuramc.figura.utils.FriendlyByteBufWrapper;

public class FSBForge extends FSB {
    @Override
    public void sendPacket(Packet packet) {
        var id = packet.getId();
        var resLoc = new ResourceLocation(id.namespace(), id.path());
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        packet.write(new FriendlyByteBufWrapper(buf));
        PacketDistributor.SERVER.noArg().send(new ServerboundCustomPayloadPacket(resLoc, buf));
    }
}
