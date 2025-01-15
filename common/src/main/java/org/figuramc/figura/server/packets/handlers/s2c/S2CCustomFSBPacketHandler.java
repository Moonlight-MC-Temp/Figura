package org.figuramc.figura.server.packets.handlers.s2c;

import org.figuramc.figura.FiguraMod;
import org.figuramc.figura.avatar.Avatar;
import org.figuramc.figura.avatar.AvatarManager;
import org.figuramc.figura.lua.api.ServerPacketsAPI;
import org.figuramc.figura.server.packets.CustomFSBPacket;
import org.figuramc.figura.server.utils.IFriendlyByteBuf;

public class S2CCustomFSBPacketHandler extends ConnectedPacketHandler<CustomFSBPacket> {
    @Override
    protected void handlePacket(CustomFSBPacket packet) {
        ServerPacketsAPI.handlePacket(packet.id(), packet.data());
    }

    @Override
    public CustomFSBPacket serialize(IFriendlyByteBuf byteBuf) {
        return new CustomFSBPacket(byteBuf);
    }
}
