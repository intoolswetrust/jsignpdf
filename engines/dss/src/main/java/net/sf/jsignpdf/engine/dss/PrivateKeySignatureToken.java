package net.sf.jsignpdf.engine.dss;

import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.Signature;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.List;

import eu.europa.esig.dss.enumerations.EncryptionAlgorithm;
import eu.europa.esig.dss.enumerations.SignatureAlgorithm;
import eu.europa.esig.dss.model.DSSException;
import eu.europa.esig.dss.model.ToBeSigned;
import eu.europa.esig.dss.model.x509.CertificateToken;
import eu.europa.esig.dss.token.AbstractSignatureTokenConnection;
import eu.europa.esig.dss.token.DSSPrivateKeyAccessEntry;
import eu.europa.esig.dss.token.DSSPrivateKeyEntry;

/**
 * In-memory {@link eu.europa.esig.dss.token.SignatureTokenConnection} that signs the DSS
 * {@link ToBeSigned} with a plain JCA {@link Signature}.
 *
 * <p>
 * Signing is left to {@link AbstractSignatureTokenConnection}. Callers must pass the
 * {@link SignatureAlgorithm} from the signature parameters, not one derived from the key: DSS rejects a
 * {@code SignatureValue} whose algorithm differs from the parameters, and the two disagree whenever a
 * PKCS#11 key reports {@code "RSA"} under a certificate carrying {@code id-RSASSA-PSS}.
 * </p>
 *
 * <p>
 * This is the seam where PKCS#11 keys work unchanged (the {@link PrivateKey} is a provider key) and where a
 * future external-signing token (e.g. CloudFoxy) would plug in. Ported from {@code jsignpdf-pades}.
 * </p>
 *
 * @author Josef Cacek
 */
final class PrivateKeySignatureToken extends AbstractSignatureTokenConnection {

    /** Portable PSS name; DSS's own {@code SHAxxxwithRSAandMGF1} is registered by BouncyCastle only. */
    private static final String PSS_JCE_NAME = "RSASSA-PSS";

    private final PrivateKey privateKey;
    private final Provider keyProvider;
    private final CertificateToken[] certificateChain;
    private final DSSPrivateKeyEntry keyEntry;

    /**
     * @param key      the signing key
     * @param chain    the signing certificate chain, signer first
     * @param provider the provider the key came from (a PKCS#11 provider for a token key), or {@code null}
     *                 when unknown; used to keep an opaque key on its own provider
     */
    PrivateKeySignatureToken(PrivateKey key, Certificate[] chain, Provider provider) {
        this.privateKey = key;
        this.keyProvider = provider;
        this.certificateChain = new CertificateToken[chain.length];
        for (int i = 0; i < chain.length; i++) {
            this.certificateChain[i] = new CertificateToken((X509Certificate) chain[i]);
        }
        this.keyEntry = new PrivateKeyEntryImpl();
    }

    @Override
    public List<DSSPrivateKeyEntry> getKeys() throws DSSException {
        return Collections.singletonList(keyEntry);
    }

    DSSPrivateKeyEntry getKeyEntry() {
        return keyEntry;
    }

    /**
     * Requests PSS under its portable name (RFC 4055 makes it equivalent to the BouncyCastle name once the
     * base class supplies the {@code PSSParameterSpec}), and pins the key's own provider when it offers the
     * algorithm. Delayed provider selection is unavailable here: the base class calls
     * {@code setParameter} before {@code initSign}, which resolves the provider immediately and would pin
     * SunRsaSign, rejecting a non-extractable PKCS#11 key.
     */
    @Override
    protected Signature getSignatureInstance(String javaSignatureAlgorithm) throws NoSuchAlgorithmException {
        if (isPssName(javaSignatureAlgorithm)) {
            try {
                return newSignature(PSS_JCE_NAME);
            } catch (NoSuchAlgorithmException e) {
                // No portable PSS implementation available; fall back to the BouncyCastle-style name.
            }
        }
        return newSignature(javaSignatureAlgorithm);
    }

    private Signature newSignature(String algorithm) throws NoSuchAlgorithmException {
        if (keyProvider != null && keyProvider.getService("Signature", algorithm) != null) {
            return Signature.getInstance(algorithm, keyProvider);
        }
        return Signature.getInstance(algorithm);
    }

    private static boolean isPssName(String javaSignatureAlgorithm) {
        return javaSignatureAlgorithm != null && javaSignatureAlgorithm.endsWith("withRSAandMGF1");
    }

    @Override
    public void close() {
    }

    private class PrivateKeyEntryImpl implements DSSPrivateKeyAccessEntry {
        @Override
        public CertificateToken getCertificate() {
            return certificateChain[0];
        }

        @Override
        public CertificateToken[] getCertificateChain() {
            return certificateChain;
        }

        @Override
        public EncryptionAlgorithm getEncryptionAlgorithm() {
            return EncryptionAlgorithm.forKey(privateKey);
        }

        @Override
        public PrivateKey getPrivateKey() {
            return privateKey;
        }
    }
}
