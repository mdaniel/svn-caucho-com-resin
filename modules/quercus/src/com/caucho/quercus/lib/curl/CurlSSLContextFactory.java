/*
 * Copyright (c) 1998-2016 Caucho Technology -- all rights reserved
 *
 * This file is part of Resin(R) Open Source
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Resin Open Source is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * Resin Open Source is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, or any warranty
 * of NON-INFRINGEMENT.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Resin Open Source; if not, write to the
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Nam Nguyen
 */

package com.caucho.quercus.lib.curl;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.AlgorithmParameters;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.KeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPrivateCrtKeySpec;

import javax.crypto.Cipher;
import javax.crypto.EncryptedPrivateKeyInfo;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509KeyManager;
import javax.net.ssl.X509TrustManager;
import javax.xml.bind.DatatypeConverter;

import sun.security.util.DerInputStream;
import sun.security.util.DerValue;

public class CurlSSLContextFactory
{
  private static String PKCS12_HEADER = "-----BEGIN RSA PRIVATE KEY-----";
  private static String PKCS12_FOOTER = "-----END RSA PRIVATE KEY-----";
  
  private static String PKCS8_HEADER = "-----BEGIN PRIVATE KEY-----";
  private static String PKCS8_FOOTER = "-----END PRIVATE KEY-----";
  
  private static String PKCS8_ENC_HEADER = "-----BEGIN ENCRYPTED PRIVATE KEY-----";
  private static String PKCS8_ENC_FOOTER = "-----END ENCRYPTED PRIVATE KEY-----";
  
  public static SSLContext createUntrusted(String algorithm)
    throws Exception
  {    
    SSLContext sslContext = SSLContext.getInstance(algorithm);

    X509TrustManager trustManager = new X509TrustManager() {
      @Override
      public void checkClientTrusted(X509Certificate[] arg0, String arg1) throws CertificateException
      {
      }

      @Override
      public void checkServerTrusted(X509Certificate[] arg0, String arg1) throws CertificateException
      {
      }

      @Override
      public X509Certificate[] getAcceptedIssuers()
      {
        return null;
      }
      
    };
    
    sslContext.init(null, new TrustManager[] { trustManager }, new SecureRandom());
    
    return sslContext;
  }
  
  public static SSLContext createCaInfo(String algorithm, String certFile)
    throws Exception
  {    
    SSLContext sslContext = SSLContext.getInstance(algorithm);

    String keyAlias = "server_key";
    X509Certificate cert = getCertificate(certFile);
    
    KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
    keyStore.load(null);
    keyStore.setCertificateEntry(keyAlias, cert);
    
    KeyManagerFactory kmfactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
    kmfactory.init(keyStore, null);
    KeyManager[] keyManagers = kmfactory.getKeyManagers();
    
    TrustManagerFactory tmf = TrustManagerFactory.getInstance("X509");
    tmf.init(keyStore);
    TrustManager[] trustManagers = tmf.getTrustManagers();
    
    sslContext.init(keyManagers, trustManagers, null);
    
    return sslContext;
  }
  
  public static SSLContext create(String certFile, String keyFile,
                                  String password, String algorithm)
    throws Exception
  {
    X509Certificate cert = getCertificate(certFile);
    //PublicKey publicKey = cert.getPublicKey();
        
    PrivateKey privateKey = getPrivateKey(keyFile, password);
    
    KeyStore keystore = KeyStore.getInstance("JKS");
    keystore.load(null, null);
    
    String keyAlias = "client_key";
    KeyStore.TrustedCertificateEntry trustedEntry = new KeyStore.TrustedCertificateEntry(cert);
    keystore.setEntry(keyAlias, trustedEntry, null);
   
    String keyStorePassword = "changeit";
    
    if (password != null) {
      keyStorePassword = password;
    }
    
    keystore.setKeyEntry(keyAlias + "_prv", privateKey, keyStorePassword.toCharArray(), new Certificate[] {cert});
    
    KeyManagerFactory kmfactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
    kmfactory.init(keystore, keyStorePassword.toCharArray());
    
    KeyManager[] keymanagers = kmfactory.getKeyManagers();
    if (keyAlias != null) {
      for (int i = 0; i < keymanagers.length; i++) {
        keymanagers[i] = new CurlX509KeyManager((X509KeyManager) keymanagers[i], keyAlias);
      }
    }
    
    SSLContext sslcontext = SSLContext.getInstance(algorithm);
    sslcontext.init(keymanagers, null, new SecureRandom());
    
    return sslcontext;
  }
  
