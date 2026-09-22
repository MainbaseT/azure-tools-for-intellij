package com.microsoft.azure.toolkit.intellij.appmod.utils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Utility class to generate a machine ID based on MAC address.
 *
 * Uses NetUtils.getMac() which executes system commands (getmac on Windows,
 * ifconfig/ip on Unix) to get the MAC address. The MAC is then normalized to match
 * Node.js os.networkInterfaces() format (lowercase + colon separator) before hashing.
 *
 * Windows getmac command returns MACs in the order matching Node.js Friendly Name sorting,
 * so the first valid MAC from getmac matches what Node.js would return.
 */
public class MachineIdUtils {

    private static class MachineIdHolder {
        static final String MACHINE_ID = computeMachineId();

        private static String computeMachineId() {
            try {
                // Get MAC address using NetUtils (uses getmac command on Windows, ifconfig/ip on Unix)
                String macAddress = NetUtils.getMac();
                if (macAddress == null || macAddress.trim().isEmpty()) {
                    return null;
                }

                // Normalize MAC format to match Node.js: lowercase + colon separator
                // NetUtils returns: 00-0D-3A-09-37-E5 (uppercase + hyphen on Windows)
                // Node.js returns:  00:0d:3a:09:37:e5 (lowercase + colon)
                String normalizedMac = macAddress.replace("-", ":").toLowerCase();

                // SHA-256 hash of the normalized MAC address
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                byte[] hash = digest.digest(normalizedMac.getBytes(StandardCharsets.UTF_8));
                return bytesToHex(hash);
            } catch (Exception e) {
                return null;
            }
        }
    }

    public static String getMachineId() {
        if (MachineIdUtils.MachineIdHolder.MACHINE_ID == null) {
            return java.util.UUID.randomUUID().toString().toLowerCase();
        }
        return MachineIdUtils.MachineIdHolder.MACHINE_ID;
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b & 0xFF));
        }
        return sb.toString();
    }
}
