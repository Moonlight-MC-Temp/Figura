package org.figuramc.figura.server.packets.handlers.c2s;

import org.figuramc.figura.server.packets.Packet;
import org.figuramc.figura.server.utils.IFriendlyByteBuf;

import java.util.UUID;

public interface C2SPacketHandler<P extends Packet> {
    P serialize(IFriendlyByteBuf byteBuf);
    void handle(UUID sender, P packet);
}
