package org.figuramc.figura.server.packets.s2c;

import org.figuramc.figura.server.avatars.EHashPair;
import org.figuramc.figura.server.packets.Packet;
import org.figuramc.figura.server.utils.Hash;
import org.figuramc.figura.server.utils.IFriendlyByteBuf;
import org.figuramc.figura.server.utils.Identifier;
import org.figuramc.figura.server.utils.Pair;
import org.jetbrains.annotations.Nullable;

import java.util.BitSet;
import java.util.UUID;

import static java.nio.charset.StandardCharsets.UTF_8;

public class S2CUserdataPacket implements Packet {
    public static final Identifier PACKET_ID = new Identifier("figura", "s2c/userdata");

    private final int responseId;
    private final UUID target;
    private final BitSet prideBadges;
    private final @Nullable Pair<String, EHashPair> avatar;

    public S2CUserdataPacket(int responseId, UUID target, BitSet prideBadges, @Nullable Pair<String, EHashPair> avatar) {
        this.responseId = responseId;
        this.target = target;
        this.prideBadges = prideBadges;
        this.avatar = avatar;
    }

    public S2CUserdataPacket(IFriendlyByteBuf byteBuf) {
        this.responseId = byteBuf.readInt();
        this.target = byteBuf.readUUID();
        this.prideBadges = BitSet.valueOf(byteBuf.readByteArray(Integer.MAX_VALUE));
        if (byteBuf.readByte() != 0) {
            String avatarId = new String(byteBuf.readByteArray(Integer.MAX_VALUE), UTF_8);
            Hash hash = byteBuf.readHash();
            Hash ehash = byteBuf.readHash();
            avatar = new Pair<>(avatarId, new EHashPair(hash, ehash));
        }
        else avatar = null;
    }

    public int responseId() {
        return responseId;
    }

    public UUID target() {
        return target;
    }

    public BitSet prideBadges() {
        return prideBadges;
    }

    public Pair<String, EHashPair> avatar() {
        return avatar;
    }

    @Override
    public void write(IFriendlyByteBuf byteBuf) {
        byteBuf.writeInt(responseId);
        byteBuf.writeUUID(target);
        byteBuf.writeByteArray(prideBadges.toByteArray());
        if (avatar != null) {
            byteBuf.writeByte(1);
            byteBuf.writeByteArray(avatar.left().getBytes(UTF_8));
            byteBuf.writeBytes(avatar.right().hash().get());
            byteBuf.writeBytes(avatar.right().ehash().get());
        }
        else {
            byteBuf.writeByte(0);
        }
    }

    @Override
    public Identifier getId() {
        return PACKET_ID;
    }
}
