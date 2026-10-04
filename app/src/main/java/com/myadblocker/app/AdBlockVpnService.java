package com.myadblocker.test;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.net.VpnService;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.system.Os;
import android.system.OsConstants;
import android.system.StructPollfd;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class AdBlockVpnService extends VpnService {

    private static final String TAG = "AdBlockVpn";
    private static final String CHANNEL_ID = "adblock_vpn";
    private static final int NOTIF_ID = 1;

    public static final String ACTION_STOP = "com.myadblocker.test.STOP";

    // UI is ko baad mein parh sakti hai (VPN ON/OFF + counter)
    public static volatile boolean isRunning = false;
    public static final AtomicInteger blockedCount = new AtomicInteger(0);

    private ParcelFileDescriptor vpnInterface;
    private Thread packetThread;
    private ExecutorService dnsPool;
    private DomainBlocker blocker;
    private volatile boolean running = false;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (intent != null && ACTION_STOP.equals(intent.getAction())) {
            shutdown();
            stopSelf();
            return START_NOT_STICKY;
        }

        // Duplicate start protection
        if (isRunning && vpnInterface != null) {
            return START_STICKY;
        }

        startForeground(NOTIF_ID, buildNotification());

        Builder builder = new Builder();
        builder.setSession("My Ad Blocker")
                .addAddress("10.0.0.2", 32)
                .addRoute("10.0.0.1", 32)
                .addDnsServer("10.0.0.1");

        vpnInterface = builder.establish();

        if (vpnInterface == null) {
            stopSelf();
            return START_NOT_STICKY;
        }

        // Ek hi baar blocklist load hogi
        blocker = new DomainBlocker(getApplicationContext());
        dnsPool = Executors.newFixedThreadPool(8);
        isRunning = true;
        running = true;

        final ParcelFileDescriptor pfd = vpnInterface;

        packetThread = new Thread(() -> runLoop(pfd), "vpn-packet-loop");
        packetThread.start();

        return START_STICKY;
    }

    private void runLoop(ParcelFileDescriptor pfd) {
        try {
            final FileInputStream input = new FileInputStream(pfd.getFileDescriptor());
            final FileOutputStream output = new FileOutputStream(pfd.getFileDescriptor());
            byte[] buf = new byte[32767];

            StructPollfd p = new StructPollfd();
            p.fd = pfd.getFileDescriptor();
            p.events = (short) OsConstants.POLLIN;
            StructPollfd[] fds = new StructPollfd[]{p};

            while (running && !Thread.currentThread().isInterrupted()) {

                // Har 500ms baad stop flag check hota hai (read ab atakta nahi)
                p.revents = 0;
                int ready = Os.poll(fds, 500);
                if (ready <= 0) {
                    continue;
                }
                if ((p.revents & (OsConstants.POLLERR
                        | OsConstants.POLLHUP
                        | OsConstants.POLLNVAL)) != 0) {
                    break;
                }
                if ((p.revents & OsConstants.POLLIN) == 0) {
                    continue;
                }

                int length = input.read(buf);
                if (length <= 0) {
                    continue;
                }

                if (!PacketInspector.isDnsPacket(buf, length)) {
                    continue; // non-DNS ignore (log nahi, warna slow hota hai)
                }

                // Buffer dobara use hoga, isliye copy bana kar worker ko dein
                final byte[] packet = Arrays.copyOf(buf, length);
                final int len = length;

                dnsPool.execute(() -> handleDns(packet, len, output));
            }
        } catch (Exception e) {
            Log.e(TAG, "VPN loop ended: " + e.getMessage());
        }
    }

    private void handleDns(byte[] packet, int length, FileOutputStream output) {
        try {
            byte[] dnsPayload = Ipv4UdpParser.getDnsPayload(packet, length);
            if (dnsPayload == null) return;

            String domain = DnsQueryParser.getDomain(dnsPayload, dnsPayload.length);
            if (domain == null) return;

            byte[] dnsResponse;

            if (blocker.isBlocked(domain)) {
                Log.d(TAG, "BLOCKED: " + domain);
                blockedCount.incrementAndGet();
                dnsResponse = DnsResponseBuilder.createBlockedResponse(
                        dnsPayload, dnsPayload.length);
            } else {
                dnsResponse = DnsForwarder.forward(this, dnsPayload);
                if (dnsResponse == null) {
                    Log.w(TAG, "FORWARD FAILED: " + domain);
                    dnsResponse = servFail(dnsPayload);
                }
            }

            if (dnsResponse == null) return;

            byte[] responsePacket = Ipv4UdpResponseBuilder.buildResponse(
                    packet, length, dnsResponse);

            if (responsePacket != null) {
                synchronized (output) {
                    output.write(responsePacket, 0, responsePacket.length);
                    output.flush();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "handleDns error: " + e.getMessage());
        }
    }

    // Upstream fail ho to app ko foran error mile, hang na ho
    private byte[] servFail(byte[] query) {
        if (query == null || query.length < 12) return null;
        byte[] r = Arrays.copyOf(query, query.length);
        r[2] = (byte) 0x81;  // QR=1, RD=1
        r[3] = (byte) 0x82;  // RA=1, RCODE=2 (SERVFAIL)
        return r;
    }

    private Notification buildNotification() {
        NotificationManager nm = getSystemService(NotificationManager.class);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(
                    CHANNEL_ID, "Ad Blocker", NotificationManager.IMPORTANCE_LOW);
            nm.createNotificationChannel(ch);
        }

        Notification.Builder nb = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                ? new Notification.Builder(this, CHANNEL_ID)
                : new Notification.Builder(this);

        return nb.setContentTitle("My Ad Blocker")
                .setContentText("Ad blocking is active")
                .setSmallIcon(android.R.drawable.ic_lock_lock)
                .setOngoing(true)
                .build();
    }

    @Override
    public void onDestroy() {
        shutdown();
        super.onDestroy();
    }

    private void shutdown() {
        isRunning = false;
        running = false;

        if (packetThread != null) {
            packetThread.interrupt();
            packetThread = null;
        }

        if (dnsPool != null) {
            dnsPool.shutdownNow();
            dnsPool = null;
        }

        if (vpnInterface != null) {
            try {
                vpnInterface.close(); // blocked read() ko bhi tod deta hai
            } catch (Exception ignored) {
            }
            vpnInterface = null;
        }

        stopForeground(true);
    }
}