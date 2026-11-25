package com.stagesgraphs;

import java.security.*;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/** Password storage compatible with Android 5.1. */
public final class ProfilePassword {
    private static final int ITERATIONS = 20000;
    public static String create(String password) {
        if (password == null || password.isEmpty() || password.length() > 64)
            throw new IllegalArgumentException("Enter a password (1-64 characters)");
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        return hex(salt)+":"+hex(derive(password,salt));
    }
    public static boolean matches(String password,String stored) {
        if (password == null || password.isEmpty() || password.length() > 64 || stored == null) return false;
        String[] fields = stored.split(":",-1);
        if (fields.length != 2 || fields[0].length() != 32 || fields[1].length() != 64) return false;
        try { return MessageDigest.isEqual(unhex(fields[1]),derive(password,unhex(fields[0]))); }
        catch (IllegalArgumentException invalid) { return false; }
    }
    private static byte[] derive(String password,byte[] salt) {
        PBEKeySpec key = new PBEKeySpec(password.toCharArray(),salt,ITERATIONS,256);
        try { return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1").generateSecret(key).getEncoded(); }
        catch (GeneralSecurityException unavailable) { throw new IllegalStateException(unavailable); }
        finally { key.clearPassword(); }
    }
    private static String hex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte value : bytes) {
            result.append(Character.forDigit((value & 255) >>> 4,16));
            result.append(Character.forDigit(value & 15,16));
        }
        return result.toString();
    }
    private static byte[] unhex(String text) {
        byte[] result = new byte[text.length()/2];
        for (int i=0;i<result.length;i++) {
            int high = Character.digit(text.charAt(i*2),16),low = Character.digit(text.charAt(i*2+1),16);
            if (high < 0 || low < 0) throw new IllegalArgumentException("Invalid password record");
            result[i] = (byte)((high << 4) | low);
        }
        return result;
    }
}
