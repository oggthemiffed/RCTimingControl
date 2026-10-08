package dev.monkeypatch.rctiming.config;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * The checks shared by settings that name a service off the venue network and the club's key for it (the live
 * feed relay, RaceHub's results address). The key goes with every connection, so the address must be
 * encrypted; the unencrypted form is allowed only to this machine, for testing. Blank settings count as unset.
 */
public final class SecureEndpoint {

    private SecureEndpoint() {
    }

    /**
     * The address in {@code setting}, or null when it is unset.
     *
     * @param secureScheme the scheme it must use, such as https
     * @param plainScheme  the unencrypted scheme allowed only to this machine, such as http
     * @throws IllegalArgumentException when it would send the key in the clear to another machine
     */
    public static URI url(URI url, String setting, String secureScheme, String plainScheme) {
        if (url == null || url.toString().isBlank()) {
            return null;
        }
        if (!secureScheme.equalsIgnoreCase(url.getScheme())
                && !(plainScheme.equalsIgnoreCase(url.getScheme()) && LoopbackHosts.isLoopback(url.getHost()))) {
            throw new IllegalArgumentException(setting + " must be a secure " + secureScheme + " address, so the "
                    + "club's key isn't sent in the clear (plain " + plainScheme + " works only to this machine), "
                    + "not " + url);
        }
        return url;
    }

    /** The key, or null when it is unset. */
    public static String token(String token) {
        return token == null || token.isBlank() ? null : token;
    }

    /** The settings still needed before the service can be used, empty when both are set. */
    public static List<String> missingSettings(URI url, String urlSetting, String token, String tokenSetting) {
        List<String> missing = new ArrayList<>();
        if (url == null) {
            missing.add(urlSetting);
        }
        if (token == null) {
            missing.add(tokenSetting);
        }
        return missing;
    }
}
