package net.sf.jsignpdf.pkcs11;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.security.Key;
import java.security.KeyStoreSpi;
import java.security.Provider;
import java.security.cert.Certificate;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Fake PKCS#11 providers for tests: each profile gets its own provider offering the keystore type of the backend,
 * with a single alias {@code key-<profile id>}. No native code involved.
 */
final class TestProviders implements Pkcs11Profiles.ProviderFactory {

    final AtomicInteger created = new AtomicInteger();

    @Override
    public Provider create(Pkcs11Backend backend, Path configFile) {
        created.incrementAndGet();
        String name = Pkcs11ConfigText.value(readQuietly(configFile), Pkcs11ConfigText.KEY_NAME);
        return new FakeProvider(backend.providerNamePrefix() + name, backend.keyStoreType(), "key-" + name);
    }

    private static String readQuietly(Path p) {
        try {
            return java.nio.file.Files.readString(p);
        } catch (java.io.IOException e) {
            throw new IllegalStateException(e);
        }
    }

    static final class FakeProvider extends Provider {
        private static final long serialVersionUID = 1L;

        FakeProvider(String name, String type, String alias) {
            super(name, "1.0", "test provider");
            putService(new Service(this, "KeyStore", type, FakeKeyStore.class.getName(), null, null) {
                @Override
                public Object newInstance(Object param) {
                    return new FakeKeyStore(alias);
                }
            });
        }
    }

    static final class FakeKeyStore extends KeyStoreSpi {
        private final String alias;
        char[] lastPassword;

        FakeKeyStore(String alias) {
            this.alias = alias;
        }

        @Override
        public Key engineGetKey(String a, char[] password) {
            return null;
        }

        @Override
        public Certificate[] engineGetCertificateChain(String a) {
            return null;
        }

        @Override
        public Certificate engineGetCertificate(String a) {
            return null;
        }

        @Override
        public Date engineGetCreationDate(String a) {
            return null;
        }

        @Override
        public void engineSetKeyEntry(String a, Key key, char[] password, Certificate[] chain) {
        }

        @Override
        public void engineSetKeyEntry(String a, byte[] key, Certificate[] chain) {
        }

        @Override
        public void engineSetCertificateEntry(String a, Certificate cert) {
        }

        @Override
        public void engineDeleteEntry(String a) {
        }

        @Override
        public Enumeration<String> engineAliases() {
            return Collections.enumeration(List.of(alias));
        }

        @Override
        public boolean engineContainsAlias(String a) {
            return alias.equals(a);
        }

        @Override
        public int engineSize() {
            return 1;
        }

        @Override
        public boolean engineIsKeyEntry(String a) {
            return alias.equals(a);
        }

        @Override
        public boolean engineIsCertificateEntry(String a) {
            return false;
        }

        @Override
        public String engineGetCertificateAlias(Certificate cert) {
            return null;
        }

        @Override
        public void engineStore(OutputStream stream, char[] password) {
        }

        @Override
        public void engineLoad(InputStream stream, char[] password) {
            if (password != null && "wrong".equals(new String(password))) {
                throw new IllegalStateException("CKR_PIN_INCORRECT");
            }
            lastPassword = password;
        }
    }
}
