package org.figuramc.figura.server;

import org.figuramc.figura.server.commands.FiguraServerCommandSource;

import java.util.UUID;

public class FiguraSpigotCommandSource implements FiguraServerCommandSource {
    private final UUID player;

    public FiguraSpigotCommandSource(UUID player) {
        this.player = player;
    }

    @Override
    public UUID getExecutorUUID() {
        return player;
    }
}
