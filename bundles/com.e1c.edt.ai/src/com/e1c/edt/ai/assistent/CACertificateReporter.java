/*
 * Copyright (c) 2026, ООО 1С-Софт
 */
package com.e1c.edt.ai.assistent;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Enumeration;

import com.google.inject.Inject;

/**
 * @author Bogdan Sushkov
 *
 */
@SuppressWarnings("nls")
public final class CACertificateReporter
    implements ICACertificateReporter
{

    @Inject
    public CACertificateReporter()
    {
    }

    /**
     * Builds a plain text .log report with CA certificates from current JVM truststore.
     * Never throws to callers (wraps exceptions into report).
     */
    @Override
    public String buildPlainLog()
    {
        StringBuilder sb = new StringBuilder(64_000);

        sb.append("=== CA CERTIFICATES REPORT ===\n");
        sb.append("java.version=").append(System.getProperty("java.version")).append("\n");
        sb.append("java.vendor=").append(System.getProperty("java.vendor")).append("\n");
        sb.append("java.home=").append(System.getProperty("java.home")).append("\n");

        String tsProp = System.getProperty("javax.net.ssl.trustStore", "");
        String tsTypeProp = System.getProperty("javax.net.ssl.trustStoreType", "");
        String tsPassProp = System.getProperty("javax.net.ssl.trustStorePassword", "");

        sb.append("javax.net.ssl.trustStore=").append(tsProp).append("\n");
        sb.append("javax.net.ssl.trustStoreType=").append(tsTypeProp).append("\n\n");

        TrustStoreLocation loc = resolveTrustStore(tsProp, tsTypeProp, tsPassProp);
        sb.append("trustStore.path=").append(loc.path).append("\n");
        sb.append("trustStore.type=").append(loc.type).append("\n\n");

        try
        {
            KeyStore ks = KeyStore.getInstance(loc.type);
            try (InputStream in = Files.newInputStream(Path.of(loc.path)))
            {
                ks.load(in, loc.password);
            }

            int total = 0;
            Enumeration<String> aliases = ks.aliases();
            while (aliases.hasMoreElements())
            {
                String alias = aliases.nextElement();
                Certificate c = ks.getCertificate(alias);
                if (!(c instanceof X509Certificate))
                    continue;
                total++;
            }

            sb.append("entries=").append(total).append("\n");
            sb.append("----------------------------------------\n");

            aliases = ks.aliases();
            while (aliases.hasMoreElements())
            {
                String alias = aliases.nextElement();
                Certificate c = ks.getCertificate(alias);
                if (!(c instanceof X509Certificate))
                {
                    continue;
                }

                X509Certificate x = (X509Certificate)c;

                sb.append("alias=").append(alias).append("\n");
                sb.append("subject=").append(x.getSubjectX500Principal().getName()).append("\n");
                sb.append("issuer=").append(x.getIssuerX500Principal().getName()).append("\n");
                sb.append("validFrom=").append(x.getNotBefore()).append("\n");
                sb.append("validTo=").append(x.getNotAfter()).append("\n");
                sb.append("sha256=").append(sha256Fingerprint(x.getEncoded())).append("\n");
                sb.append("----------------------------------------\n");
            }

            sb.append("=== END CA CERTIFICATES REPORT ===\n");
            return sb.toString();

        }
        catch (Throwable t)
        {
            sb.append("ERROR: Failed to read truststore.\n");
            sb.append("reason=")
                .append(t.getClass().getName())
                .append(": ")
                .append(String.valueOf(t.getMessage()))
                .append("\n");
            sb.append("HINT: If you use a custom trustStore, ensure the path/password are correct.\n");
            sb.append("=== END CA CERTIFICATES REPORT ===\n");
            return sb.toString();
        }
    }

    private TrustStoreLocation resolveTrustStore(String tsProp, String typeProp, String passProp)
    {
        String type = (typeProp == null || typeProp.isBlank()) ? "JKS" : typeProp.trim();

        // password: if explicitly specified - use it. else try default 'changeit'
        char[] pass = (passProp != null && !passProp.isBlank()) ? passProp.toCharArray() : "changeit".toCharArray();

        // If trustStore property is set - use it
        if (tsProp != null && !tsProp.isBlank())
        {
            return new TrustStoreLocation(tsProp, type, pass);
        }

        // default truststore: <java.home>/lib/security/cacerts or <java.home>/jre/lib/security/cacerts
        String javaHome = System.getProperty("java.home");
        Path p1 = Path.of(javaHome, "lib", "security", "cacerts");
        Path p2 = Path.of(javaHome, "jre", "lib", "security", "cacerts");

        String path = Files.exists(p1) ? p1.toString() : p2.toString();
        return new TrustStoreLocation(path, type, pass);
    }

    private String sha256Fingerprint(byte[] der) throws Exception
    {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] dig = md.digest(der);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < dig.length; i++)
        {
            if (i > 0)
            {
                sb.append(':');
            }
            sb.append(String.format("%02X", dig[i]));
        }
        return sb.toString();
    }

    private final class TrustStoreLocation
    {
        final String path;
        final String type;
        final char[] password;

        TrustStoreLocation(String path, String type, char[] password)
        {
            this.path = path;
            this.type = type;
            this.password = password;
        }
    }
}
