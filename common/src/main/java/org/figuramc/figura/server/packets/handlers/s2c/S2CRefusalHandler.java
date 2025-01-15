package org.figuramc.figura.server.packets.handlers.s2c;

import org.figuramc.figura.backend2.FSB;
import org.figuramc.figura.server.packets.s2c.S2CRefusedPacket;
import org.figuramc.figura.server.utils.IFriendlyByteBuf;

public class S2CRefusalHandler implements S2CPacketHandler<S2CRefusedPacket> {
    @Override
    public S2CRefusedPacket serialize(IFriendlyByteBuf byteBuf) {
        return new S2CRefusedPacket();
    }

    @Override
    public void handle(S2CRefusedPacket packet) {
        FSB.instance().handleConnectionRefusal();
    }
}
