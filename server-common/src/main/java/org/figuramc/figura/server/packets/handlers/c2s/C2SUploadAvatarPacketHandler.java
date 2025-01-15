package org.figuramc.figura.server.packets.handlers.c2s;

import org.figuramc.figura.server.FiguraServer;
import org.figuramc.figura.server.FiguraUser;
import org.figuramc.figura.server.packets.AllowIncomingStreamPacket;
import org.figuramc.figura.server.packets.CloseIncomingStreamPacket;
import org.figuramc.figura.server.packets.c2s.C2SUploadAvatarPacket;
import org.figuramc.figura.server.utils.IFriendlyByteBuf;
import org.figuramc.figura.server.utils.StatusCode;

public class C2SUploadAvatarPacketHandler extends AuthorizedC2SPacketHandler<C2SUploadAvatarPacket> {
    public C2SUploadAvatarPacketHandler(FiguraServer parent) {
        super(parent);
    }

    @Override
    protected void handle(FiguraUser sender, C2SUploadAvatarPacket packet) {
        boolean avatarExists = parent.avatarManager().avatarExists(packet.hash());
        if (getNewAvatarsCount(sender, packet.avatarId()) > parent.config().avatarsCountLimit()) {
            sender.sendPacket(new CloseIncomingStreamPacket(packet.streamId(), StatusCode.TOO_MANY_AVATARS));
        }
        if (avatarExists) {
            sender.replaceOrAddOwnedAvatar(packet.avatarId(), packet.hash(), packet.ehash());
            sender.sendPacket(new CloseIncomingStreamPacket(packet.streamId(), StatusCode.ALREADY_EXISTS));
        }
        else {
            parent.avatarManager().receiveAvatar(sender, packet.avatarId(), packet.streamId(), packet.hash(), packet.ehash());
            sender.sendPacket(new AllowIncomingStreamPacket(packet.streamId()));
        }
    }

    private int getNewAvatarsCount(FiguraUser user, String id) {
        return user.ownedAvatars().size() + (user.ownedAvatars().containsKey(id) ? 0 : 1);
    }

    @Override
    public C2SUploadAvatarPacket serialize(IFriendlyByteBuf byteBuf) {
        return new C2SUploadAvatarPacket(byteBuf);
    }
}
