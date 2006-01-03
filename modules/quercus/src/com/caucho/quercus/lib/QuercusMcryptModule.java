/*
 * Copyright (c) 1998-2004 Caucho Technology -- all rights reserved
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
 * @author Scott Ferguson
 */

package com.caucho.quercus.lib;

import java.util.logging.*;

import com.caucho.util.L10N;

import com.caucho.vfs.Path;

import com.caucho.quercus.module.AbstractQuercusModule;
import com.caucho.quercus.module.Optional;

import com.caucho.quercus.env.Value;
import com.caucho.quercus.env.BooleanValue;
import com.caucho.quercus.env.LongValue;
import com.caucho.quercus.env.Env;

/**
 * PHP encryption
 */
public class QuercusMcryptModule extends AbstractQuercusModule {
  private static final L10N L = new L10N(QuercusMcryptModule.class);

  private static final Logger log =
    Logger.getLogger(QuercusMcryptModule.class.getName());

  public static final int MCRYPT_DEV_RANDOM = 0;
  public static final int MCRYPT_DEV_URANDOM = 1;
  public static final int MCRYPT_RAND = 2;
  
  public static final String MCRYPT_MODE_ECB = "ecb";
  public static final String MCRYPT_MODE_CBC = "cbc";
  public static final String MCRYPT_MODE_CFB = "cfb";
  public static final String MCRYPT_MODE_OFB = "ofb";
  public static final String MCRYPT_MODE_NOFB = "nofb";
  public static final String MCRYPT_MODE_STREAM = "stream";

  public static final String MCRYPT_ARCFOUR = "arcfour";
  public static final String MCRYPT_BLOWFISH = "blowfish";
  public static final String MCRYPT_DES = "des";
  public static final String MCRYPT_3DES = "tripledes";
  public static final String MCRYPT_RC4 = "RC4";
  public static final String MCRYPT_RIJNDAEL_128 = "rijndael-128";
  public static final String MCRYPT_RIJNDAEL_192 = "rijndael-192";
  public static final String MCRYPT_RIJNDAEL_256 = "rijndael-256";

  /**
   * Creates the IV vector.
   */
  public static String mcrypt_create_iv(int size,
					@Optional int randomMode)
  {
    StringBuilder sb = new StringBuilder();

    for (int i = 0; i < size; i++)
      sb.append((char) RandomUtil.nextInt(256));

    return sb.toString();
  }

  /**
   * Returns the algorithms name
   */
  public static String mcrypt_enc_get_algorithms_name(Mcrypt mcrypt)
  {
    if (mcrypt != null)
      return mcrypt.get_algorithms_name();
    else
      return "";
  }

  /**
   * Returns the block size
   */
  public static int mcrypt_enc_get_block_size(Mcrypt mcrypt)
  {
    if (mcrypt != null)
      return mcrypt.get_iv_size();
    else
      return 0;
  }

  /**
   * Returns the IV size
   */
  public static int mcrypt_enc_get_iv_size(Mcrypt mcrypt)
  {
    if (mcrypt != null)
      return mcrypt.get_iv_size();
    else
      return 0;
  }

  /**
   * Returns the key size
   */
  public static int mcrypt_enc_get_key_size(Mcrypt mcrypt)
  {
    if (mcrypt != null)
      return mcrypt.get_key_size();
    else
      return 0;
  }

  /**
   * Encrypt
   */
  public static String mcrypt_encrypt(Env env,
				      String cipher,
				      String key,
				      String data,
				      String mode,
				      @Optional String iv)
  {
    try {
      Mcrypt mcrypt = new Mcrypt(env, cipher, mode);

      mcrypt.init(key, iv);

      return new String(mcrypt.encrypt(data.getBytes()));
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Decrypt
   */
  public static String mcrypt_decrypt(Env env,
				      String cipher,
				      String key,
				      String data,
				      String mode,
				      @Optional String iv)
  {
    try {
      Mcrypt mcrypt = new Mcrypt(env, cipher, mode);

      mcrypt.init(key, iv);

      return new String(mcrypt.decrypt(data.getBytes()));
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Initialize encrption
   */
  public static String mcrypt_generic(Mcrypt mcrypt, String data)
  {
    if (mcrypt == null)
      return null;
    else
      return new String(mcrypt.encrypt(data.getBytes()));
  }

  /**
   * Initialize encrption
   */
  public static boolean mcrypt_generic_deinit(Mcrypt mcrypt)
  {
    if (mcrypt == null)
      return false;
    else
      return mcrypt.deinit();
  }

  /**
   * Initialize encrption
   */
  public static Value mcrypt_generic_init(Mcrypt mcrypt, String key, String iv)
  {
    if (mcrypt == null)
      return BooleanValue.FALSE;
    else
      return new LongValue(mcrypt.init(key, iv));
  }

  /**
   * Closes the module
   */
  public static boolean mcrypt_generic_end(Mcrypt mcrypt)
  {
    if (mcrypt == null)
      return false;
    else {
      mcrypt.close();
      
      return true;
    }
  }

  /**
   * Closes the module
   */
  public static boolean mcrypt_module_close(Mcrypt mcrypt)
  {
    if (mcrypt == null)
      return false;
    else {
      mcrypt.close();
      
      return true;
    }
  }

  /**
   * Returns the block size for an algorithm.
   */
  public static int mcrypt_module_get_algo_block_size(Env env,
						      String cipher,
						      @Optional String libDir)
  {
    try {
      Mcrypt mcrypt = new Mcrypt(env, cipher, "cbc");

      return mcrypt.get_block_size();
    } catch (Exception e) {
      env.error(e);

      return -1;
    }
  }

  /**
   * Returns the key size for an algorithm.
   */
  public static int mcrypt_module_get_algo_key_size(Env env,
						    String cipher,
						    @Optional String libDir)
  {
    try {
      Mcrypt mcrypt = new Mcrypt(env, cipher, "cbc");

      return mcrypt.get_key_size();
    } catch (Exception e) {
      env.error(e);

      return -1;
    }
  }

  /**
   * Open a new mcrypt object.
   */
  public static Value mcrypt_module_open(Env env,
					 String algorithm,
					 Path algorithm_directory,
					 String mode,
					 Path mode_directory)
  {
    try {
      return env.wrapJava(new Mcrypt(env, algorithm, mode));
    } catch (Exception e) {
      env.error(e);

      return BooleanValue.FALSE;
    }
  }

  /**
   * Initialize encrption
   */
  public static String mdecrypt_generic(Mcrypt mcrypt, String data)
  {
    if (mcrypt == null)
      return null;
    else
      return new String(mcrypt.decrypt(data.getBytes()));
  }
}
