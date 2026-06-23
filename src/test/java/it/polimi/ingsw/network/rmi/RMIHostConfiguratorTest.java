package it.polimi.ingsw.network.rmi;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Unit tests for RMIHostConfigurator.
 *
 * Verifies: server bootstraps set the RMI advertised host before exporting objects.
 */
class RMIHostConfiguratorTest {

    private final String originalHostname = System.getProperty(RMIHostConfigurator.RMI_HOSTNAME_PROPERTY);

    @AfterEach
    void restoreProperty() {
        if (originalHostname == null) {
            System.clearProperty(RMIHostConfigurator.RMI_HOSTNAME_PROPERTY);
        } else {
            System.setProperty(RMIHostConfigurator.RMI_HOSTNAME_PROPERTY, originalHostname);
        }
    }

    @Test
    void explicitHostSetsRmiHostnameProperty() {
        System.clearProperty(RMIHostConfigurator.RMI_HOSTNAME_PROPERTY);

        String advertised = RMIHostConfigurator.configure("172.20.10.2");

        assertEquals("172.20.10.2", advertised);
        assertEquals("172.20.10.2", System.getProperty(RMIHostConfigurator.RMI_HOSTNAME_PROPERTY));
    }

    @Test
    void blankHostLeavesExistingConfigurationUntouched() {
        System.clearProperty(RMIHostConfigurator.RMI_HOSTNAME_PROPERTY);

        String advertised = RMIHostConfigurator.configure(" ");

        assertNull(advertised);
        assertNull(System.getProperty(RMIHostConfigurator.RMI_HOSTNAME_PROPERTY));
    }
}
