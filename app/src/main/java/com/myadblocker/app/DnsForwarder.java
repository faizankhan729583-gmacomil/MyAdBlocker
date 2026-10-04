package com.myadblocker.test;

import android.net.VpnService;
import android.util.Log;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class DnsForwarder {

    private static final String TAG = "DnsForwarder";
    private static final String[] SERVERS = {"8.8.8.8", "1.1.1.1", "9.9.9.9"};

    public static byte[] forward(VpnService service, byte[] dnsQuery) {

        if (dnsQuery == null || dnsQuery.length == 0) {
            return null;
        }

        for (String server : SERVERS) {
            byte[] result = tryServer(service, dnsQuery, server);
            if (result != null) {
                return result;
            }
        }
        return null;
    }

    private static byte[] tryServer(VpnService service, byte[] dnsQuery, String server) {

        DatagramSocket socket = null;

        try {
            socket = new DatagramSocket();

            if (!service.protect(socket)) {
                Log.e(TAG, "protect() failed");
                return null;
            }

            socket.setSoTimeout(2000);

            socket.send(new DatagramPacket(
                    dnsQuery,
                    dnsQuery.length,
                    InetAddress.getByName(server),
                    53));

            DatagramPacket response = new DatagramPacket(new byte[4096], 4096);
            socket.receive(response);

            byte[] result = new byte[response.getLength()];
            System.arraycopy(response.getData(), response.getOffset(),
                    result, 0, response.getLength());
            return result;

        } catch (Exception e) {
            Log.e(TAG, "Server " + server + " failed: " + e.getMessage());
            return null;

        } finally {
            if (socket != null) {
                socket.close();
            }
        }
    }
}