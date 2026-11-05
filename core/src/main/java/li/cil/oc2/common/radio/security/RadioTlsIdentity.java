package li.cil.oc2.common.radio.security;

import java.io.*;
import java.math.BigInteger;
import java.security.*;
import java.security.cert.*;
import java.security.spec.*;
import java.util.*;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

/**
 * Server-private, independently generated device identity. Never send the private blob to guests.
 */
@SuppressWarnings("PMD.AvoidFieldNameMatchingMethodName") // Fields back their same-named accessors.
public final class RadioTlsIdentity {
    private final UUID id;
    private final long generation;
    final PrivateKey privateKey;
    final X509Certificate certificate;

    private RadioTlsIdentity(
            UUID id, long generation, PrivateKey privateKey, X509Certificate certificate) {
        this.id = Objects.requireNonNull(id);
        if (generation < 0) throw new IllegalArgumentException("Negative identity generation");
        this.generation = generation;
        this.privateKey = privateKey;
        this.certificate = certificate;
    }

    @SuppressWarnings("PMD.ReplaceJavaUtilDate") // The certificate builder takes java.util.Date.
    public static RadioTlsIdentity create(UUID id, long generation) {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
            SecureRandom random = new SecureRandom();
            generator.initialize(new ECGenParameterSpec("secp256r1"), random);
            KeyPair pair = generator.generateKeyPair();
            X500Name name = new X500Name("CN=" + id + ",OU=" + generation);
            long now = System.currentTimeMillis();
            var builder =
                    new JcaX509v3CertificateBuilder(
                            name,
                            new BigInteger(159, random).add(BigInteger.ONE),
                            new Date(now - 86400000L),
                            new Date(now + 315360000000L),
                            name,
                            pair.getPublic());
            X509Certificate certificate =
                    new JcaX509CertificateConverter()
                            .getCertificate(
                                    builder.build(
                                            new JcaContentSignerBuilder("SHA256withECDSA")
                                                    .build(pair.getPrivate())));
            return new RadioTlsIdentity(id, generation, pair.getPrivate(), certificate);
        } catch (Exception exception) {
            throw new IllegalStateException("Cannot generate radio TLS identity", exception);
        }
    }

    public UUID id() {
        return id;
    }

    public long generation() {
        return generation;
    }

    public byte[] certificate() {
        try {
            return certificate.getEncoded();
        } catch (CertificateEncodingException exception) {
            throw new IllegalStateException(exception);
        }
    }

    public byte[] encodedPrivateMaterial() {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(bytes);
            out.writeInt(1);
            out.writeUTF(id.toString());
            out.writeLong(generation);
            byte[] key = privateKey.getEncoded();
            out.writeInt(key.length);
            out.write(key);
            byte[] cert = certificate();
            out.writeInt(cert.length);
            out.write(cert);
            return bytes.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException(exception);
        }
    }

    // Every validation failure is reported uniformly as an invalid identity.
    @SuppressWarnings("PMD.ExceptionAsFlowControl")
    public static RadioTlsIdentity restore(UUID id, long generation, byte[] blob) {
        if (blob == null || blob.length > 16384)
            throw new IllegalArgumentException("Invalid identity blob");
        try {
            DataInputStream in = new DataInputStream(new ByteArrayInputStream(blob));
            if (in.readInt() != 1
                    || !in.readUTF().equals(id.toString())
                    || in.readLong() != generation)
                throw new IOException("Identity binding mismatch");
            byte[] key = readPart(in);
            byte[] cert = readPart(in);
            if (in.available() != 0) throw new IOException("Trailing identity data");
            PrivateKey privateKey =
                    KeyFactory.getInstance("EC").generatePrivate(new PKCS8EncodedKeySpec(key));
            X509Certificate certificate =
                    (X509Certificate)
                            CertificateFactory.getInstance("X.509")
                                    .generateCertificate(new ByteArrayInputStream(cert));
            certificate.verify(certificate.getPublicKey());
            Signature signature = Signature.getInstance("SHA256withECDSA");
            signature.initSign(privateKey);
            signature.update(cert);
            byte[] proof = signature.sign();
            signature.initVerify(certificate.getPublicKey());
            signature.update(cert);
            if (!signature.verify(proof)) throw new GeneralSecurityException("Key mismatch");
            return new RadioTlsIdentity(id, generation, privateKey, certificate);
        } catch (Exception exception) {
            throw new IllegalArgumentException("Invalid radio TLS identity", exception);
        }
    }

    private static byte[] readPart(DataInputStream in) throws IOException {
        int length = in.readInt();
        if (length < 1 || length > 8192) throw new IOException("Invalid identity component");
        byte[] result = new byte[length];
        in.readFully(result);
        return result;
    }
}
