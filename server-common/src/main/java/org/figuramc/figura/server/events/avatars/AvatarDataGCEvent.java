package org.figuramc.figura.server.events.avatars;

import org.figuramc.figura.server.events.CancellableEvent;

public class AvatarDataGCEvent extends CancellableEvent {
    private final byte[] hash;

    public AvatarDataGCEvent(byte[] hash) {
        this.hash = hash;
    }

    public byte[] hash() {
        return hash;
    }
}
