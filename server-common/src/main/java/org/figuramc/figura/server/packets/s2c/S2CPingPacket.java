package org.figuramc.figura.server.packets.s2c;

import org.figuramc.figura.server.packets.Packet;
import org.figuramc.figura.server.utils.IFriendlyByteBuf;
import org.figuramc.figura.server.utils.Identifier;

import java.util.UUID;

public class S2CPingPacket implements Packet {
    public static final Identifier PACKET_ID = new Identifier("figura", "s2c/ping");

    private final UUID sender;
    private final int id;
    private final byte[] data;

    public S2CPingPacket(UUID sender, int id, byte[] data) {
        this.sender = sender;
        this.id = id;
        this.data = data;
    }

    public S2CPingPacket(IFriendlyByteBuf buf) {
        sender = buf.readUUID();
        id = buf.readInt();
        data = buf.readBytes();
    }

    public UUID sender() {
        return sender;
    }

    public int id() {
        return id;
    }


    public byte[] data() {
        return data;
    }

    @Override
    public void write(IFriendlyByteBuf buf) {
        buf.writeUUID(sender);
        buf.writeInt(id);
        buf.writeBytes(data);
    }

    @Override
    public Identifier getId() {
        return PACKET_ID;
    }
}
