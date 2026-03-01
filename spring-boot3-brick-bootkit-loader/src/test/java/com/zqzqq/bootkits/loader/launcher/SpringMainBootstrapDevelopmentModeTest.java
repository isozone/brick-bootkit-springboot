package com.zqzqq.bootkits.loader.launcher;

import com.zqzqq.bootkits.loader.DevelopmentMode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SpringMainBootstrapDevelopmentModeTest {

    private static final String[] PROPERTY_KEYS = new String[]{
            "plugin.developmentMode",
            "spring-boot3-brick-bootkit.developmentMode",
            "developmentMode"
    };

    private String originalMode;
    private final String[] originalPropertyValues = new String[PROPERTY_KEYS.length];

    @BeforeEach
    void setUp() throws Exception {
        originalMode = getDevelopmentModeField();
        for (int i = 0; i < PROPERTY_KEYS.length; i++) {
            originalPropertyValues[i] = System.getProperty(PROPERTY_KEYS[i]);
            System.clearProperty(PROPERTY_KEYS[i]);
        }
        setDevelopmentModeField(null);
    }

    @AfterEach
    void tearDown() throws Exception {
        for (int i = 0; i < PROPERTY_KEYS.length; i++) {
            String value = originalPropertyValues[i];
            if (value == null) {
                System.clearProperty(PROPERTY_KEYS[i]);
            } else {
                System.setProperty(PROPERTY_KEYS[i], value);
            }
        }
        setDevelopmentModeField(originalMode);
    }

    @Test
    void resolveDevelopmentMode_ShouldUseOverrideValue() {
        System.setProperty("plugin.developmentMode", DevelopmentMode.ISOLATION);
        SpringBootstrap springBootstrap = new CoexistBootstrap();

        String mode = SpringMainBootstrap.resolveDevelopmentMode(springBootstrap);

        assertEquals(DevelopmentMode.COEXIST, mode);
    }

    @Test
    void resolveDevelopmentMode_ShouldUsePropertyWhenNotOverridden() {
        System.setProperty("plugin.developmentMode", DevelopmentMode.COEXIST);
        SpringBootstrap springBootstrap = new DefaultBootstrap();

        String mode = SpringMainBootstrap.resolveDevelopmentMode(springBootstrap);

        assertEquals(DevelopmentMode.COEXIST, mode);
    }

    @Test
    void resolveDevelopmentMode_ShouldFallbackToDefaultWhenNoProperty() {
        SpringBootstrap springBootstrap = new DefaultBootstrap();

        String mode = SpringMainBootstrap.resolveDevelopmentMode(springBootstrap);

        assertEquals(DevelopmentMode.ISOLATION, mode);
    }

    @Test
    void resolveDevelopmentMode_InvalidProperty_ShouldThrow() {
        System.setProperty("plugin.developmentMode", "invalid-mode");
        SpringBootstrap springBootstrap = new DefaultBootstrap();

        assertThrows(IllegalStateException.class, () -> SpringMainBootstrap.resolveDevelopmentMode(springBootstrap));
    }

    private static String getDevelopmentModeField() throws Exception {
        Field field = DevelopmentModeSetting.class.getDeclaredField("developmentMode");
        field.setAccessible(true);
        return (String) field.get(null);
    }

    private static void setDevelopmentModeField(String value) throws Exception {
        Field field = DevelopmentModeSetting.class.getDeclaredField("developmentMode");
        field.setAccessible(true);
        field.set(null, value);
    }

    static class DefaultBootstrap implements SpringBootstrap {
        @Override
        public void run(String[] args) {
        }
    }

    static class CoexistBootstrap implements SpringBootstrap {
        @Override
        public void run(String[] args) {
        }

        @Override
        public String developmentMode() {
            return DevelopmentMode.COEXIST;
        }
    }
}
