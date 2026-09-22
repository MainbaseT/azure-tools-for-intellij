package com.microsoft.azure.toolkit.intellij.appmod.utils;

import org.apache.commons.lang3.StringUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NetUtils {

    private static final int COMMAND_TIMEOUT_MS = 5000;
    public static final Pattern INTACT_MAC_PATTERN = Pattern.compile("^([0-9A-Fa-f]{2}[:-]){5}[0-9A-Fa-f]{2}$");
    private static final Pattern MAC_PATTERN = Pattern.compile("([0-9A-Fa-f]{2}[:-]){5}[0-9A-Fa-f]{2}");
    private static final String[] INVALID_MAC_ADDRESS = {"00:00:00:00:00:00", "ff:ff:ff:ff:ff:ff", "ac:de:48:00:11:22"};
    private static final String[][] UNIX_COMMANDS = {
            {"/sbin/ifconfig", "-a"},
            {"/sbin/ip", "link"}
    };
    private static final String[] WINDOWS_COMMAND = {"getmac"};

    public static String getMac() {
        final String commandMac = getMacByCommand();
        if (StringUtils.isNotBlank(commandMac)) {
            return commandMac;
        }
        return getMacByNetworkInterface();
    }

    private static String getMacByCommand() {
        List<String> macs = getMacsByCommand();
        return !macs.isEmpty() ? macs.get(0) : StringUtils.EMPTY;
    }

    private static List<String> getMacsByCommand() {
        List<String> macs = new ArrayList<>();
        final String os = System.getProperty("os.name").toLowerCase();

        if (StringUtils.startsWithIgnoreCase(os, "win")) {
            return getMacsByCommand(WINDOWS_COMMAND);
        }

        // Try Unix commands in order until one succeeds
        for (String[] command : UNIX_COMMANDS) {
            macs = getMacsByCommand(command);
            if (!macs.isEmpty()) {
                return macs;
            }
        }
        return macs;
    }

    private static List<String> getMacsByCommand(String[] command) {
        List<String> macs = new ArrayList<>();
        final StringBuilder ret = new StringBuilder();
        Process process = null;
        try {
            final ProcessBuilder probuilder = new ProcessBuilder(command);
            probuilder.redirectErrorStream(true);
            process = probuilder.start();
            try (final InputStream inputStream = process.getInputStream();
                 final InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                 final BufferedReader br = new BufferedReader(inputStreamReader)) {
                String tmp;
                while ((tmp = br.readLine()) != null) {
                    ret.append(tmp);
                }
            }
            boolean finished = process.waitFor(COMMAND_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            if (!finished || process.exitValue() != 0) {
                return macs;
            }
        } catch (IOException ex) {
            return macs;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return macs;
        } finally {
            if (process != null && process.isAlive()) {
                process.destroy();
            }
        }
        String commandMacsString = ret.toString();

        Matcher matcher = MAC_PATTERN.matcher(commandMacsString);
        while (matcher.find()) {
            String mac = matcher.group(0);
            if (isValidMac(mac)) {
                macs.add(mac);
            }
        }
        return macs;
    }

    private static String getMacByNetworkInterface() {
        List<String> macs = getMacsByNetworkInterface();
        if (macs.isEmpty()) {
            return StringUtils.EMPTY;
        }
        return macs.get(0);
    }

    private static List<String> getMacsByNetworkInterface() {
        List<String> macs = new ArrayList<>();
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            if (interfaces == null) {
                return macs;
            }
            while (interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();
                if (networkInterface.isLoopback()) {
                    continue;
                }
                if (networkInterface.getHardwareAddress() != null) {
                    byte[] mac = networkInterface.getHardwareAddress();
                    // Refers https://www.mkyong.com/java/how-to-get-mac-address-in-java/
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < mac.length; i++) {
                        sb.append(String.format("%02X%s", mac[i] & 0xFF, (i < mac.length - 1) ? "-" : ""));
                    }
                    String macStr = sb.toString();
                    if (isValidMac(macStr)) {
                        macs.add(macStr);
                    }
                }
            }
        } catch (SocketException e) {
            return macs;
        }
        return macs;
    }

    private static boolean isValidMac(String mac) {
        if (StringUtils.isEmpty(mac)) {
            return false;
        }
        if (!isValidRawMac(mac)) {
            return false;
        }
        final String fixedMac = mac.replaceAll("-", ":");
        return !StringUtils.equalsAnyIgnoreCase(fixedMac, INVALID_MAC_ADDRESS);
    }

    private static boolean isValidRawMac(String raw) {
        return StringUtils.isNotEmpty(raw) && INTACT_MAC_PATTERN.matcher(raw).find();
    }
}
