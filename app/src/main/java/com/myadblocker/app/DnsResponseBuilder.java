package com.myadblocker.test;

public class DnsResponseBuilder {

    public static byte[] createBlockedResponse(byte[] query, int length) {

        if (query == null || length < 12) {
            return null;
        }

        int questionEnd = findQuestionEnd(query, length);

        if (questionEnd < 0 || questionEnd + 4 > length) {
            return null;
        }

        int responseLength = questionEnd + 4;

        byte[] response = new byte[responseLength];

        // Transaction ID
        response[0] = query[0];
        response[1] = query[1];

        // Response + Authoritative Answer + NXDOMAIN
        response[2] = (byte) 0x84;
        response[3] = (byte) 0x83;

        // One question
        response[4] = 0;
        response[5] = 1;

        // No answers
        response[6] = 0;
        response[7] = 0;

        // No authority records
        response[8] = 0;
        response[9] = 0;

        // No additional records
        response[10] = 0;
        response[11] = 0;

        // Copy original question section
        System.arraycopy(
                query,
                12,
                response,
                12,
                responseLength - 12
        );

        return response;
    }

    private static int findQuestionEnd(byte[] packet, int length) {

        int position = 12;

        while (position < length) {

            int size = packet[position] & 0xFF;

            if (size == 0) {
                return position;
            }

            if ((size & 0xC0) == 0xC0) {
                return -1;
            }

            if (size > 63) {
                return -1;
            }

            position++;

            if (position + size > length) {
                return -1;
            }

            position += size;
        }

        return -1;
    }
}
