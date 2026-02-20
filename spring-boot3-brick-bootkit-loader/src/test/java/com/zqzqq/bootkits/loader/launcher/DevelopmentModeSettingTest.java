package com.zqzqq.bootkits.loader.launcher;

import com.zqzqq.bootkits.loader.DevelopmentMode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DevelopmentModeSetting unit tests.
 */
class DevelopmentModeSettingTest {

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
    void setStandaloneDevelopmentMode_ShouldSetMode() {
        DevelopmentModeSetting.setStandaloneDevelopmentMode(DevelopmentMode.ISOLATION);
        assertEquals(DevelopmentMode.ISOLATION, DevelopmentModeSetting.getDevelopmentMode());
        assertTrue(DevelopmentModeSetting.isolation());
        assertFalse(DevelopmentModeSetting.coexist());
    }

    @Test
    void setStandaloneDevelopmentMode_Invalid_ShouldThrow() {
        assertThrows(IllegalStateException.class,
                () -> DevelopmentModeSetting.setStandaloneDevelopmentMode("invalid-mode"));
    }

    @Test
    void tryInitFromProperties_ShouldReadPluginDevelopmentMode() {
        System.setProperty("plugin.developmentMode", DevelopmentMode.COEXIST);

        assertTrue(DevelopmentModeSetting.coexist());
        assertEquals(DevelopmentMode.COEXIST, DevelopmentModeSetting.getDevelopmentMode());
    }

    @Test
    void tryInitFromProperties_ShouldNotOverrideExistingMode() throws Exception {
        setDevelopmentModeField(DevelopmentMode.ISOLATION);
        System.setProperty("plugin.developmentMode", DevelopmentMode.COEXIST);

        assertTrue(DevelopmentModeSetting.isolation());
        assertEquals(DevelopmentMode.ISOLATION, DevelopmentModeSetting.getDevelopmentMode());
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
}
