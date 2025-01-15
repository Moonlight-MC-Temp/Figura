package org.figuramc.figura.server.packets.c2s;

import org.figuramc.figura.server.packets.Packet;
import org.figuramc.figura.server.utils.IFriendlyByteBuf;
import org.figuramc.figura.server.utils.Identifier;

import java.nio.charset.StandardCharsets;

public class C2SDeleteAvatarPacket implements Packet {
    public static Identifier PACKET_ID = new Identifier("figura", "c2s/avatars/delete");

    private final String avatarId;

    public C2SDeleteAvatarPacket(String avatarId) {
        this.avatarId = avatarId;
    }

    public C2SDeleteAvatarPacket(IFriendlyByteBuf byteBuf) {
        avatarId = new String(byteBuf.readByteArray(256), StandardCharsets.UTF_8);
    }

    @Override
    public void write(IFriendlyByteBuf buf) {
        buf.writeByteArray(avatarId.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public Identifier getId() {
        return PACKET_ID;
    }

    public String avatarId() {
        return avatarId;
    }
}
