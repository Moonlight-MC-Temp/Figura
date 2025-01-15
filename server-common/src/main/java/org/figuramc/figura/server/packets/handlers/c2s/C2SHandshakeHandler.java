package org.figuramc.figura.server.packets.handlers.c2s;

import org.figuramc.figura.server.FiguraServer;
import org.figuramc.figura.server.FiguraUserManager;
import org.figuramc.figura.server.events.Events;
import org.figuramc.figura.server.events.HandshakeEvent;
import org.figuramc.figura.server.packets.c2s.C2SBackendHandshakePacket;
import org.figuramc.figura.server.packets.s2c.S2CConnectedPacket;
import org.figuramc.figura.server.packets.s2c.S2CRefusedPacket;
import org.figuramc.figura.server.utils.IFriendlyByteBuf;

import java.util.UUID;

public class C2SHandshakeHandler implements C2SPacketHandler<C2SBackendHandshakePacket> {
    private final FiguraServer parent;

    public C2SHandshakeHandler(FiguraServer parent) {
        this.parent = parent;
    }

    @Override
    public C2SBackendHandshakePacket serialize(IFriendlyByteBuf byteBuf) {
        return new C2SBackendHandshakePacket();
    }

    @Override
    public void handle(UUID sender, C2SBackendHandshakePacket packet) {
        if (Events.call(new HandshakeEvent(sender)).isCancelled()) {
            parent.sendPacket(sender, new S2CRefusedPacket());
        }
        else {
            FiguraUserManager manager = parent.userManager();
            var user = manager.setupOnlinePlayer(sender);
            user.sendPacket(parent.getHandshake());
            manager.forEachUser(u -> {
                if (u != user) {
                    u.sendPacket(new S2CConnectedPacket(user.uuid()));
                }
            });
        }
    }
}
