package org.figuramc.figura.server.packets.handlers.s2c;

import org.figuramc.figura.backend2.FSB;
import org.figuramc.figura.server.packets.AllowIncomingStreamPacket;
import org.figuramc.figura.server.utils.IFriendlyByteBuf;

public class AllowOutcomingStreamPacketHandler extends ConnectedPacketHandler<AllowIncomingStreamPacket> {
    @Override
    protected void handlePacket(AllowIncomingStreamPacket packet) {
        FSB.instance().handleAllow(packet.streamId());
    }

    @Override
    public AllowIncomingStreamPacket serialize(IFriendlyByteBuf byteBuf) {
        return new AllowIncomingStreamPacket(byteBuf);
    }
}
