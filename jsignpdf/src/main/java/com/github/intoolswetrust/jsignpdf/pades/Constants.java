package com.github.intoolswetrust.jsignpdf.pades;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.logging.LogManager;
import java.util.logging.Logger;

public class Constants {

    static {
        try (InputStream is = Constants.class.getClassLoader().
                getResourceAsStream("logging.properties")) {
            LogManager.getLogManager().readConfiguration(is);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static final Logger LOGGER = Logger.getLogger(Constants.class.getPackage().getName());

    /**
     * Version of JSignPdf
     */
    public static final String VERSION;

    public static final Set<String> SUPPORTED_CRITICAL_EXTENSION_OIDS;

    static {
        final Set<String> oidSet = new HashSet<String>();
        oidSet.add("2.5.29.15"); // KeyUsage
        oidSet.add("2.5.29.17"); // Subject Alternative Name
        oidSet.add("2.5.29.19"); // Basic Constraints
        oidSet.add("2.5.29.29"); // Certificate Issuer
        oidSet.add("2.5.29.37"); // Extended Key Usage
        SUPPORTED_CRITICAL_EXTENSION_OIDS = Collections.unmodifiableSet(oidSet);

        String version = "[UNKNOWN]";
        try (InputStream is = Constants.class
                .getResourceAsStream("/META-INF/maven/com.github.kwart.jsign/jsignpdf/pom.properties")) {
            if (is != null) {
                Properties props = new Properties();
                props.load(is);
                if (props.containsKey("version")) {
                    version = props.getProperty("version");
                }
            }
        } catch (IOException e) {
            // ignore
        }
        VERSION = version;
    }

}
