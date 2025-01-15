package org.figuramc.figura.server.packets;

import org.figuramc.figura.server.utils.IFriendlyByteBuf;
import org.figuramc.figura.server.utils.Identifier;

/**
 * Packed used to send avatar data to other side. Same on client and server side.
 */
public class AvatarDataPacket implements Packet {
    public static final Identifier PACKET_ID = new Identifier("figura", "avatars/data");
    public static final int MAX_CHUNK_SIZE = 32766 - 5;


    private final int streamId;
    private final boolean finalChunk;
    private final byte[] avatarData;

    public AvatarDataPacket(int streamId, boolean finalChunk, byte[] avatarData) {
        this.streamId = streamId;
        this.finalChunk = finalChunk;
        this.avatarData = avatarData;
    }

    public AvatarDataPacket(IFriendlyByteBuf buf) {
        this.streamId = buf.readInt();
        this.finalChunk = buf.readByte() != 0;
        this.avatarData = buf.readBytes();
    }

    public int streamId() {
        return streamId;
    }

    public boolean finalChunk() {
        return finalChunk;
    }

    public byte[] avatarData() {
        return avatarData;
    }

    @Override
    public void write(IFriendlyByteBuf buf) {
        buf.writeInt(streamId);
        buf.writeByte(finalChunk ? 1 : 0);
        buf.writeBytes(avatarData);
    }

    @Override
    public Identifier getId() {
        return PACKET_ID;
    }
}
