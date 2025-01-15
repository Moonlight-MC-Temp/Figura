package org.figuramc.figura.server.events.users;

import org.figuramc.figura.server.FiguraUser;
import org.figuramc.figura.server.events.ReturnableEvent;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class LoadPlayerDataEvent extends ReturnableEvent<FiguraUser> {
    private final UUID player;

    public LoadPlayerDataEvent(UUID player) {
        this.player = player;
    }

    public UUID player() {
        return player;
    }
}
