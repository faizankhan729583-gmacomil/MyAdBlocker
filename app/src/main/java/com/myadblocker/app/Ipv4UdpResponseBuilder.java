package com.myadblocker.test;

public class Ipv4UdpResponseBuilder {

    public static byte[] buildResponse(
            byte[] requestPacket,
            int requestLength,
            byte[] dnsResponse) {

        if (requestPacket == null ||
                dnsResponse == null ||
                requestLength < 28) {
            return null;
        }

        int ihl = (requestPacket[0] & 0x0F) * 4;

        if (ihl < 20 || requestLength < ihl + 8) {
            return null;
        }

        int udpOffset = ihl;

        int sourcePort =
                ((requestPacket[udpOffset] & 0xFF) << 8)
                | (requestPacket[udpOffset + 1] & 0xFF);

        int destinationPort =
                ((requestPacket[udpOffset + 2] & 0xFF) << 8)
                | (requestPacket[udpOffset + 3] & 0xFF);

        int totalLength = ihl + 8 + dnsResponse.length;

        byte[] response = new byte[totalLength];

        // Copy original IP header
        System.arraycopy(
                requestPacket,
                0,
                response,
                0,
                ihl
        );

        // Swap source and destination IP addresses
        for (int i = 0; i < 4; i++) {
            response[12 + i] = requestPacket[16 + i];
            response[16 + i] = requestPacket[12 + i];
        }

        // IPv4 total length
        response[2] = (byte) ((totalLength >> 8) & 0xFF);
        response[3] = (byte) (totalLength & 0xFF);

        // UDP source port = original destination port
        response[udpOffset] = (byte) ((destinationPort >> 8) & 0xFF);
        response[udpOffset + 1] = (byte) (destinationPort & 0xFF);

        // UDP destination port = original source port
        response[udpOffset + 2] = (byte) ((sourcePort >> 8) & 0xFF);
        response[udpOffset + 3] = (byte) (sourcePort & 0xFF);

        // UDP length
        int udpLength = 8 + dnsResponse.length;

        response[udpOffset + 4] =
                (byte) ((udpLength >> 8) & 0xFF);

        response[udpOffset + 5] =
                (byte) (udpLength & 0xFF);

        // UDP checksum initially zero
        response[udpOffset + 6] = 0;
        response[udpOffset + 7] = 0;

        // Copy DNS response
        System.arraycopy(
                dnsResponse,
                0,
                response,
                udpOffset + 8,
                dnsResponse.length
        );

        // IPv4 checksum
        response[10] = 0;
        response[11] = 0;

        int checksum = checksum(response, 0, ihl);

        response[10] = (byte) ((checksum >> 8) & 0xFF);
        response[11] = (byte) (checksum & 0xFF);

        return response;
    }

    private static int checksum(byte[] data, int offset, int length) {

        long sum = 0;

        for (int i = offset; i < offset + length; i += 2) {

            int high = data[i] & 0xFF;
            int low = 0;

            if (i + 1 < offset + length) {
                low = data[i + 1] & 0xFF;
            }

            sum += (high << 8) | low;

            while ((sum >> 16) != 0) {
                sum = (sum & 0xFFFF) + (sum >> 16);
            }
        }

        return (int) (~sum) & 0xFFFF;
    }
}