  private static X509Certificate getCertificate(String file)
    throws Exception
  {
    CertificateFactory fact = CertificateFactory.getInstance("X.509");

    InputStream is = new FileInputStream (file);
    X509Certificate cert = (X509Certificate) fact.generateCertificate(is);
    
    return cert;
  }
  
  private static PrivateKey getPrivateKey(String fileName, String password)
    throws Exception
  {
    FileInputStream is = new FileInputStream(fileName);
    StringBuilder sb = new StringBuilder();
    
    try {
      int ch;
      while ((ch = is.read()) >= 0) {
        sb.append((char) ch);
      }
    }
    finally {
      is.close();
    }
    
    String str = sb.toString();
    
    PrivateKey key = null;
    
    BouncyProvider bouncy = BouncyProvider.getBouncy();
    
    if (bouncy != null) {
      key = bouncy.getPrivateKey(str, password);
    }
    
    if (key == null) {
      key = getPrivateKeyPkcs12(str);
    }

    if (key == null) {
      key = getPrivateKeyPkcs8(str);
    }
    
    if (key == null) {
      // only works for older encryption versions
      key = getPrivateKeyPkcs8Encrypted(str, password);
    }
    
    if (key == null) {
      throw new IOException("unknown key format: " + fileName);
    }

    return key;
  }
    
  
  private static byte[] getBytes(String str, String header, String footer)
    throws Exception
  {
    int p = str.indexOf(header);
    
    if (p < 0) {
      return null;
    }
    
    int q = str.indexOf(footer, p + header.length());
    
    if (q < 0) {
      throw new IOException("missing footer: " + footer);
    }
    
    str = str.substring(p + header.length(), q);
    String base64 = str.replaceAll("\\s", "");
    
    //byte[] bytes = Base64.getDecoder().decode(base64);

    byte[] bytes = DatatypeConverter.parseBase64Binary(base64);
    
    return bytes;
  }
    
  private static PrivateKey getPrivateKeyPkcs12(String str)
    throws Exception
  {
    byte[] bytes = getBytes(str, PKCS12_HEADER, PKCS12_FOOTER);
    
    if (bytes == null) {
      return null;
    }
    
    DerInputStream derReader = new DerInputStream(bytes);
    DerValue[] seq = derReader.getSequence(0);

    BigInteger modulus = seq[1].getBigInteger();
    BigInteger publicExp = seq[2].getBigInteger();
    BigInteger privateExp = seq[3].getBigInteger();
    BigInteger prime1 = seq[4].getBigInteger();
    BigInteger prime2 = seq[5].getBigInteger();
    BigInteger exp1 = seq[6].getBigInteger();
    BigInteger exp2 = seq[7].getBigInteger();
    BigInteger crtCoef = seq[8].getBigInteger();

    RSAPrivateCrtKeySpec keySpec = new RSAPrivateCrtKeySpec(modulus, publicExp, privateExp, prime1, prime2, exp1, exp2, crtCoef);

    KeyFactory factory = KeyFactory.getInstance("RSA");

    return factory.generatePrivate(keySpec);
  }
  
  private static PrivateKey getPrivateKeyPkcs8(String str)
    throws Exception
  {
    byte[] bytes = getBytes(str, PKCS8_HEADER, PKCS8_FOOTER);
    
    if (bytes == null) {
      return null;
    }

    KeyFactory factory = KeyFactory.getInstance("RSA");

    return factory.generatePrivate(new PKCS8EncodedKeySpec(bytes));
  }
  
  private static PrivateKey getPrivateKeyPkcs8Encrypted(String str, String password)
    throws Exception
  {
    byte[] bytes = getBytes(str, PKCS8_ENC_HEADER, PKCS8_ENC_FOOTER);

    if (bytes == null) {
      return null;
    }
    
    EncryptedPrivateKeyInfo encryptPKInfo = new EncryptedPrivateKeyInfo(bytes);

    Cipher cipher = Cipher.getInstance(encryptPKInfo.getAlgName());
    
    PBEKeySpec pbeKeySpec = new PBEKeySpec(password.toCharArray());
    SecretKeyFactory secFac = SecretKeyFactory.getInstance(encryptPKInfo.getAlgName());
    Key pbeKey = secFac.generateSecret(pbeKeySpec);
    
    AlgorithmParameters algParams = encryptPKInfo.getAlgParameters();
    cipher.init(Cipher.DECRYPT_MODE, pbeKey, algParams);
    KeySpec keySpec = encryptPKInfo.getKeySpec(cipher);
    
    KeyFactory factory = KeyFactory.getInstance("RSA");

    return factory.generatePrivate(keySpec);
  }
}
