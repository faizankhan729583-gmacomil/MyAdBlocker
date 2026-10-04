package com.myadblocker.test;

import android.content.Context;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Set;

public class DomainBlocker {

    private final Set<String> blockedDomains = new HashSet<>(131072);

    public DomainBlocker(Context context) {
        loadBlockedDomains(context);
    }

    public DomainBlocker(Set<String> domains) {
        if (domains != null) {
            for (String domain : domains) {
                if (domain != null && !domain.trim().isEmpty()) {
                    blockedDomains.add(domain.trim().toLowerCase());
                }
            }
        }
    }

    private void loadBlockedDomains(Context context) {
        try {
            InputStream input = context.getAssets().open("ads.txt");

            BufferedReader reader =
                    new BufferedReader(new InputStreamReader(input));

            String line;

            while ((line = reader.readLine()) != null) {
                line = line.trim().toLowerCase();

                if (!line.isEmpty() && !line.startsWith("#")) {
                    blockedDomains.add(line);
                }
            }

            reader.close();

        } catch (Exception ignored) {
        }
    }

    public int size() {
        return blockedDomains.size();
    }

    public boolean isBlocked(String domain) {

        if (domain == null) {
            return false;
        }

        String d = domain.toLowerCase().trim();

        // trailing dot hata dein (example.com.)
        if (d.endsWith(".")) {
            d = d.substring(0, d.length() - 1);
        }

        // a.b.example.com -> b.example.com -> example.com
        while (!d.isEmpty()) {

            if (blockedDomains.contains(d)) {
                return true;
            }

            int dot = d.indexOf('.');
            if (dot < 0) {
                break;
            }

            d = d.substring(dot + 1);
        }

        return false;
    }
}