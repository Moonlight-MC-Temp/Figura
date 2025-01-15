package org.figuramc.figura.server.packets.handlers.s2c;

import org.figuramc.figura.avatar.AvatarManager;
import org.figuramc.figura.server.packets.s2c.S2CNotifyPacket;
import org.figuramc.figura.server.utils.IFriendlyByteBuf;

public class S2CNotifyPacketHandler extends ConnectedPacketHandler<S2CNotifyPacket> {
    @Override
    protected void handlePacket(S2CNotifyPacket packet) {
        AvatarManager.clearAvatars(packet.target());
    }

    @Override
    public S2CNotifyPacket serialize(IFriendlyByteBuf byteBuf) {
        return new S2CNotifyPacket(byteBuf);
    }
}
