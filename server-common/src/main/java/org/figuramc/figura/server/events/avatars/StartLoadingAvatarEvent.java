package org.figuramc.figura.server.events.avatars;

import org.figuramc.figura.server.events.ReturnableEvent;
import org.figuramc.figura.server.utils.Hash;

import java.util.concurrent.CompletableFuture;

public class StartLoadingAvatarEvent extends ReturnableEvent<byte[]> {
    private final Hash hash;
    public StartLoadingAvatarEvent(Hash hash) {
        this.hash = hash;
    }

    public Hash hash() {
        return hash;
    }
}
