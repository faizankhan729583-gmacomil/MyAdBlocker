package com.myadblocker.test;

public class Ipv4UdpResponseBuilderTest {

    public static void main(String[] args) {

        byte[] request = new byte[32];

        // IPv4
        request[0] = 0x45;

        // Source IP: 10.0.0.2
        request[12] = 10;
        request[13] = 0;
        request[14] = 0;
        request[15] = 2;

        // Destination IP: 8.8.8.8
        request[16] = 8;
        request[17] = 8;
        request[18] = 8;
        request[19] = 8;

        // UDP
        request[9] = 17;

        // Source port: 53000
        request[20] = (byte) (53000 >> 8);
        request[21] = (byte) 53000;

        // Destination port: 53
        request[22] = 0;
        request[23] = 53;

        // Simple DNS response
        byte[] dns = new byte[] {
                0x12, 0x34,
                (byte) 0x84, (byte) 0x83,
                0x00, 0x01,
                0x00, 0x00,
                0x00, 0x00,
                0x00, 0x00
        };

        byte[] response =
                Ipv4UdpResponseBuilder.buildResponse(
                        request,
                        request.length,
                        dns
                );

        if (response == null) {
            System.out.println("Response FAILED");
            return;
        }

        System.out.println("Response created successfully");
        System.out.println("Response size: " + response.length);

        System.out.println(
                "Source IP: "
                        + (response[12] & 0xFF) + "."
                        + (response[13] & 0xFF) + "."
                        + (response[14] & 0xFF) + "."
                        + (response[15] & 0xFF)
        );

        System.out.println(
                "Destination IP: "
                        + (response[16] & 0xFF) + "."
                        + (response[17] & 0xFF) + "."
                        + (response[18] & 0xFF) + "."
                        + (response[19] & 0xFF)
        );
    }
}
