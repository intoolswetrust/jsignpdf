package net.sf.jsignpdf.engine.dss;

import java.io.File;
import java.math.BigInteger;
import java.nio.file.Files;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.util.Date;

import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DERNull;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.pkcs.RSAPublicKey;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509ExtensionUtils;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

/**
 * Builds a certificate whose SubjectPublicKeyInfo is marked {@code id-RSASSA-PSS} over a public key
 * exported from a PKCS#11 token, so the token presents the "PSS-only certificate under an RSA-reporting
 * key" shape that {@link DssPssSigningTest} covers in software. Issued by a throwaway CA generated here,
 * so no private-key operation on the token and no PIN are involved.
 *
 * <p>
 * Manual tool for the hardware checklist in {@code design-doc/3.2-algorithm-agility.md}; not a test.
 * </p>
 */
public final class PssTokenCertGen {

    private static final String ID_RSASSA_PSS = "1.2.840.113549.1.1.10";

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("usage: PssTokenCertGen <pubkey.der> <output-dir> [subject-CN]");
            System.exit(2);
        }
        Security.addProvider(new BouncyCastleProvider());

        SubjectPublicKeyInfo tokenKey = readPublicKey(Files.readAllBytes(new File(args[0]).toPath()));
        File outDir = new File(args[1]);
        outDir.mkdirs();
        String cn = args.length > 2 ? args[2] : "JSignPdf PSS Test Signer";

        Date notBefore = new Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000L);
        Date notAfter = new Date(System.currentTimeMillis() + 3650L * 24 * 60 * 60 * 1000L);

        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        KeyPair caKey = kpg.generateKeyPair();
        X500Name caName = new X500Name("CN=JSignPdf PSS Test CA, O=JSignPdf Test");

        JcaX509ExtensionUtils extUtils = new JcaX509ExtensionUtils();
        JcaX509v3CertificateBuilder caBuilder = new JcaX509v3CertificateBuilder(
                caName, BigInteger.ONE, notBefore, notAfter, caName, caKey.getPublic());
        caBuilder.addExtension(Extension.basicConstraints, true, new BasicConstraints(true));
        caBuilder.addExtension(Extension.keyUsage, true, new KeyUsage(KeyUsage.keyCertSign | KeyUsage.cRLSign));
        caBuilder.addExtension(Extension.subjectKeyIdentifier, false,
                extUtils.createSubjectKeyIdentifier(caKey.getPublic()));
        ContentSigner caSigner = new JcaContentSignerBuilder("SHA256withRSA")
                .setProvider(BouncyCastleProvider.PROVIDER_NAME).build(caKey.getPrivate());
        X509Certificate caCert = toCert(caBuilder.build(caSigner));

        SubjectPublicKeyInfo pssSpki = new SubjectPublicKeyInfo(
                new AlgorithmIdentifier(new ASN1ObjectIdentifier(ID_RSASSA_PSS)), tokenKey.parsePublicKey());

        X500Name leafName = new X500Name("CN=" + cn + ", O=JSignPdf Test");
        X509v3CertificateBuilder leafBuilder = new X509v3CertificateBuilder(
                caName, BigInteger.valueOf(2), notBefore, notAfter, leafName, pssSpki);
        leafBuilder.addExtension(Extension.basicConstraints, true, new BasicConstraints(false));
        leafBuilder.addExtension(Extension.keyUsage, true,
                new KeyUsage(KeyUsage.digitalSignature | KeyUsage.nonRepudiation));
        leafBuilder.addExtension(Extension.authorityKeyIdentifier, false,
                extUtils.createAuthorityKeyIdentifier(caCert));
        X509Certificate leafCert = toCert(leafBuilder.build(caSigner));

        File leafFile = new File(outDir, "pss-leaf.der");
        File caFile = new File(outDir, "pss-ca.der");
        Files.write(leafFile.toPath(), leafCert.getEncoded());
        Files.write(caFile.toPath(), caCert.getEncoded());

        System.out.println("leaf certificate : " + leafFile);
        System.out.println("  subject        : " + leafCert.getSubjectX500Principal());
        System.out.println("  issuer         : " + leafCert.getIssuerX500Principal());
        System.out.println("  SPKI algorithm : " + leafCert.getPublicKey().getAlgorithm());
        System.out.println("  modulus match  : " + modulusOf(tokenKey).equals(modulusOf(pssSpki)));
        System.out.println("CA certificate   : " + caFile);
    }

    private static BigInteger modulusOf(SubjectPublicKeyInfo spki) throws Exception {
        return RSAPublicKey.getInstance(spki.parsePublicKey()).getModulus();
    }

    private static X509Certificate toCert(X509CertificateHolder holder) throws Exception {
        return new JcaX509CertificateConverter().setProvider(BouncyCastleProvider.PROVIDER_NAME)
                .getCertificate(holder);
    }

    /** Accepts either a SubjectPublicKeyInfo or the bare PKCS#1 RSAPublicKey that pkcs11-tool may emit. */
    private static SubjectPublicKeyInfo readPublicKey(byte[] der) throws Exception {
        try {
            SubjectPublicKeyInfo spki = SubjectPublicKeyInfo.getInstance(ASN1Primitive.fromByteArray(der));
            RSAPublicKey.getInstance(spki.parsePublicKey());
            return spki;
        } catch (Exception notSpki) {
            RSAPublicKey rsa = RSAPublicKey.getInstance(ASN1Sequence.fromByteArray(der));
            return new SubjectPublicKeyInfo(
                    new AlgorithmIdentifier(PKCSObjectIdentifiers.rsaEncryption, DERNull.INSTANCE), rsa);
        }
    }

    private PssTokenCertGen() {
    }
}
