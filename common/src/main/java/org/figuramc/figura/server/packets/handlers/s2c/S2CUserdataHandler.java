package org.figuramc.figura.server.packets.handlers.s2c;

import org.figuramc.figura.backend2.FSB;
import org.figuramc.figura.server.packets.s2c.S2CUserdataPacket;
import org.figuramc.figura.server.utils.IFriendlyByteBuf;

public class S2CUserdataHandler extends ConnectedPacketHandler<S2CUserdataPacket> {
    @Override
    protected void handlePacket(S2CUserdataPacket packet) {
        FSB.instance().handleUserdata(packet);
    }

    @Override
    public S2CUserdataPacket serialize(IFriendlyByteBuf byteBuf) {
        return new S2CUserdataPacket(byteBuf);
    }
}
