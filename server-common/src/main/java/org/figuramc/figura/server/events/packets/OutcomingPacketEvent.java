package org.figuramc.figura.server.events.packets;

import org.figuramc.figura.server.events.CancellableEvent;
import org.figuramc.figura.server.packets.Packet;

import java.util.UUID;

public class OutcomingPacketEvent extends CancellableEvent {
    private final UUID receiver;
    private final Packet outcomingPacket;

    public OutcomingPacketEvent(UUID receiver, Packet outcomingPacket) {
        this.receiver = receiver;
        this.outcomingPacket = outcomingPacket;
    }

    public UUID receiver() {
        return receiver;
    }

    public Packet outcomingPacket() {
        return outcomingPacket;
    }
}
