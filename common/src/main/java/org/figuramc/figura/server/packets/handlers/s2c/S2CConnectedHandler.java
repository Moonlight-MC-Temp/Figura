package org.figuramc.figura.server.packets.handlers.s2c;

import org.figuramc.figura.backend2.FSB;
import org.figuramc.figura.server.packets.s2c.S2CConnectedPacket;
import org.figuramc.figura.server.utils.IFriendlyByteBuf;

public class S2CConnectedHandler extends ConnectedPacketHandler<S2CConnectedPacket> {
    @Override
    protected void handlePacket(S2CConnectedPacket packet) {
        FSB.instance().handleUserConnected(packet);
    }

    @Override
    public S2CConnectedPacket serialize(IFriendlyByteBuf byteBuf) {
        return new S2CConnectedPacket(byteBuf);
    }
}
