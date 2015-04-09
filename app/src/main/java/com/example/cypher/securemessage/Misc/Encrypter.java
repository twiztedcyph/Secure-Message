package com.example.cypher.securemessage.Misc;

import android.content.Context;
import android.security.KeyPairGeneratorSpec;
import android.util.Base64;

import java.io.IOException;
import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Calendar;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.security.auth.x500.X500Principal;

public class Encrypter
{
    private KeyPairGenerator keyPairGenerator;
    private KeyStore.PrivateKeyEntry keyEntry;
    private KeyPair keyPair;
    private PublicKey publicKey;
    private PrivateKey privateKey;
    private byte[] encryptedByte, decryptedByte;
    private Cipher cipherOne, cipherTwo;
    private String encryptedString, decryptedString;
    private final String TAG = "com.twizted.secmsg";

    public Encrypter()
    {
    }

    public void genKeys(Context context, String alias) throws
    NoSuchProviderException,
    NoSuchAlgorithmException,
    InvalidAlgorithmParameterException
    {
        Calendar notBefore = Calendar.getInstance();
        Calendar notAfter = Calendar.getInstance();
        notAfter.add(Calendar.YEAR, 1);

        KeyPairGeneratorSpec spec = new KeyPairGeneratorSpec.Builder(context)
                .setAlias(alias)
                .setSubject(new X500Principal(String.format("CN=%s, OU=%s", alias, context.getPackageName())))
                .setSerialNumber(BigInteger.ONE)
                .setStartDate(notBefore.getTime())
                .setEndDate(notAfter.getTime())
                .build();

        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA", "AndroidKeyStore");
        kpg.initialize(spec);
        KeyPair kp = kpg.generateKeyPair();
    }

    public RSAPublicKey getMyPubKey()
    throws KeyStoreException, CertificateException,
    NoSuchAlgorithmException, IOException,
    UnrecoverableEntryException
    {
        KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
        keyStore.load(null);
        keyEntry = (KeyStore.PrivateKeyEntry) keyStore.getEntry("myKey", null);
        return (RSAPublicKey) keyEntry.getCertificate().getPublicKey();
    }

    public RSAPrivateKey getMyPrivKey()
    throws CertificateException, NoSuchAlgorithmException,
    IOException, KeyStoreException,
    UnrecoverableEntryException
    {
        KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
        keyStore.load(null);
        keyEntry = (KeyStore.PrivateKeyEntry) keyStore.getEntry("myKey", null);
        return (RSAPrivateKey) keyEntry.getPrivateKey();
    }

    public String encrypt(String plainText, PublicKey publicKey) throws NoSuchAlgorithmException,
    NoSuchPaddingException, InvalidKeyException,
    IllegalBlockSizeException, BadPaddingException, NoSuchProviderException
    {
        cipherOne = Cipher.getInstance("RSA/ECB/PKCS1Padding", "AndroidOpenSSL");
        cipherOne.init(Cipher.ENCRYPT_MODE, publicKey);

        encryptedByte = cipherOne.doFinal(plainText.getBytes());
        encryptedString = Base64.encodeToString(encryptedByte, Base64.NO_WRAP);

        return encryptedString;
    }

    public String decrypt(String cipherText, PrivateKey privateKey) throws NoSuchAlgorithmException,
    NoSuchPaddingException, InvalidKeyException,
    IllegalBlockSizeException, BadPaddingException, NoSuchProviderException
    {
        cipherTwo = Cipher.getInstance("RSA/ECB/PKCS1Padding", "AndroidOpenSSL");
        cipherTwo.init(Cipher.DECRYPT_MODE, privateKey);

        decryptedByte = cipherTwo.doFinal(Base64.decode(cipherText, Base64.NO_WRAP));
        decryptedString = new String(decryptedByte);

        return decryptedString;

    }
}

