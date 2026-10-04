package com.myadblocker.test;

import java.util.HashSet;
import java.util.Set;

public class DomainBlockerTest {

    public static void main(String[] args) {

        Set<String> domains = new HashSet<>();

        domains.add("doubleclick.net");
        domains.add("googlesyndication.com");

        System.out.println("doubleclick.net = "
                + (domains.contains("doubleclick.net") ? "BLOCKED" : "ALLOWED"));

        System.out.println("google.com = "
                + (domains.contains("google.com") ? "BLOCKED" : "ALLOWED"));
    }
}
