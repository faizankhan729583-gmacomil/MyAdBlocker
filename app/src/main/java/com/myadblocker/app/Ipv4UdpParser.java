package com.myadblocker.test;

import java.nio.ByteBuffer;

public class Ipv4UdpParser {

    public static byte[] getDnsPayload(byte[] packet, int length) {

        if (packet == null || length < 28) {
            return null;
        }

        int version = (packet[0] >> 4) & 0x0F;
        int ihl = (packet[0] & 0x0F) * 4;

        if (version != 4 || ihl < 20 || length < ihl + 8) {
            return null;
        }

        int protocol = packet[9] & 0xFF;

        if (protocol != 17) {
            return null;
        }

        int sourcePort =
                ((packet[ihl] & 0xFF) << 8)
                | (packet[ihl + 1] & 0xFF);

        int destinationPort =
                ((packet[ihl + 2] & 0xFF) << 8)
                | (packet[ihl + 3] & 0xFF);

        if (sourcePort != 53 && destinationPort != 53) {
            return null;
        }

        int udpHeaderLength = 8;
        int payloadStart = ihl + udpHeaderLength;

        if (payloadStart >= length) {
            return null;
        }

        int payloadLength = length - payloadStart;

        return java.util.Arrays.copyOfRange(
                packet,
                payloadStart,
                payloadStart + payloadLength
        );
    }
}
