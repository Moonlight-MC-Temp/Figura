package org.figuramc.figura.server.packets.c2s;

import org.figuramc.figura.server.packets.Packet;
import org.figuramc.figura.server.utils.Hash;
import org.figuramc.figura.server.utils.IFriendlyByteBuf;
import org.figuramc.figura.server.utils.Identifier;

import static java.nio.charset.StandardCharsets.UTF_8;

public class C2SUploadAvatarPacket implements Packet {
    public static final Identifier PACKET_ID = new Identifier("figura", "c2s/avatars/upload");

    private final int streamId;
    private final String avatarId;
    private final Hash hash;
    private final Hash ehash;

    public C2SUploadAvatarPacket(int streamId, String avatarId, Hash hash, Hash ehash) {
        this.streamId = streamId;
        this.avatarId = avatarId;
        this.hash = hash;
        this.ehash = ehash;
    }

    public C2SUploadAvatarPacket(IFriendlyByteBuf buf) {
        this.streamId = buf.readInt();
        this.avatarId = new String(buf.readByteArray(256), UTF_8);
        this.hash = buf.readHash();
        this.ehash = buf.readHash();
    }

    public int streamId() {
        return streamId;
    }

    public String avatarId() {
        return avatarId;
    }

    public Hash hash() {
        return hash;
    }

    public Hash ehash() {
        return ehash;
    }

    @Override
    public void write(IFriendlyByteBuf buf) {
        buf.writeInt(streamId);
        buf.writeByteArray(avatarId.getBytes(UTF_8));
        buf.writeBytes(hash.get());
        buf.writeBytes(ehash.get());
    }

    @Override
    public Identifier getId() {
        return PACKET_ID;
    }
}
