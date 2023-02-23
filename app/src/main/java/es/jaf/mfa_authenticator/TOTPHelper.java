package es.jaf.mfa_authenticator;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;


public class TOTPHelper {
    public static final String SHA1 = "HmacSHA1";
    public static final String SHA256 = "HmacSHA256";
    public static final String SHA512 = "HmacSHA512";

    public static String generate(byte[] secret, int digits, String algorithm) {
        return String.format("%06d", generate(secret, digits, System.currentTimeMillis() / 1000, algorithm));
    }

    private static int generate(byte[] key, int digits, long t, String algorithm) {
        int r = 0;
        try {
            t /= 30;
            byte[] data = new byte[8];
            long value = t;
            for (int i = 8; i-- > 0; value >>>= 8) {
                data[i] = (byte) value;
            }

            String alg;
            if ("SHA512".equalsIgnoreCase(algorithm)) {
                alg = SHA512;
            } else if ("SHA256".equalsIgnoreCase(algorithm)) {
                alg = SHA256;
            } else {
                alg = SHA1;
            }
            SecretKeySpec signKey = new SecretKeySpec(key, alg);
            Mac mac = Mac.getInstance(alg);
            mac.init(signKey);
            byte[] hash = mac.doFinal(data);


            int offset = hash[20 - 1] & 0xF;

            long truncatedHash = 0;
            for (int i = 0; i < 4; ++i) {
                truncatedHash <<= 8;
                truncatedHash |= (hash[offset + i] & 0xFF);
            }

            truncatedHash &= 0x7FFFFFFF;
            truncatedHash %= Math.pow(10,digits);

            r  = (int) truncatedHash;
        }

        catch(Exception e){
            //Nothing
        }

        return r;
    }
}
