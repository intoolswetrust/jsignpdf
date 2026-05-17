package com.intoolswetrust.jsignpdf;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class BootstrapTest {

    @Test
    public void parsesModernJavaVersionStrings() {
        assertEquals(21, Bootstrap.parseJavaMajor("21"));
        assertEquals(21, Bootstrap.parseJavaMajor("21.0.7"));
        assertEquals(17, Bootstrap.parseJavaMajor("17.0.1"));
    }

    @Test
    public void parsesLegacyDottedVersions() {
        assertEquals(8, Bootstrap.parseJavaMajor("1.8"));
        assertEquals(8, Bootstrap.parseJavaMajor("1.8.0_412"));
    }

    @Test
    public void parsesGarbageAsZero() {
        assertEquals(0, Bootstrap.parseJavaMajor(""));
        assertEquals(0, Bootstrap.parseJavaMajor(null));
        assertEquals(0, Bootstrap.parseJavaMajor("not-a-version"));
    }

    @Test
    public void detectsWindowsX64() {
        assertEquals("win", Bootstrap.detectFxClassifier("Windows 11", "amd64"));
    }

    @Test
    public void detectsLinuxX64() {
        assertEquals("linux", Bootstrap.detectFxClassifier("Linux", "amd64"));
    }

    @Test
    public void detectsLinuxAarch64() {
        assertEquals("linux-aarch64", Bootstrap.detectFxClassifier("Linux", "aarch64"));
    }

    @Test
    public void detectsMacX64() {
        assertEquals("mac", Bootstrap.detectFxClassifier("Mac OS X", "x86_64"));
    }

    @Test
    public void detectsMacAarch64() {
        assertEquals("mac-aarch64", Bootstrap.detectFxClassifier("Mac OS X", "aarch64"));
        assertEquals("mac-aarch64", Bootstrap.detectFxClassifier("Mac OS X", "arm64"));
    }

    @Test
    public void unsupportedPlatformReturnsNull() {
        assertNull(Bootstrap.detectFxClassifier("AIX", "ppc64"));
        assertNull(Bootstrap.detectFxClassifier("Windows 11", "aarch64"));
    }
}
