package org.figuramc.figura.server.packets.handlers.s2c;

import org.figuramc.figura.backend2.FSB;
import org.figuramc.figura.server.packets.s2c.S2CUserdataNotFoundPacket;
import org.figuramc.figura.server.utils.IFriendlyByteBuf;

public class S2CUserdataNotFoundHandler extends ConnectedPacketHandler<S2CUserdataNotFoundPacket> {
    @Override
    protected void handlePacket(S2CUserdataNotFoundPacket packet) {
        FSB.instance().handleUserdataNotFound(packet);
    }

    @Override
    public S2CUserdataNotFoundPacket serialize(IFriendlyByteBuf byteBuf) {
        return new S2CUserdataNotFoundPacket(byteBuf);
    }
}
