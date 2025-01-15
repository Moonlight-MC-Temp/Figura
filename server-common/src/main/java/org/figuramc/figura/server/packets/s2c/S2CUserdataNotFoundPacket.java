package org.figuramc.figura.server.packets.s2c;

import org.figuramc.figura.server.packets.Packet;
import org.figuramc.figura.server.utils.IFriendlyByteBuf;
import org.figuramc.figura.server.utils.Identifier;

public class S2CUserdataNotFoundPacket implements Packet {
    public static final Identifier PACKET_ID = new Identifier("figura", "s2c/userdata_not_found");
    private final int transactionId;

    public S2CUserdataNotFoundPacket(int transactionId) {
        this.transactionId = transactionId;
    }

    public S2CUserdataNotFoundPacket(IFriendlyByteBuf buf) {
        this.transactionId = buf.readInt();
    }

    @Override
    public void write(IFriendlyByteBuf buf) {
        buf.writeInt(transactionId);
    }

    public int transactionId() {
        return transactionId;
    }

    @Override
    public Identifier getId() {
        return PACKET_ID;
    }
}
