package dev.monkeypatch.rctiming.config;

import java.net.InetAddress;
import java.net.UnknownHostException;

/** Whether an address names this machine, for settings that allow an unencrypted connection only to it. */
public final class LoopbackHosts {

    private LoopbackHosts() {
    }

    /** True for localhost and loopback IP literals (127.x, ::1). Never looks a name up. */
    public static boolean isLoopback(String host) {
        if (host == null) {
            return false;
        }
        if ("localhost".equalsIgnoreCase(host)) {
            return true;
        }
        String literal = host.startsWith("[") && host.endsWith("]") ? host.substring(1, host.length() - 1) : host;
        boolean ipLiteral = literal.matches("\\d{1,3}(\\.\\d{1,3}){3}") || literal.contains(":");
        if (!ipLiteral) {
            return false;
        }
        try {
            return InetAddress.getByName(literal).isLoopbackAddress();
        } catch (UnknownHostException e) {
            return false;
        }
    }
}
