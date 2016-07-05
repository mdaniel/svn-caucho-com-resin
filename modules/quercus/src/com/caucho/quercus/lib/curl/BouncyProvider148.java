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

import java.io.Reader;
import java.io.StringReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.security.PrivateKey;
import java.security.Provider;

/**
 * For bouncy 1.48 and above.
 */
public class BouncyProvider148 extends BouncyProvider
{
  public PrivateKey getPrivateKey(String str, String password)
    throws Exception
  {
    StringReader reader = new StringReader(str);
    
    try {
      BouncyCastleProvider provider = new BouncyCastleProvider();
      
      PEMParser parser = new PEMParser(reader);
      Object obj = parser.readObject();
      
      PEMKeyPair keyPair = PEMKeyPair.create(obj);
      PrivateKeyInfo keyInfo;
      
      if (keyPair != null) {
        keyInfo = keyPair.getPrivateKeyInfo();
      }
      else {
        PKCS8EncryptedPrivateKeyInfo encryptedKeyInfo = new PKCS8EncryptedPrivateKeyInfo(obj);
        
        JceOpenSSLPKCS8DecryptorProviderBuilder builder = new JceOpenSSLPKCS8DecryptorProviderBuilder();
        builder.setProvider(provider);
        InputDecryptorProvider input = builder.build(password.toCharArray());
        
        keyInfo = encryptedKeyInfo.decryptPrivateKeyInfo(input);
      }
      
      PrivateKey key = provider.getPrivateKey(keyInfo);
      
      return key;
    }
    finally {
      reader.close();
    }
  }
  
  static class BouncyCastleProvider {
    private Object _obj;
    private Method _getPrivateKeyMethod;
    
    public BouncyCastleProvider()
      throws Exception
    {
      Class<?> cls = Class.forName("org.bouncycastle.jce.provider.BouncyCastleProvider");
      Class<?> keyCls = Class.forName("org.bouncycastle.asn1.pkcs.PrivateKeyInfo");
      
      _getPrivateKeyMethod = cls.getMethod("getPrivateKey", keyCls);
      
      _obj = cls.newInstance();
    }
    
    public PrivateKey getPrivateKey(PrivateKeyInfo keyInfo)
      throws Exception
    {
      Object obj = _getPrivateKeyMethod.invoke(_obj, keyInfo.getObject());
      
      return (PrivateKey) obj;
    }
    
    public Object getObject()
    {
      return _obj;
    }
  }
  
  static class PEMParser {
    private Object _pemParser;
    private Method _readObjectMethod;
    
    public PEMParser(Reader reader)
      throws Exception
    {
      Class<?> cls = Class.forName("org.bouncycastle.openssl.PEMParser");
      Constructor<?> cons = cls.getConstructor(Reader.class);
      _readObjectMethod = cls.getMethod("readObject");

      _pemParser = cons.newInstance(reader);
    }
    
    public Object readObject()
      throws Exception
    {
      return _readObjectMethod.invoke(_pemParser);
    }
  }
  
  static class JceOpenSSLPKCS8DecryptorProviderBuilder {
    private Object _builder;
    private Method _decryptorBuildMethod;
    private Method _decryptorProviderMethod;
    
    public JceOpenSSLPKCS8DecryptorProviderBuilder()
      throws Exception
    {
      Class<?> cls = Class.forName("org.bouncycastle.openssl.jcajce.JceOpenSSLPKCS8DecryptorProviderBuilder");
      _decryptorBuildMethod = cls.getMethod("build", char[].class);
      _decryptorProviderMethod = cls.getMethod("setProvider", Provider.class);

      _builder = cls.newInstance();
    }
    
    public void setProvider(BouncyCastleProvider provider)
      throws Exception
    {
      _decryptorProviderMethod.invoke(_builder, provider.getObject());
    }
    
    public InputDecryptorProvider build(char[] password)
      throws Exception
    {
      Object obj = _decryptorBuildMethod.invoke(_builder, password);
      
      return new InputDecryptorProvider(obj);
    }
  }
  
  static class PKCS8EncryptedPrivateKeyInfo {
    private Object _obj;
    private Method _keyInfoDecryptMethod;
    
    public PKCS8EncryptedPrivateKeyInfo(Object obj)
      throws Exception
    {
      Class<?> cls = Class.forName("org.bouncycastle.pkcs.PKCS8EncryptedPrivateKeyInfo");
      Class<?> inputCls = Class.forName("org.bouncycastle.operator.InputDecryptorProvider");

      _keyInfoDecryptMethod = cls.getMethod("decryptPrivateKeyInfo", inputCls);
      _obj = obj;
    }
    
    public PrivateKeyInfo decryptPrivateKeyInfo(InputDecryptorProvider decryptor)
      throws Exception
    {
      Object obj = _keyInfoDecryptMethod.invoke(_obj, decryptor.getObject());
      
      return new PrivateKeyInfo(obj);
    }
  }
  
  static class InputDecryptorProvider {
    private Object _obj;
    
    public InputDecryptorProvider(Object obj)
      throws Exception
    {
      _obj = obj;
    }
    
    public Object getObject()
    {
      return _obj;
    }
  }
  
  static class PrivateKeyInfo {
    private Object _obj;
    
    public PrivateKeyInfo(Object obj)
    {
      _obj = obj;
    }
    
    public Object getObject()
    {
      return _obj;
    }
  }
  
  static class PEMKeyPair {
    private Object _obj;
    private Method _getPrivateKeyInfoMethod;
    
    public PEMKeyPair(Object obj)
      throws Exception
    {
      Class<?> cls = Class.forName("org.bouncycastle.openssl.PEMKeyPair");
      _getPrivateKeyInfoMethod = cls.getMethod("getPrivateKeyInfo");
      
      _obj = obj;
    }
    
    public static PEMKeyPair create(Object obj)
      throws Exception
    {
      if (obj.getClass().getName().equals("org.bouncycastle.openssl.PEMKeyPair")) {
        return new PEMKeyPair(obj);
      }
      else {
        return null;
      }
    }
    
    public PrivateKeyInfo getPrivateKeyInfo()
      throws Exception
    {
      Object obj = _getPrivateKeyInfoMethod.invoke(_obj);
      
      return new PrivateKeyInfo(obj);
    }
  }
}
