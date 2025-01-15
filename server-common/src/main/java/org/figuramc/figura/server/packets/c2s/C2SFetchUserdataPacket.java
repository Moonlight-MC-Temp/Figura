package org.figuramc.figura.server.packets.c2s;

import org.figuramc.figura.server.packets.Packet;
import org.figuramc.figura.server.utils.IFriendlyByteBuf;
import org.figuramc.figura.server.utils.Identifier;

import java.util.UUID;

/**
 * Packet sent to server in order to get player's userdata
 */
public class C2SFetchUserdataPacket implements Packet {
    public static final Identifier PACKET_ID = new Identifier("figura", "c2s/userdata");

    private final int requestId;
    private final UUID target;

    public C2SFetchUserdataPacket(int requestId, UUID target) {
        this.requestId = requestId;
        this.target = target;
    }

    public C2SFetchUserdataPacket(IFriendlyByteBuf byteBuf) {
        requestId = byteBuf.readInt();
        target = byteBuf.readUUID();
    }

    public int transactionId() {
        return requestId;
    }

    @Override
    public void write(IFriendlyByteBuf byteBuf) {
        byteBuf.writeInt(requestId);
        byteBuf.writeUUID(target);
    }

    public UUID target() {
        return target;
    }

    @Override
    public Identifier getId() {
        return PACKET_ID;
    }
}
