package dev.monkeypatch.rctiming.infrastructure.network;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.web.context.WebServerInitializedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * The addresses phones, tablets and boards on the venue network should open (#23): one
 * {@code http://<ip>:<port>/} per network the laptop is on, plus one by its hostname. Logged at
 * start-up and shown on the About page.
 */
@Component
public class LanAddresses {

    private static final Logger log = LoggerFactory.getLogger(LanAddresses.class);

    /** Interfaces that never reach another device: container bridges, VM host-only networks. */
    private static final List<String> VIRTUAL_PREFIXES =
            List.of("docker", "veth", "br-", "virbr", "vmnet", "vboxnet", "cni", "flannel");

    private final String bindAddress;
    private volatile int port;

    public LanAddresses(@Value("${server.address:}") String bindAddress) {
        this.bindAddress = bindAddress == null ? "" : bindAddress.trim();
    }

    @EventListener
    public void onWebServerStarted(WebServerInitializedEvent event) {
        // A separate actuator port would start a second server; the app's own is the one to share
        if (!"management".equals(event.getApplicationContext().getServerNamespace())) {
            port = event.getWebServer().getPort();
        }
    }

    @EventListener(ApplicationReadyEvent.class)
    public void logAddresses() {
        List<String> urls = urls();
        if (urls.isEmpty()) {
            log.info("RC Timing is running, but no network address was found for other devices to use");
        } else {
            log.info("RC Timing is running. Open it from other devices at: {}", String.join("  ", urls));
        }
    }

    /** The URLs to open from other devices, IP addresses first; empty before the server starts. */
    public List<String> urls() {
        if (port <= 0) {
            return List.of();
        }
        Set<String> hosts = new LinkedHashSet<>();
        if (!bindAddress.isEmpty() && !isWildcard(bindAddress)) {
            hosts.add(bindAddress);
        } else {
            hosts.addAll(interfaceAddresses());
            if (!hosts.isEmpty()) {
                hostname().ifPresent(hosts::add);
            }
        }
        List<String> urls = new ArrayList<>();
        for (String host : hosts) {
            urls.add(url(host, port));
        }
        return urls;
    }

    /** {@code http://host:port/}, with an IPv6 address in brackets and no port for 80. */
    static String url(String host, int port) {
        String authority = host.contains(":") && !host.startsWith("[") ? "[" + host + "]" : host;
        return "http://" + authority + (port == 80 ? "" : ":" + port) + "/";
    }

    private static boolean isWildcard(String address) {
        return address.equals("0.0.0.0") || address.equals("::") || address.equals("[::]");
    }

    private static List<String> interfaceAddresses() {
        List<String> addresses = new ArrayList<>();
        try {
            for (NetworkInterface nic : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (!nic.isUp() || nic.isLoopback() || nic.isVirtual() || isVirtualName(nic.getName())) {
                    continue;
                }
                for (InetAddress address : Collections.list(nic.getInetAddresses())) {
                    // IPv4 only: it is what people can read off a screen and type into a phone
                    if (address instanceof Inet4Address && !address.isLoopbackAddress()
                            && !address.isLinkLocalAddress()) {
                        addresses.add(address.getHostAddress());
                    }
                }
            }
        } catch (SocketException e) {
            log.warn("Could not list network interfaces: {}", e.getMessage());
        }
        Collections.sort(addresses);
        return addresses;
    }

    private static boolean isVirtualName(String name) {
        String lower = name.toLowerCase(Locale.ROOT);
        return VIRTUAL_PREFIXES.stream().anyMatch(lower::startsWith);
    }

    private static Optional<String> hostname() {
        try {
            String name = InetAddress.getLocalHost().getHostName();
            if (name == null || name.isBlank() || name.equals("localhost") || name.matches("[0-9.]+")) {
                return Optional.empty();
            }
            return Optional.of(name);
        } catch (UnknownHostException e) {
            return Optional.empty();
        }
    }
}
