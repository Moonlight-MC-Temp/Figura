package org.figuramc.figura.server.events.avatars;

import org.figuramc.figura.server.events.ReturnableEvent;
import org.figuramc.figura.server.utils.Hash;

import java.util.concurrent.CompletableFuture;

public class AvatarExistenceFetchEvent extends ReturnableEvent<Boolean> {
    private final Hash hash;

    public AvatarExistenceFetchEvent(Hash hash) {
        this.hash = hash;
    }

    public Hash hash() {
        return hash;
    }
}
