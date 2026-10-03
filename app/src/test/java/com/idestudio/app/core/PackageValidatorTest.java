package com.idestudio.app.core;

import com.idestudio.app.core.utils.PackageValidator;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PackageValidatorTest {

    @Test
    public void testGenerateDefaultPackage() {
        assertEquals("com.mycoolapp", PackageValidator.generateDefaultPackage("My Cool App"));
        assertEquals("com.androidstudio", PackageValidator.generateDefaultPackage("Android-Studio!"));
        assertEquals("com.app123", PackageValidator.generateDefaultPackage("123"));
    }

    @Test
    public void testValidPackageNames() {
        assertTrue(PackageValidator.isValidPackageName("com.example.app"));
        assertTrue(PackageValidator.isValidPackageName("org.myproject.demo"));
        assertTrue(PackageValidator.isValidPackageName("in.studio.app_v2"));
    }

    @Test
    public void testInvalidPackageNames() {
        // Single segment
        assertFalse(PackageValidator.isValidPackageName("myapp"));
        // Contains Java keyword
        assertFalse(PackageValidator.isValidPackageName("com.class.myapp"));
        assertFalse(PackageValidator.isValidPackageName("com.int.project"));
        // Starts with digit segment
        assertFalse(PackageValidator.isValidPackageName("com.123app.test"));
        // Empty or null
        assertFalse(PackageValidator.isValidPackageName(""));
        assertFalse(PackageValidator.isValidPackageName(null));
    }
}
