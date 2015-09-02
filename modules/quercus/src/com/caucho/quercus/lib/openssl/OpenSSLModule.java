package com.caucho.quercus.lib.openssl;

import java.util.Random;

import com.caucho.quercus.annotation.Optional;
import com.caucho.quercus.annotation.Reference;
import com.caucho.quercus.env.ArgRef;
import com.caucho.quercus.env.BooleanValue;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.StringValue;
import com.caucho.quercus.env.Value;
import com.caucho.quercus.module.AbstractQuercusModule;

public class OpenSSLModule extends AbstractQuercusModule
{
  /**
   * (PHP 5 &gt;= 5.3.0)<br/>
   * Generates a string of pseudo-random bytes, with the number of bytes determined by the length parameter.
   * <p>It also indicates if a cryptographically strong algorithm was used to produce the pseudo-random bytes,
   * and does this via the optional crypto_strong parameter. It's rare for this to be FALSE, but some systems may be broken or old.
   * @link http://php.net/manual/en/function.openssl-random-pseudo-bytes.php
   * @param int $length <p>
   * The length of the desired string of bytes. Must be a positive integer. PHP will
   * try to cast this parameter to a non-null integer to use it.
   * </p>
   * @param bool $crypto_strong [optional]<p>
   * If passed into the function, this will hold a boolean value that determines
   * if the algorithm used was "cryptographically strong", e.g., safe for usage with GPG,
   * passwords, etc. true if it did, otherwise false
   * </p>
   * @return string the generated &string; of bytes on success, or false on failure.
   */
  public static Value openssl_random_pseudo_bytes(Env env, Value length,
      @Optional @Reference Value cryptoStrong) {
    if (cryptoStrong != null) {
      cryptoStrong.set(BooleanValue.TRUE);
    }
    Random random = new Random();
    byte[] bytes = new byte[length.toInt()];
    for (int i = 0; i < bytes.length;) {
      for (int rnd = random.nextInt(Integer.MAX_VALUE), n = Math.min(
          bytes.length - i, 4); n-- > 0; rnd >>= 8)
        bytes[i++] = (byte) Math.abs((byte) rnd);
    }
    return StringValue.create(new String(bytes));
  }
}
