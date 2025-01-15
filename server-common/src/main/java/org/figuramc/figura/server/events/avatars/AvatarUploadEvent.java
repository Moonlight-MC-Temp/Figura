package org.figuramc.figura.server.events.avatars;

import org.figuramc.figura.server.events.CancellableEvent;

import java.util.UUID;

public class AvatarUploadEvent extends CancellableEvent {
    private final UUID sender;
    private final String avatarId;

    public AvatarUploadEvent(UUID sender, String avatarId) {
        this.sender = sender;
        this.avatarId = avatarId;
    }

    public UUID sender() {
        return sender;
    }

    public String avatarId() {
        return avatarId;
    }
}
