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
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.Security;

/**
 * For bouncy 1.47 and below.
 */
public class BouncyProviderOld extends BouncyProvider
{ 
  public PrivateKey getPrivateKey(String str, String password)
    throws Exception
  {
    Provider provider = Security.getProvider("BC");
    if (provider == null) {
      BouncyProvider bouncyProvider = new BouncyProvider();
      
      Security.addProvider(bouncyProvider.getObject());
    }
    
    StringReader strReader = new StringReader(str);
    PEMReader reader = new PEMReader(strReader, password);
    
    Object obj = reader.readObject();
        
    if (obj instanceof PrivateKey) {
      return (PrivateKey) obj;
    }
    
    KeyPair keyPair = (KeyPair) obj;
    
    return keyPair.getPrivate();
  }
  
  static class BouncyProvider {
    private Provider _obj;
    
    public BouncyProvider()
      throws Exception
    {
      Class<?> cls = Class.forName("org.bouncycastle.jce.provider.BouncyCastleProvider");
      _obj = (Provider) cls.newInstance();
    }
    
    public Provider getObject()
    {
      return _obj;
    }
  }
  
  static class PEMReader {
    private Object _obj;
    private Method _readObjectMethod;
    
    public PEMReader(Reader reader)
      throws Exception
    {
      Class<?> cls = Class.forName("org.bouncycastle.openssl.PEMReader");
      Constructor<?> cons = cls.getConstructor(Reader.class);
      _readObjectMethod = cls.getMethod("readObject");
      
      _obj = cons.newInstance(reader);
    }
    
    public PEMReader(Reader reader, String password)
      throws Exception
    {
      Class<?> cls = Class.forName("org.bouncycastle.openssl.PEMReader");
      Class<?> finderCls = Class.forName("org.bouncycastle.openssl.PasswordFinder");
      _readObjectMethod = cls.getMethod("readObject");

      if (password != null) {
        Constructor<?> cons = cls.getConstructor(Reader.class, finderCls);

        PasswordFinder finder = new PasswordFinder(password);
        
        _obj = cons.newInstance(reader, finder.getObject());
      }
      else {
        Constructor<?> cons = cls.getConstructor(Reader.class);

        _obj = cons.newInstance(reader);
      }
    }
    
    public Object readObject()
      throws Exception
    {
      Object obj = _readObjectMethod.invoke(_obj);
      
      return obj;
    }
  }
  
  static class PasswordFinder implements InvocationHandler {
    private Object _obj;
    private String _password;
    
    public PasswordFinder(String password)
      throws Exception
    {
      _password = password;
      
      Class<?> cls = Class.forName("org.bouncycastle.openssl.PasswordFinder");
      ClassLoader loader = Thread.currentThread().getContextClassLoader();
      
      _obj = Proxy.newProxyInstance(loader, new Class<?>[] { cls }, this);
    }
    
    public Object getObject()
    {
      return _obj;
    }

    public Object invoke(Object proxy, Method method, Object[] args)
      throws Throwable
    {
      return _password.toCharArray();
    }

  }
}
