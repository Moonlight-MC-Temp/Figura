package org.figuramc.figura.server.packets.handlers.c2s;

import org.figuramc.figura.server.FiguraServer;
import org.figuramc.figura.server.FiguraUser;
import org.figuramc.figura.server.packets.Packet;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public abstract class AuthorizedC2SPacketHandler<P extends Packet> implements C2SPacketHandler<P> {
    protected final FiguraServer parent;

    protected AuthorizedC2SPacketHandler(FiguraServer parent) {
        this.parent = parent;
    }

    @Override
    public final void handle(UUID sender, P packet) {
        FiguraUser user = parent.userManager().getUserOrNull(sender);
        if (user == null || user.offline()) return;
        handle(user, packet);
    }

    protected abstract void handle(FiguraUser sender, P packet);
}
