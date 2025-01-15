package org.figuramc.figura.server.packets.s2c;

import org.figuramc.figura.server.packets.Packet;
import org.figuramc.figura.server.utils.IFriendlyByteBuf;
import org.figuramc.figura.server.utils.Identifier;

import java.util.UUID;

public class S2CConnectedPacket implements Packet {
    public static final Identifier PACKET_ID = new Identifier("figura", "s2c/connected");

    private final UUID player;

    public S2CConnectedPacket(UUID player) {
        this.player = player;
    }

    public S2CConnectedPacket(IFriendlyByteBuf buf) {
        player = buf.readUUID();
    }

    public UUID player() {
        return player;
    }

    @Override
    public void write(IFriendlyByteBuf buf) {
        buf.writeUUID(player);
    }

    @Override
    public Identifier getId() {
        return PACKET_ID;
    }
}
