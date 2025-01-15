package org.figuramc.figura.server;

import org.figuramc.figura.server.packets.CustomFSBPacket;

import java.util.HashMap;

public class FiguraCustomPackets {
    private final HashMap<Integer, String> idMap = new HashMap<>();
    private final HashMap<String, CustomPacketListener> listeners = new HashMap<>();

    public interface CustomPacketListener {
        void dispatch(String packetName, FiguraUser sender, byte[] data);
    }

    /**
     * Registers a new listener for specified packet. Rewrites previous for specified packetName listener.
     * @param packetName name of packet to listen to
     * @param listener listener object
     */
    public void registerListener(String packetName, CustomPacketListener listener) {
        listeners.put(packetName, listener);
        idMap.put(packetName.hashCode(), packetName);
    }

    public void handlePacket(FiguraUser sender, CustomFSBPacket packet) {
        String packetName = idMap.get(packet.id());
        CustomPacketListener listener = listeners.get(packetName);
        if (listener != null) {
            listener.dispatch(packetName, sender, packet.data());
        }
    }
}
