package org.figuramc.figura.server.packets.handlers.s2c;

import org.figuramc.figura.backend2.FSB;
import org.figuramc.figura.server.packets.AvatarDataPacket;
import org.figuramc.figura.server.utils.IFriendlyByteBuf;

public class S2CAvatarDataPacketHandler extends ConnectedPacketHandler<AvatarDataPacket> {
    @Override
    protected void handlePacket(AvatarDataPacket packet) {
        FSB.instance().handleAvatarData(packet.streamId(), packet.avatarData(), packet.finalChunk());
    }

    @Override
    public AvatarDataPacket serialize(IFriendlyByteBuf byteBuf) {
        return new AvatarDataPacket(byteBuf);
    }
}
