package org.figuramc.figura.server.packets.handlers.c2s;

import org.figuramc.figura.server.FiguraServer;
import org.figuramc.figura.server.FiguraUser;
import org.figuramc.figura.server.packets.c2s.C2SDeleteAvatarPacket;
import org.figuramc.figura.server.utils.IFriendlyByteBuf;

public class C2SDeleteAvatarPacketHandler extends AuthorizedC2SPacketHandler<C2SDeleteAvatarPacket> {
    public C2SDeleteAvatarPacketHandler(FiguraServer parent) {
        super(parent);
    }

    @Override
    protected void handle(FiguraUser sender, C2SDeleteAvatarPacket packet) {
        sender.removeOwnedAvatar(packet.avatarId());
        sender.removeEquippedAvatar();
    }

    @Override
    public C2SDeleteAvatarPacket serialize(IFriendlyByteBuf byteBuf) {
        return new C2SDeleteAvatarPacket(byteBuf);
    }
}
