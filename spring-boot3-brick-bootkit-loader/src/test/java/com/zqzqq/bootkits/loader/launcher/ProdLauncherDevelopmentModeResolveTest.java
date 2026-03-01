package com.zqzqq.bootkits.loader.launcher;

import com.zqzqq.bootkits.loader.DevelopmentMode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProdLauncherDevelopmentModeResolveTest {

    private static final String[] PROPERTY_KEYS = new String[]{
            "plugin.developmentMode",
            "spring-boot3-brick-bootkit.developmentMode",
            "developmentMode"
    };

    private final String[] originalPropertyValues = new String[PROPERTY_KEYS.length];

    @BeforeEach
    void setUp() {
        for (int i = 0; i < PROPERTY_KEYS.length; i++) {
            originalPropertyValues[i] = System.getProperty(PROPERTY_KEYS[i]);
            System.clearProperty(PROPERTY_KEYS[i]);
        }
    }

    @AfterEach
    void tearDown() {
        for (int i = 0; i < PROPERTY_KEYS.length; i++) {
            String value = originalPropertyValues[i];
            if (value == null) {
                System.clearProperty(PROPERTY_KEYS[i]);
            } else {
                System.setProperty(PROPERTY_KEYS[i], value);
            }
        }
    }

    @Test
    void resolveDevelopmentMode_ShouldUseManifestValueFirst() {
        System.setProperty("plugin.developmentMode", DevelopmentMode.ISOLATION);

        String mode = ProdLauncher.resolveDevelopmentMode(DevelopmentMode.COEXIST);

        assertEquals(DevelopmentMode.COEXIST, mode);
    }

    @Test
    void resolveDevelopmentMode_ShouldUsePropertiesWhenManifestMissing() {
        System.setProperty("plugin.developmentMode", DevelopmentMode.COEXIST);

        String mode = ProdLauncher.resolveDevelopmentMode(null);

        assertEquals(DevelopmentMode.COEXIST, mode);
    }

    @Test
    void resolveDevelopmentMode_ShouldFallbackLegacyPropertyOrder() {
        System.setProperty("spring-boot3-brick-bootkit.developmentMode", DevelopmentMode.COEXIST);

        String mode = ProdLauncher.resolveDevelopmentMode(null);

        assertEquals(DevelopmentMode.COEXIST, mode);
    }

    @Test
    void resolveDevelopmentMode_InvalidProperty_ShouldThrow() {
        System.setProperty("plugin.developmentMode", "invalid-mode");

        assertThrows(IllegalStateException.class, () -> ProdLauncher.resolveDevelopmentMode(null));
    }

    @Test
    void resolveDevelopmentMode_NoManifestAndNoProperties_ShouldThrow() {
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> ProdLauncher.resolveDevelopmentMode(null));

        assertTrue(exception.getMessage().contains("Missing developmentMode configuration"));
    }
}
