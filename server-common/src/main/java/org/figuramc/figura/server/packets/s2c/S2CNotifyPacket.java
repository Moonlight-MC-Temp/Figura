package org.figuramc.figura.server.packets.s2c;

import org.figuramc.figura.server.packets.Packet;
import org.figuramc.figura.server.utils.IFriendlyByteBuf;
import org.figuramc.figura.server.utils.Identifier;

import java.util.UUID;

public class S2CNotifyPacket implements Packet {
    public static final Identifier PACKET_ID = new Identifier("figura", "s2c/notify");
    private final UUID target;

    public S2CNotifyPacket(UUID target) {
        this.target = target;
    }

    public S2CNotifyPacket(IFriendlyByteBuf buf) {
        target = buf.readUUID();
    }

    @Override
    public void write(IFriendlyByteBuf buf) {
        buf.writeUUID(target);
    }

    public UUID target() {
        return target;
    }

    @Override
    public Identifier getId() {
        return PACKET_ID;
    }
}
