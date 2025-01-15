package org.figuramc.figura.server.packets.handlers.s2c;

import org.figuramc.figura.backend2.FSB;
import org.figuramc.figura.server.packets.CloseIncomingStreamPacket;
import org.figuramc.figura.server.packets.CloseOutcomingStreamPacket;
import org.figuramc.figura.server.utils.IFriendlyByteBuf;

public class CloseIncomingStreamPacketHandler extends ConnectedPacketHandler<CloseIncomingStreamPacket> {
    @Override
    protected void handlePacket(CloseIncomingStreamPacket packet) {
        FSB.instance().closeOutcomingStreamPacket(packet.streamId(), packet.code());
    }

    @Override
    public CloseIncomingStreamPacket serialize(IFriendlyByteBuf byteBuf) {
        return new CloseIncomingStreamPacket(byteBuf);
    }
}
