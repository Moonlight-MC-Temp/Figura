package org.figuramc.figura.server.packets.handlers.s2c;

import org.figuramc.figura.backend2.FSB;
import org.figuramc.figura.server.packets.s2c.S2CPingPacket;
import org.figuramc.figura.server.utils.IFriendlyByteBuf;

public class S2CPingPacketHandler implements S2CPacketHandler<S2CPingPacket> {
    @Override
    public S2CPingPacket serialize(IFriendlyByteBuf byteBuf) {
        return new S2CPingPacket(byteBuf);
    }

    @Override
    public void handle(S2CPingPacket packet) {
        FSB.instance().handlePing(packet);
    }
}
