package org.figuramc.figura.server.packets.handlers.s2c;

import org.figuramc.figura.backend2.FSB;
import org.figuramc.figura.server.packets.Packet;
import org.figuramc.figura.server.utils.IFriendlyByteBuf;

public abstract class ConnectedPacketHandler<T extends Packet> implements S2CPacketHandler<T> {
    @Override
    public void handle(T packet) {
        if (FSB.instance().connected()) handlePacket(packet);
    }

    protected abstract void handlePacket(T packet);
}
