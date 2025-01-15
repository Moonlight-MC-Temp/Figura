package org.figuramc.figura.server.packets;

import org.figuramc.figura.server.utils.IFriendlyByteBuf;
import org.figuramc.figura.server.utils.Identifier;
import org.figuramc.figura.server.utils.StatusCode;

/**
 * Closes outcoming stream opened by this side. Same on client and server side.
 */
public class CloseOutcomingStreamPacket implements Packet {
    public static final Identifier PACKET_ID = new Identifier("figura", "stream/close_out");
    private final int streamId;
    private final StatusCode code;

    public CloseOutcomingStreamPacket(int streamId, StatusCode code) {
        this.streamId = streamId;
        this.code = code;
    }

    public CloseOutcomingStreamPacket(IFriendlyByteBuf buf) {
        streamId = buf.readInt();
        code = StatusCode.getFromCode(buf.readShort());
    }

    public int streamId() {
        return streamId;
    }

    public StatusCode code() {
        return code;
    }

    @Override
    public void write(IFriendlyByteBuf buf) {
        buf.writeInt(streamId);
        buf.writeShort(code.statusCode());
    }

    @Override
    public Identifier getId() {
        return PACKET_ID;
    }
}
