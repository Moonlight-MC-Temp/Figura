package org.figuramc.figura.server.events.avatars;

import org.figuramc.figura.server.events.Event;

import java.util.List;
import java.util.UUID;

public class AvatarMetadataLoadingExceptionEvent extends Event {
    private final byte[] avatarHash;
    private final Throwable exception;
    private final List<UUID> awaitingReceivers;

    public AvatarMetadataLoadingExceptionEvent(byte[] avatarHash, Throwable exception, List<UUID> awaitingReceivers) {
        this.avatarHash = avatarHash;
        this.exception = exception;
        this.awaitingReceivers = awaitingReceivers;
    }

    public byte[] avatarHash() {
        return avatarHash;
    }

    public Throwable exception() {
        return exception;
    }

    public List<UUID> awaitingReceivers() {
        return awaitingReceivers;
    }
}
