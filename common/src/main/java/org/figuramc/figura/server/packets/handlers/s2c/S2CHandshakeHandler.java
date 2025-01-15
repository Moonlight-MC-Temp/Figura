package org.figuramc.figura.server.packets.handlers.s2c;

import org.figuramc.figura.backend2.FSB;
import org.figuramc.figura.server.packets.s2c.S2CBackendHandshakePacket;
import org.figuramc.figura.server.utils.IFriendlyByteBuf;

public class S2CHandshakeHandler implements S2CPacketHandler<S2CBackendHandshakePacket>{
    @Override
    public S2CBackendHandshakePacket serialize(IFriendlyByteBuf byteBuf) {
        return new S2CBackendHandshakePacket(byteBuf);
    }

    @Override
    public void handle(S2CBackendHandshakePacket packet) {
        FSB.instance().handleHandshake(packet);
    }
}
