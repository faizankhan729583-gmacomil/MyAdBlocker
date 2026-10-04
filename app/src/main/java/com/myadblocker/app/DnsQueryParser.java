package com.myadblocker.test;

public class DnsQueryParser {

    public static String getDomain(byte[] packet, int length) {

        if (packet == null || length < 13) {
            return null;
        }

        try {
            int position = 12;
            StringBuilder domain = new StringBuilder();

            while (position < length) {

                int size = packet[position] & 0xFF;

                if (size == 0) {
                    break;
                }

                // DNS compression pointer: not a normal question name
                if ((size & 0xC0) == 0xC0) {
                    return domain.length() > 0
                            ? domain.toString()
                            : null;
                }

                if (size > 63) {
                    return null;
                }

                position++;

                if (position + size > length) {
                    return null;
                }

                if (domain.length() > 0) {
                    domain.append(".");
                }

                for (int i = 0; i < size; i++) {
                    int value = packet[position + i] & 0xFF;

                    if (value < 32 || value > 126) {
                        return null;
                    }

                    domain.append((char) value);
                }

                position += size;
            }

            if (domain.length() == 0) {
                return null;
            }

            return domain.toString();

        } catch (Exception ignored) {
            return null;
        }
    }
}
