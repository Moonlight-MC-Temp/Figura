package org.figuramc.figura.server.events.avatars;

import org.figuramc.figura.server.events.CancellableEvent;
import org.figuramc.figura.server.events.ReturnableEvent;
import org.figuramc.figura.server.utils.Hash;

import java.util.concurrent.CompletableFuture;

public class RemoveAvatarDataEvent extends CancellableEvent {
    private final Hash avatarHash;

    public RemoveAvatarDataEvent(Hash avatarHash) {
        this.avatarHash = avatarHash;
    }

    public Hash avatarHash() {
        return avatarHash;
    }
}
