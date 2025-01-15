package org.figuramc.figura.server.packets;

import org.figuramc.figura.server.utils.IFriendlyByteBuf;
import org.figuramc.figura.server.utils.Identifier;

public class CustomFSBPacket implements Packet {
    public static final Identifier PACKET_ID = new Identifier("figura", "ping/server");
    public static final int MAX_SERVER_PING_SIZE = 32766 - 4;
    private final int id;
    private final byte[] data;

    public CustomFSBPacket(int id, byte[] data) {
        if (data.length > MAX_SERVER_PING_SIZE) throw new IllegalArgumentException("Server ping size can't be more than %s".formatted(MAX_SERVER_PING_SIZE));
        this.id = id;
        this.data = data;
    }

    public CustomFSBPacket(IFriendlyByteBuf buf) {
        id = buf.readInt();
        data = buf.readBytes();
    }

    @Override
    public void write(IFriendlyByteBuf buf) {
        buf.writeInt(id);
        buf.writeBytes(data);
    }

    public int id() {
        return id;
    }

    public byte[] data() {
        return data;
    }

    @Override
    public Identifier getId() {
        return PACKET_ID;
    }
}
