package org.figuramc.figura.server.packets;

import org.figuramc.figura.server.utils.IFriendlyByteBuf;
import org.figuramc.figura.server.utils.Identifier;

/**
 * Closes incoming stream opened by other side. Same on client and server side.
 */
public class AllowIncomingStreamPacket implements Packet {
    public static final Identifier PACKET_ID = new Identifier("figura", "stream/allow_in");

    private final int streamId;

    public AllowIncomingStreamPacket(int streamId) {
        this.streamId = streamId;
    }

    public AllowIncomingStreamPacket(IFriendlyByteBuf buf) {
        streamId = buf.readInt();
    }

    @Override
    public void write(IFriendlyByteBuf buf) {
        buf.writeInt(streamId);
    }

    public int streamId() {
        return streamId;
    }

    @Override
    public Identifier getId() {
        return PACKET_ID;
    }
}
