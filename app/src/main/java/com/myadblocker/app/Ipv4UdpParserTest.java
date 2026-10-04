package com.myadblocker.test;

import java.util.Arrays;

public class Ipv4UdpParserTest {

    public static void main(String[] args) {

        byte[] dns = new byte[] {
                0x12, 0x34,
                0x01, 0x00,
                0x00, 0x01,
                0x00, 0x00,
                0x00, 0x00,
                0x00, 0x00,

                0x0B,
                'd','o','u','b','l','e','c','l','i','c','k',
                0x03,
                'n','e','t',
                0x00,

                0x00, 0x01,
                0x00, 0x01
        };

        int udpLength = 8 + dns.length;
        int totalLength = 20 + udpLength;

        byte[] packet = new byte[totalLength];

        // IPv4 header
        packet[0] = 0x45;
        packet[2] = (byte) ((totalLength >> 8) & 0xFF);
        packet[3] = (byte) (totalLength & 0xFF);
        packet[8] = 64;
        packet[9] = 17;

        // UDP ports: source 12345, destination 53
        packet[20] = 0x30;
        packet[21] = 0x39;
        packet[22] = 0x00;
        packet[23] = 0x35;

        packet[24] = (byte) ((udpLength >> 8) & 0xFF);
        packet[25] = (byte) (udpLength & 0xFF);

        System.arraycopy(dns, 0, packet, 28, dns.length);

        byte[] result =
                Ipv4UdpParser.getDnsPayload(packet, packet.length);

        String domain =
                DnsQueryParser.getDomain(result, result.length);

        System.out.println("Detected domain: " + domain);
    }
}
