package com.myadblocker.test;

public class PacketInspector {

    public static boolean isDnsPacket(byte[] packet, int length) {

        if (packet == null || length < 28) {
            return false;
        }

        int version = (packet[0] >> 4) & 0x0F;
        int ihl = (packet[0] & 0x0F) * 4;

        if (version != 4 || ihl < 20 || length < ihl + 8) {
            return false;
        }

        int protocol = packet[9] & 0xFF;

        if (protocol != 17) {
            return false;
        }

        int sourcePort =
                ((packet[ihl] & 0xFF) << 8)
                | (packet[ihl + 1] & 0xFF);

        int destinationPort =
                ((packet[ihl + 2] & 0xFF) << 8)
                | (packet[ihl + 3] & 0xFF);

        return sourcePort == 53 || destinationPort == 53;
    }
}
