package org.figuramc.figura.server.packets.c2s;

import org.figuramc.figura.server.packets.Packet;
import org.figuramc.figura.server.utils.Hash;
import org.figuramc.figura.server.utils.IFriendlyByteBuf;
import org.figuramc.figura.server.utils.Identifier;

/**
 * Packet sent to server in order to request an avatar.
 */
public class C2SFetchAvatarPacket implements Packet {
    public static final Identifier PACKET_ID = new Identifier("figura", "c2s/avatars/fetch");

    private final int streamId;
    private final Hash hash;

    public C2SFetchAvatarPacket(int streamId, Hash hash) {
        this.streamId = streamId;
        this.hash = hash;
    }

    public C2SFetchAvatarPacket(IFriendlyByteBuf buf) {
        streamId = buf.readInt();
        hash = buf.readHash();
    }

    public int streamId() {
        return streamId;
    }

    public Hash hash() {
        return hash;
    }

    @Override
    public void write(IFriendlyByteBuf byteBuf) {
        byteBuf.writeInt(streamId);
        byteBuf.writeBytes(hash.get());
    }

    @Override
    public Identifier getId() {
        return PACKET_ID;
    }
}
