/*
 * Copyright (c) 1998-2006 Caucho Technology -- all rights reserved
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
 * @author Sam
 */

package com.caucho.quercus.lib;

import java.util.HashMap;
import java.util.Map;

import java.util.logging.Logger;
import java.util.logging.Level;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;

import com.caucho.util.L10N;

import com.caucho.quercus.module.AbstractQuercusModule;
import com.caucho.quercus.module.Optional;

import com.caucho.quercus.env.*;
import com.caucho.config.ConfigException;

/**
 * Mhash functions.
 *
 * This module uses the {@link MessageDigest} class to calculate
 * digests. Typical java installations support MD2, MD5, SHA1, SHA256, SHA384,
 * and SHA512.  Extension libraries such as
 * <a href="http://www.bouncycastle.org/">bouncycastle</a>
 * can be used to extend the digest algorithms that are available.
 *
 * <h3>Configuring the java algorithm name and provider</h3>
 *
 * Configuration can override the default java algorithm name and/or
 * provider that corresponds to the PHP name.  If only a java name
 * is supplied, it is used in a call to {@link MessageDigest#getInstance(String)}.
 * If a java name and provider is supplied they are used in a call
 * to {@link MessageDigest#getInstance(String, String)}.
 *
 * <pre>
 * &lt;init>
 *   &lt;algorithm name="MHASH_TIGER" java-name="PUSSYCAT"/>
 *   &lt;algorithm name="MHASH_GOST" java-name="GOST" java-provider="hogwarts"/>
 * &lt;/init>
 * </pre>
 *
 * <h3>Configuring new algorithm's</h3>
 *
 * Configuration can provide new algorithms to the PHP environment.
 *
 * <pre>
 * &lt;init>
 *   &lt;algorithm name="MHASH_MYSPECIALHASH" java-name="specialhash"/>
 * &lt;/init>
 * </pre>
 *
 * <pre>
 * &lt;?php $hash = mhash(MHASH_MYSPECIALHASH, $data)  ... ?>
 * </pre>
 *
 */
public class QuercusMhashModule extends AbstractQuercusModule {
  private static final L10N L = new L10N(QuercusMhashModule.class);
  private static final Logger log
    = Logger.getLogger(QuercusMhashModule.class.getName());

  private HashMap<Integer, MhashAlgorithm> _algorithmMap
    = new HashMap<Integer, MhashAlgorithm>();

  private int _highestOrdinal;

  public QuercusMhashModule()
  {
    addAlgorithm("MHASH_CRC32", 0, "CRC32");
    addAlgorithm("MHASH_MD5", 1, "MD5");
    addAlgorithm("MHASH_SHA1", 2, "SHA-1");
    addAlgorithm("MHASH_HAVAL256", 3, "HAVAL-256");
    addAlgorithm("MHASH_RIPEMD160", 5, "RIPEMD-160");
    addAlgorithm("MHASH_TIGER", 7, "TIGER");
    addAlgorithm("MHASH_GOST", 8, "GOST");
    addAlgorithm("MHASH_CRC32B", 9, "CRC32B");
    addAlgorithm("MHASH_HAVAL224", 10, "HAVAL-224");
    addAlgorithm("MHASH_HAVAL192", 11, "HAVAL-192");
    addAlgorithm("MHASH_HAVAL160", 12, "HAVAL-160");
    addAlgorithm("MHASH_HAVAL128", 13, "HAVAL-128");
    addAlgorithm("MHASH_TIGER128", 14, "TIGER-128");
    addAlgorithm("MHASH_TIGER160", 15, "TIGER-160");
    addAlgorithm("MHASH_MD4", 16, "MD4");
    addAlgorithm("MHASH_SHA256", 17, "SHA-256");
    addAlgorithm("MHASH_ADLER32", 18, "ADLER-32");
    addAlgorithm("MHASH_SHA224", 19, "SHA-224");
    addAlgorithm("MHASH_SHA512", 20, "SHA-512");
    addAlgorithm("MHASH_SHA384", 21, "SHA-384");
    addAlgorithm("MHASH_WHIRLPOOL", 22, "WHIRLPOOL");
    addAlgorithm("MHASH_RIPEMD128", 23, "RIPEMD-128");
    addAlgorithm("MHASH_RIPEMD256", 24, "RIPEMD-256");
    addAlgorithm("MHASH_RIPEMD320", 25, "RIPEMD-320");
    addAlgorithm("MHASH_SNEFRU128", 26, "SNEFRU-128");
    addAlgorithm("MHASH_SNEFRU256", 27, "SNEFRU-256");
    addAlgorithm("MHASH_MD2", 28, "MD2");
  }

  private void addAlgorithm(String name, int ordinal, String javaName)
  {
    MhashAlgorithm algorithm = new MhashAlgorithm(name, javaName, null);

    _algorithmMap.put(ordinal, algorithm);

    if (ordinal > _highestOrdinal)
      _highestOrdinal = ordinal;
  }

