package org.figuramc.figura.server.packets.handlers.s2c;

import org.figuramc.figura.backend2.FSB;
import org.figuramc.figura.server.packets.CloseOutcomingStreamPacket;
import org.figuramc.figura.server.utils.IFriendlyByteBuf;

public class CloseOutcomingStreamPacketHandler extends ConnectedPacketHandler<CloseOutcomingStreamPacket> {
    @Override
    protected void handlePacket(CloseOutcomingStreamPacket packet) {
        FSB.instance().closeIncomingStream(packet.streamId(), packet.code());
    }

    @Override
    public CloseOutcomingStreamPacket serialize(IFriendlyByteBuf byteBuf) {
        return new CloseOutcomingStreamPacket(byteBuf);
    }
}
