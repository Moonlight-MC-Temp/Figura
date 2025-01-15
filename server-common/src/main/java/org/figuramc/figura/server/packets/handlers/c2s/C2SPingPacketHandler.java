package org.figuramc.figura.server.packets.handlers.c2s;

import org.figuramc.figura.server.FiguraServer;
import org.figuramc.figura.server.FiguraUser;
import org.figuramc.figura.server.packets.c2s.C2SPingPacket;
import org.figuramc.figura.server.packets.s2c.S2CPingErrorPacket;
import org.figuramc.figura.server.packets.s2c.S2CPingPacket;
import org.figuramc.figura.server.utils.IFriendlyByteBuf;

public class C2SPingPacketHandler extends AuthorizedC2SPacketHandler<C2SPingPacket> {
    public C2SPingPacketHandler(FiguraServer parent) {
        super(parent);
    }

    @Override
    protected void handle(FiguraUser sender, C2SPingPacket packet) {
        var counter = sender.pingCounter();
        if (counter.pingsSent() > parent.config().pingsRateLimit()) {
            sender.sendPacket(new S2CPingErrorPacket(S2CPingErrorPacket.Error.RATE_LIMIT));
        }
        if (counter.bytesSent() + packet.data().length > parent.config().pingsSizeLimit()) {
            sender.sendPacket(new S2CPingErrorPacket(S2CPingErrorPacket.Error.PING_SIZE));
        }
        counter.addPing(packet.data().length);
        parent.userManager().forEachUser(user -> {
            if (packet.sync() || user != sender) user.sendPacket(new S2CPingPacket(sender.uuid(), packet.id(), packet.data()));
        });
    }

    @Override
    public C2SPingPacket serialize(IFriendlyByteBuf byteBuf) {
        return new C2SPingPacket(byteBuf);
    }
}