  public void addAlgorithm(MhashAlgorithm algorithm)
    throws ConfigException
  {
    // first, check for reconfigure

    for (MhashAlgorithm compare : _algorithmMap.values()) {
      if (compare.getName().equals(algorithm.getName())) {

        compare.setJavaName(algorithm.getJavaName());
        compare.setJavaProvider(algorithm.getJavaProvider());

        if (compare.createMessageDigest() == null)
          throw new ConfigException(L.l("no MessageDigest for `{0}'", compare));

        log.config(L.l("reconfigured `{0}'", algorithm));

        return;
      }
    }

    // not a reconfigure, so add new

    if (algorithm.createMessageDigest() == null)
      throw new ConfigException(L.l("no MessageDigest for `{0}'", algorithm));

    int ordinal = ++_highestOrdinal;

    _algorithmMap.put(ordinal, algorithm);

    log.config(L.l("added `{0}'", algorithm));

  }

  public Map<String, Value> getConstMap()
  {
    HashMap<String, Value> constMap = new HashMap<String, Value>();

    for (Map.Entry<Integer, MhashAlgorithm> entry : _algorithmMap.entrySet()) {
      constMap.put(entry.getValue().getName(), new LongValue(entry.getKey()));
    }

    return constMap;
  }

  public boolean isExtensionLoaded(String name)
  {
    return "mhash".equals(name);
  }

  public Value mhash(int hash, String data, @Optional String key)
  {
    if (key.length() > 0)
      throw new UnsupportedOperationException("key"); // XXX:

    MhashAlgorithm algorithm = _algorithmMap.get(hash);

    if (algorithm == null)
      return BooleanValue.FALSE;

    MessageDigest messageDigest = algorithm.createMessageDigest();

    if (messageDigest == null) {
      log.warning(L.l("no MessageDigest for {0}", algorithm));

      return BooleanValue.FALSE;
    }

    // XXX: s/b "StringValue data" as parameter
    StringValue dataV = new StringValue(data);

    byte[] result = messageDigest.digest(dataV.toBytes());

    return new StringValue(result);
  }

  /**
   * Returns the highest available hash id.
   */
  public int mhash_count()
  {
    return _highestOrdinal;
  }

  public Value mhash_get_block_size(int hash)
  {
    MhashAlgorithm algorithm = _algorithmMap.get(hash);

    if (algorithm == null || algorithm.createMessageDigest() == null)
      return BooleanValue.FALSE;

    return new LongValue(512); // XXX: stubbed
  }

  public Value mhash_get_hash_name(int hash)
  {
    MhashAlgorithm algorithm = _algorithmMap.get(hash);

    if (algorithm == null)
      return BooleanValue.FALSE;
    else
      return new StringValue(algorithm.getShortName());
  }

  // XXX: public String mhash_keygen_s2k(int hash, String password, String salt, int bytes)

  public static class MhashAlgorithm
  {
    private String _name;
    private String _javaName;
    private String _javaProvider;

    MhashAlgorithm(String name, String javaName, String javaProvider)
    {
      _name = name;
      _javaName = javaName;
      _javaProvider = javaProvider;
    }

    public MhashAlgorithm()
    {
    }

    /**
     * The php name, for example `MHASH_CRC32'.
     */
    public void setName(String name)
    {
      _name = name;
    }

    public String getName()
    {
      return _name;
    }

    /**
     * The algorithm name to use when creating the java {@link MessageDigest}.
     *
     * @see MessageDigest#getInstance(String)
     */
    public void setJavaName(String javaName)
    {
      _javaName = javaName;
    }

    public String getJavaName()
    {
      return _javaName;
    }

    /**
     * The provider name to use when creating the java {@link MessageDigest},
     * null for the default.
     *
     * @see MessageDigest#getInstance(String, String)
     */
    public void setJavaProvider(String javaProvider)
    {
      _javaProvider = javaProvider;
    }

    public String getJavaProvider()
    {
      return _javaProvider;
    }

    public void init()
      throws ConfigException
    {
      if (_name == null)
        throw new ConfigException(L.l("`{0}' is required", "name"));

      if (!_name.startsWith("MHASH_"))
        throw new ConfigException(L.l("`{0}' must begin with `{1}'", "name", "MHASH_"));

      if (_javaName == null)
        throw new ConfigException(L.l("`{0}' is required", "java-name"));
    }

    /**
     * Returns the name without the MHASH_ prefix.
     */
    public String getShortName()
    {
      return _name.substring(6);
    }

    /**
     * Create a MessageDigest using the javaName (and javaProvider, if not null).
     */
    public MessageDigest createMessageDigest()
    {
      try {
        if (_javaProvider != null)
          return MessageDigest.getInstance(_javaName, _javaProvider);
        else
          return MessageDigest.getInstance(_javaName);
      }
      catch (NoSuchAlgorithmException ex) {
        if (log.isLoggable(Level.FINE))
          log.log(Level.FINE, ex.toString(), ex);

        return null;
      }
      catch (NoSuchProviderException ex) {
        if (log.isLoggable(Level.FINE))
          log.log(Level.FINE, ex.toString(), ex);

        return null;
      }
    }

    public String toString()
    {
      return
        "MhashAlgorithm[name=" + _name +
        " java-name=" + _javaName +
        " java-provider=" + _javaProvider +
        "]";
    }
  }
}

