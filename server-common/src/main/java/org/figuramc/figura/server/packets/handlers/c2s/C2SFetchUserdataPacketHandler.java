package org.figuramc.figura.server.packets.handlers.c2s;

import org.figuramc.figura.server.FiguraServer;
import org.figuramc.figura.server.FiguraUser;
import org.figuramc.figura.server.FiguraUserManager;
import org.figuramc.figura.server.packets.c2s.C2SFetchUserdataPacket;
import org.figuramc.figura.server.packets.s2c.S2CUserdataNotFoundPacket;
import org.figuramc.figura.server.packets.s2c.S2CUserdataPacket;
import org.figuramc.figura.server.utils.IFriendlyByteBuf;

import java.util.BitSet;
import java.util.UUID;

public class C2SFetchUserdataPacketHandler extends AuthorizedC2SPacketHandler<C2SFetchUserdataPacket> {

    public C2SFetchUserdataPacketHandler(FiguraServer parent) {
        super(parent);
    }

    @Override
    protected void handle(FiguraUser sender, C2SFetchUserdataPacket packet) {
        FiguraUserManager manager = parent.userManager();
        UUID target = packet.target();
        if (manager.userExists(target)) {
            FiguraUser user = manager.getUser(packet.target());
            BitSet badges = user.prideBadges();
            sender.sendPacket(new S2CUserdataPacket(packet.transactionId(), target, badges, user.equippedAvatar()));
        }
        else {
            sender.sendPacket(new S2CUserdataNotFoundPacket(packet.transactionId()));
        }
    }

    @Override
    public C2SFetchUserdataPacket serialize(IFriendlyByteBuf byteBuf) {
        return new C2SFetchUserdataPacket(byteBuf);
    }
}
