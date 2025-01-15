package org.figuramc.figura.server.events.users;

import org.figuramc.figura.server.FiguraUser;
import org.figuramc.figura.server.events.CancellableEvent;

public class SavePlayerDataEvent extends CancellableEvent {
    private final FiguraUser user;

    public SavePlayerDataEvent(FiguraUser user) {
        this.user = user;
    }

    public FiguraUser user() {
        return user;
    }
}
