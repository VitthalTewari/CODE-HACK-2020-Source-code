package okio;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.apache.commons.codec.digest.MessageDigestAlgorithms;

public final class HashingSource extends ForwardingSource {
    private final Mac mac;
    private final MessageDigest messageDigest;

    public static HashingSource md5(Source source) {
        return new HashingSource(source, MessageDigestAlgorithms.MD5);
    }

    public static HashingSource sha1(Source source) {
        return new HashingSource(source, MessageDigestAlgorithms.SHA_1);
    }

    public static HashingSource sha256(Source source) {
        return new HashingSource(source, MessageDigestAlgorithms.SHA_256);
    }

    public static HashingSource hmacSha1(Source source, ByteString byteString) {
        return new HashingSource(source, byteString, "HmacSHA1");
    }

    public static HashingSource hmacSha256(Source source, ByteString byteString) {
        return new HashingSource(source, byteString, "HmacSHA256");
    }

    private HashingSource(Source source, String str) {
        super(source);
        try {
            this.messageDigest = MessageDigest.getInstance(str);
            this.mac = null;
        } catch (NoSuchAlgorithmException unused) {
            throw new AssertionError();
        }
    }

    private HashingSource(Source source, ByteString byteString, String str) {
        super(source);
        try {
            this.mac = Mac.getInstance(str);
            this.mac.init(new SecretKeySpec(byteString.toByteArray(), str));
            this.messageDigest = null;
        } catch (NoSuchAlgorithmException unused) {
            throw new AssertionError();
        } catch (InvalidKeyException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public long read(Buffer buffer, long j) throws IOException {
        long read = super.read(buffer, j);
        if (read != -1) {
            long j2 = buffer.size - read;
            long j3 = buffer.size;
            Segment segment = buffer.head;
            while (j3 > j2) {
                segment = segment.prev;
                j3 -= (long) (segment.limit - segment.pos);
            }
            while (j3 < buffer.size) {
                int i = (int) ((((long) segment.pos) + j2) - j3);
                if (this.messageDigest != null) {
                    this.messageDigest.update(segment.data, i, segment.limit - i);
                } else {
                    this.mac.update(segment.data, i, segment.limit - i);
                }
                j2 = ((long) (segment.limit - segment.pos)) + j3;
                segment = segment.next;
                j3 = j2;
            }
        }
        return read;
    }

    public final ByteString hash() {
        return ByteString.m1605of(this.messageDigest != null ? this.messageDigest.digest() : this.mac.doFinal());
    }
}
