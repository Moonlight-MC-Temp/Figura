package org.figuramc.figura.server.events.packets;

import org.figuramc.figura.server.events.Event;

public class DeferredPacketErrorEvent extends Event {
    private final Exception thrownException;

    public DeferredPacketErrorEvent(Exception thrownException) {
        this.thrownException = thrownException;
    }

    public Exception thrownException() {
        return thrownException;
    }
}
