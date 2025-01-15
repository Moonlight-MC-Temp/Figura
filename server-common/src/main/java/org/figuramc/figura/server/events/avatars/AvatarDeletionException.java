package org.figuramc.figura.server.events.avatars;

import org.figuramc.figura.server.events.Event;
import org.figuramc.figura.server.utils.Hash;

public class AvatarDeletionException extends Event {
    private final Hash hash;
    private final Exception exception;
    public AvatarDeletionException(Hash hash, Exception e) {
        this.hash = hash;
        exception = e;
    }

    public Hash hash() {
        return hash;
    }

    public Exception exception() {
        return exception;
    }
}
