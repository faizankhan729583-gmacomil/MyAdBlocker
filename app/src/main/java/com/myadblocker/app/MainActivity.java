package com.myadblocker.test;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.VpnService;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {

    private static final int VPN_REQUEST_CODE = 100;
    private static final int NOTIF_REQUEST_CODE = 101;

    private TextView statusView;
    private TextView counterView;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private final Runnable uiUpdater = new Runnable() {
        @Override
        public void run() {
            boolean on = AdBlockVpnService.isRunning;
            statusView.setText(on ? "\uD83D\uDFE2  VPN ON" : "\uD83D\uDD34  VPN OFF");
            counterView.setText("Ads Blocked: " + AdBlockVpnService.blockedCount.get());
            handler.postDelayed(this, 500);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission("android.permission.POST_NOTIFICATIONS")
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                    new String[]{"android.permission.POST_NOTIFICATIONS"},
                    NOTIF_REQUEST_CODE);
        }

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding(48, 96, 48, 48);
        root.setBackgroundColor(Color.WHITE);
        root.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        TextView title = new TextView(this);
        title.setText("My Ad Blocker v6");
        title.setTextSize(28);
        title.setTypeface(null, Typeface.BOLD);
        title.setTextColor(Color.BLACK);
        title.setGravity(Gravity.CENTER);

        statusView = new TextView(this);
        statusView.setTextSize(24);
        statusView.setTextColor(Color.BLACK);
        statusView.setGravity(Gravity.CENTER);
        statusView.setPadding(0, 48, 0, 16);

        counterView = new TextView(this);
        counterView.setTextSize(20);
        counterView.setTextColor(Color.DKGRAY);
        counterView.setGravity(Gravity.CENTER);
        counterView.setPadding(0, 0, 0, 48);

        Button startBtn = new Button(this);
        startBtn.setText("START AD BLOCKER");
        startBtn.setTextColor(Color.WHITE);
        startBtn.setBackgroundColor(Color.parseColor("#2E7D32"));
        startBtn.setOnClickListener(v -> {
            Intent intent = VpnService.prepare(MainActivity.this);
            if (intent != null) {
                startActivityForResult(intent, VPN_REQUEST_CODE);
            } else {
                startVpnService();
            }
        });

        Button stopBtn = new Button(this);
        stopBtn.setText("STOP");
        stopBtn.setTextColor(Color.WHITE);
        stopBtn.setBackgroundColor(Color.parseColor("#C62828"));
        stopBtn.setOnClickListener(v -> {
            Toast.makeText(this, "STOP pressed", Toast.LENGTH_SHORT).show();
            stopVpnService();
        });

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 16, 0, 16);

        root.addView(title);
        root.addView(statusView);
        root.addView(counterView);
        root.addView(startBtn, lp);
        root.addView(stopBtn, lp);

        setContentView(root);
    }

    @Override
    protected void onResume() {
        super.onResume();
        handler.post(uiUpdater);
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(uiUpdater);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == VPN_REQUEST_CODE && resultCode == RESULT_OK) {
            startVpnService();
        }
    }

    private void startVpnService() {
        Intent intent = new Intent(this, AdBlockVpnService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
    }

    private void stopVpnService() {
        Intent stop = new Intent(this, AdBlockVpnService.class);
        stop.setAction(AdBlockVpnService.ACTION_STOP);
        startService(stop);
        stopService(new Intent(this, AdBlockVpnService.class));
    }
}