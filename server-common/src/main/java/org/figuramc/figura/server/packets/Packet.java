package org.figuramc.figura.server.packets;

import org.figuramc.figura.server.utils.IFriendlyByteBuf;
import org.figuramc.figura.server.utils.Identifier;

public interface Packet {
    void write(IFriendlyByteBuf buf);
    Identifier getId();


    interface Deserializer<P extends Packet> {
        P read(IFriendlyByteBuf buf);
    }
}
