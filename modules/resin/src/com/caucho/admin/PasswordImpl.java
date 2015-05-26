/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.admin;

import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import com.caucho.util.Base64Util;
import com.caucho.util.RandomUtil;

public class PasswordImpl extends Password {
  private static final String ALGORITHM = "AES/CBC/NoPadding";
  private static final String KEY = "V8UrPveQ42sbKqB1wkCdTA==";
  
  private String _salt;
  private String _value;
  
  public PasswordImpl()
  {
    super(true);
  }
  
  @Override
  public void setSalt(String salt)
  {
    _salt = salt;
  }
  
  @Override
  public void setValue(String value)
  {
    _value = value;
  }
  
  @Override
  public Object replaceObject()
  {
    if (_value == null || "".equals(_value))
      return null;
    
    if (! _value.startsWith("{RESIN}"))
      return _value;
    
    String value = _value.substring("{RESIN}".length());
    
    try {
      return decrypt(value, _salt);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public String encrypt(String value, String salt)
    throws Exception
  {
    Cipher cipher = Cipher.getInstance(ALGORITHM);
    
    byte []keyBytes = Base64Util.decodeToByteArray(KEY);
    
    Key key = new SecretKeySpec(keyBytes, "AES");

    /* _iv = new IvParameterSpec(iv); */
    
    cipher.init(Cipher.ENCRYPT_MODE, key, getIv(salt));
    
    byte []valueBytes = value.getBytes("utf-8");
    
    byte []plaintext = new byte[4 + valueBytes.length];
    
    System.arraycopy(valueBytes, 0, plaintext, 4, valueBytes.length);
    
    plaintext[0] = (byte) RandomUtil.getRandomLong();
    plaintext[1] = (byte) RandomUtil.getRandomLong();
    plaintext[2] = (byte) RandomUtil.getRandomLong();
    plaintext[3] = (byte) RandomUtil.getRandomLong();
    
    int len = plaintext.length;
    
    len = (len + 15) / 16 * 16;
    
    byte []paddedText = new byte[len];
    
    System.arraycopy(plaintext, 0, paddedText, 0, plaintext.length);
    
    byte []cipherText = cipher.doFinal(paddedText);

    return Base64Util.encode(cipherText);
  }
  
  private String decrypt(String value, String salt)
    throws NoSuchAlgorithmException,
           NoSuchPaddingException, 
           UnsupportedEncodingException,
           IllegalBlockSizeException,
           BadPaddingException,
           InvalidKeyException, 
           InvalidAlgorithmParameterException
  {
    Cipher cipher = Cipher.getInstance(ALGORITHM);
    
    byte []keyBytes = Base64Util.decodeToByteArray(KEY);
    
    Key key = new SecretKeySpec(keyBytes, "AES");

    /* _iv = new IvParameterSpec(iv); */
    
    cipher.init(Cipher.DECRYPT_MODE, key, getIv(salt));
    
    byte []rawCipherText = Base64Util.decodeToByteArray(value);
    
    int len = rawCipherText.length;
    
    len = (len + 15) / 16 * 16;
    
    byte []cipherText = new byte[len];
    
    System.arraycopy(rawCipherText, 0, cipherText, 0, rawCipherText.length);
    
    byte []plainText = cipher.doFinal(cipherText);
    
    int tail = plainText.length;
    
    for (; tail > 0 && plainText[tail - 1] == 0; tail--) {
    }
    
    if (tail < 4)
      return "";
    else
      return new String(plainText, 4, tail - 4, "utf-8");
  }
  
  private static IvParameterSpec getIv(String salt)
  {
    byte []iv = new byte[16];
    
    for (int i = 0; salt != null && salt.length() > 0 && i < iv.length; i++) {
      iv[i] = (byte) salt.charAt(i % salt.length());
    }
    
    return new IvParameterSpec(iv);
  }
}
