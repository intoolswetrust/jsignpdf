package net.sf.jsignpdf.engine.dss;

import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.List;

import eu.europa.esig.dss.enumerations.DigestAlgorithm;
import eu.europa.esig.dss.enumerations.EncryptionAlgorithm;
import eu.europa.esig.dss.enumerations.SignatureAlgorithm;
import eu.europa.esig.dss.model.DSSException;
import eu.europa.esig.dss.model.SignatureValue;
import eu.europa.esig.dss.model.ToBeSigned;
import eu.europa.esig.dss.model.x509.CertificateToken;
import eu.europa.esig.dss.token.AbstractSignatureTokenConnection;
import eu.europa.esig.dss.token.DSSPrivateKeyEntry;

/**
 * In-memory {@link eu.europa.esig.dss.token.SignatureTokenConnection} that signs the DSS
 * {@link ToBeSigned} with a plain JCA {@link Signature}, deriving the {@link SignatureAlgorithm} from
 * the key's {@link EncryptionAlgorithm} and the chosen {@link DigestAlgorithm}.
 *
 * <p>
 * This is the seam where PKCS#11 keys work unchanged (the {@link PrivateKey} is a provider key) and
 * where a future external-signing token (e.g. CloudFoxy) would plug in. Ported from
 * {@code jsignpdf-pades}.
 * </p>
 *
 * @author Josef Cacek
 */
final class PrivateKeySignatureToken extends AbstractSignatureTokenConnection {

    private final PrivateKey privateKey;
    private final CertificateToken[] certificateChain;
    private final DSSPrivateKeyEntry keyEntry;

    PrivateKeySignatureToken(PrivateKey key, Certificate[] chain) {
        this.privateKey = key;
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

    @Override
    public SignatureValue sign(ToBeSigned toBeSigned, DigestAlgorithm digestAlgorithm, DSSPrivateKeyEntry keyEntry)
            throws DSSException {
        try {
            EncryptionAlgorithm encAlg = EncryptionAlgorithm.forKey(privateKey);
            SignatureAlgorithm sigAlg = SignatureAlgorithm.getAlgorithm(encAlg, digestAlgorithm);

            Signature signature = Signature.getInstance(sigAlg.getJCEId());
            signature.initSign(privateKey);
            signature.update(toBeSigned.getBytes());
            byte[] sigValue = signature.sign();

            SignatureValue signatureValue = new SignatureValue();
            signatureValue.setAlgorithm(sigAlg);
            signatureValue.setValue(sigValue);
            return signatureValue;
        } catch (GeneralSecurityException e) {
            throw new DSSException("Unable to sign", e);
        }
    }

    @Override
    public void close() {
    }

    private class PrivateKeyEntryImpl implements DSSPrivateKeyEntry {
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
    }
}
