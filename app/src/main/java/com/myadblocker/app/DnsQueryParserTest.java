package com.myadblocker.test;

public class DnsQueryParserTest {

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

        String domain =
                DnsQueryParser.getDomain(query, query.length);

        System.out.println("Detected domain: " + domain);
    }
}
