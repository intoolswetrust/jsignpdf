/*
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations
 * under the License.
 *
 * The Original Code is 'JSignPdf, a free application for PDF signing'.
 *
 * The Initial Developer of the Original Code is Josef Cacek.
 * Portions created by Josef Cacek are Copyright (C) Josef Cacek. All Rights Reserved.
 *
 * Contributor(s): Josef Cacek.
 *
 * Alternatively, the contents of this file may be used under the terms
 * of the GNU Lesser General Public License, version 2.1 (the  "LGPL License"), in which case the
 * provisions of LGPL License are applicable instead of those
 * above. If you wish to allow use of your version of this file only
 * under the terms of the LGPL License and not to allow others to use
 * your version of this file under the MPL, indicate your decision by
 * deleting the provisions above and replace them with the notice and
 * other provisions required by the LGPL License. If you do not delete
 * the provisions above, a recipient may use your version of this file
 * under either the MPL or the LGPL License.
 */
package net.sf.jsignpdf.utils;

import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.List;

import eu.europa.esig.dss.enumerations.DigestAlgorithm;
import eu.europa.esig.dss.enumerations.EncryptionAlgorithm;
import eu.europa.esig.dss.enumerations.MaskGenerationFunction;
import eu.europa.esig.dss.enumerations.SignatureAlgorithm;
import eu.europa.esig.dss.model.DSSException;
import eu.europa.esig.dss.model.SignatureValue;
import eu.europa.esig.dss.model.ToBeSigned;
import eu.europa.esig.dss.model.x509.CertificateToken;
import eu.europa.esig.dss.token.AbstractSignatureTokenConnection;
import eu.europa.esig.dss.token.DSSPrivateKeyEntry;

/**
 * A thin adapter that wraps an existing {@link PrivateKey} and {@link Certificate} chain
 * into a DSS {@link eu.europa.esig.dss.token.SignatureTokenConnection}.
 * This allows integration of JSignPdf's existing key management (KeyStoreUtils, PKCS11, etc.)
 * with DSS's signing framework.
 *
 * @author Josef Cacek
 */
public class PrivateKeySignatureToken extends AbstractSignatureTokenConnection {

    private final PrivateKey privateKey;
    private final CertificateToken[] certificateChain;
    private final DSSPrivateKeyEntry keyEntry;

    public PrivateKeySignatureToken(PrivateKey key, Certificate[] chain) {
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

    public DSSPrivateKeyEntry getKeyEntry() {
        return keyEntry;
    }

    @Override
    public SignatureValue sign(ToBeSigned toBeSigned, DigestAlgorithm digestAlgorithm,
            MaskGenerationFunction mgf, DSSPrivateKeyEntry keyEntry) throws DSSException {
        try {
            EncryptionAlgorithm encAlg = EncryptionAlgorithm.forKey(privateKey);
            SignatureAlgorithm sigAlg;
            if (mgf != null) {
                sigAlg = SignatureAlgorithm.getAlgorithm(encAlg, digestAlgorithm, mgf);
            } else {
                sigAlg = SignatureAlgorithm.getAlgorithm(encAlg, digestAlgorithm);
            }

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
        // no resources to close
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
