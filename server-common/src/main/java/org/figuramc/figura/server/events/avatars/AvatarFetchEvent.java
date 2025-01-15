package org.figuramc.figura.server.events.avatars;

import org.figuramc.figura.server.events.CancellableEvent;
import org.figuramc.figura.server.utils.Hash;

public class AvatarFetchEvent extends CancellableEvent {
    private final Hash hash;

    public AvatarFetchEvent(Hash hash) {
        this.hash = hash;
    }
}
