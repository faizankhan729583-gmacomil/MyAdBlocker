package com.myadblocker.test;

public class DnsResponseBuilderTest {

    public static void main(String[] args) {

        byte[] query = new byte[] {
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

        byte[] response =
                DnsResponseBuilder.createBlockedResponse(query, query.length);

        if (response == null) {
            System.out.println("Response creation FAILED");
            return;
        }

        System.out.println("Response created successfully");
        System.out.println("Response size: " + response.length + " bytes");

        int flags =
                ((response[2] & 0xFF) << 8)
                | (response[3] & 0xFF);

        System.out.printf("DNS flags: 0x%04X%n", flags);

        if ((flags & 0x000F) == 3) {
            System.out.println("NXDOMAIN: BLOCKED");
        } else {
            System.out.println("NXDOMAIN flag missing");
        }
    }
}
