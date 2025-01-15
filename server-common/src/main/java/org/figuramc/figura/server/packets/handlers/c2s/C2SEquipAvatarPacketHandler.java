package org.figuramc.figura.server.packets.handlers.c2s;

import org.figuramc.figura.server.FiguraServer;
import org.figuramc.figura.server.FiguraUser;
import org.figuramc.figura.server.packets.c2s.C2SEquipAvatarsPacket;
import org.figuramc.figura.server.packets.s2c.S2CNotifyPacket;
import org.figuramc.figura.server.utils.IFriendlyByteBuf;

public class C2SEquipAvatarPacketHandler extends AuthorizedC2SPacketHandler<C2SEquipAvatarsPacket> {
    public C2SEquipAvatarPacketHandler(FiguraServer parent) {
        super(parent);
    }

    @Override
    protected void handle(FiguraUser sender, C2SEquipAvatarsPacket packet) {
        packet.avatars().forEach((id, pair) -> sender.setEquippedAvatar(id, pair.hash(), pair.ehash()));
        FiguraServer.getInstance().userManager().forEachUser(user -> {
            if (user != sender)
                user.sendPacket(new S2CNotifyPacket(sender.uuid()));
        });
    }

    @Override
    public C2SEquipAvatarsPacket serialize(IFriendlyByteBuf byteBuf) {
        return new C2SEquipAvatarsPacket(byteBuf);
    }
}
