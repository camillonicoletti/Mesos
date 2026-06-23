package it.polimi.ingsw.network.rmi;

/**
 * Configures the hostname embedded in exported RMI stubs.
 */
public final class RMIHostConfigurator {

    public static final String RMI_HOSTNAME_PROPERTY = "java.rmi.server.hostname";

    private RMIHostConfigurator() {
    }

    public static String configure(String advertisedHost) {
        if (advertisedHost == null || advertisedHost.isBlank()) {
            return System.getProperty(RMI_HOSTNAME_PROPERTY);
        }

        String normalizedHost = advertisedHost.trim();
        System.setProperty(RMI_HOSTNAME_PROPERTY, normalizedHost);
        return normalizedHost;
    }
}
