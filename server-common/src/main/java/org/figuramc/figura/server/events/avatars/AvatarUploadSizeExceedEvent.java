package org.figuramc.figura.server.events.avatars;

import org.figuramc.figura.server.events.CancellableEvent;

import java.util.UUID;

public class AvatarUploadSizeExceedEvent extends CancellableEvent {
    private final UUID sender;
    private final int size;

    public AvatarUploadSizeExceedEvent(UUID sender, int size) {
        this.sender = sender;
        this.size = size;
    }

    public UUID sender() {
        return sender;
    }

    public int size() {
        return size;
    }
}
