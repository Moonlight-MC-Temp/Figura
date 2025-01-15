package org.figuramc.figura.server.events;

import java.util.UUID;

public class HandshakeEvent extends CancellableEvent {
    private final UUID receiver;

    public HandshakeEvent(UUID receiver) {
        this.receiver = receiver;
    }

    public UUID receiver() {
        return receiver;
    }
}
