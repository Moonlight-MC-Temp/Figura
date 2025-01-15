package org.figuramc.figura.server.events.packets;

import org.figuramc.figura.server.events.CancellableEvent;
import org.figuramc.figura.server.packets.Packet;

import java.util.UUID;

public class IncomingPacketEvent extends CancellableEvent {
    private final UUID sender;
    private final Packet incomingPacket;

    public IncomingPacketEvent(UUID sender, Packet incomingPacket) {
        this.sender = sender;
        this.incomingPacket = incomingPacket;
    }

    public UUID sender() {
        return sender;
    }

    public Packet incomingPacket() {
        return incomingPacket;
    }
}
