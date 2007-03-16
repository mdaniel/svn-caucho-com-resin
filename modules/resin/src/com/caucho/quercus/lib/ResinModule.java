/*
 * Copyright (c) 1998-2005 Caucho Technology -- all rights reserved
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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Sam
 */


package com.caucho.quercus.lib;

import com.caucho.Version;
import com.caucho.naming.Jndi;
import com.caucho.quercus.QuercusModuleException;
import com.caucho.quercus.annotation.NotNull;
import com.caucho.quercus.annotation.Optional;
import com.caucho.quercus.annotation.ReadOnly;
import com.caucho.quercus.env.*;
import com.caucho.quercus.module.AbstractQuercusModule;
import com.caucho.util.L10N;
import com.caucho.vfs.Vfs;
import com.caucho.vfs.WriteStream;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.transaction.UserTransaction;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Hashtable;
import java.util.IdentityHashMap;
import java.util.Map;


public class ResinModule
  extends AbstractQuercusModule
{
  private static final L10N L = new L10N(ResinModule.class);

  public final static int XA_STATUS_ACTIVE = 0;
  public final static int XA_STATUS_MARKED_ROLLBACK = 1;
  public final static int XA_STATUS_PREPARED = 2;
  public final static int XA_STATUS_COMMITTED = 3;
  public final static int XA_STATUS_ROLLEDBACK = 4;
  public final static int XA_STATUS_UNKNOWN = 5;
  public final static int XA_STATUS_NO_TRANSACTION = 6;
  public final static int XA_STATUS_PREPARING = 7;
  public final static int XA_STATUS_COMMITTING = 8;
  public final static int XA_STATUS_ROLLING_BACK = 9;

  /**
   * Converts a string into its binary representation, according to the
   * given encoding, if given, or the script encoding if not given.
   */
  public static Value string_to_binary(Env env, String string, 
                                                @Optional String encoding)
  {
    if (encoding == null || encoding.length() == 0)
      encoding = env.getScriptEncoding();

    try {
      byte[] bytes = string.getBytes(encoding);

      return new BinaryBuilderValue(bytes);
    } catch (UnsupportedEncodingException e) {

      env.error(e);

      return BooleanValue.FALSE;
    }
  }

  /**
   * Perform a jndi lookup to retrieve an object.
   *
   * @param name a fully qualified name "java:comp/env/foo",
   * or a short-form "foo".

   * @return the object, or null if it is not found.
   */
  public static Object jndi_lookup(String name)
  {
    return Jndi.lookup(name);
  }


  /**
   * Returns the version of the Resin server software.
   */
  public static String resin_version()
  {
    return Version.FULL_VERSION;
  }

  /**
   * Starts a new distributed transaction.
   */
  public static Value xa_begin()
  {
    try {
      getUserTransaction().begin();

      return NullValue.NULL;
    } catch (Exception e) {
      throw new QuercusModuleException(e);
    }
  }

  /**
   * Commits the current transaction.
   */
  public static Value xa_commit()
  {
    try {
      getUserTransaction().commit();

      return NullValue.NULL;
    } catch (Exception e) {
      throw new QuercusModuleException(e);
    }
  }

  /**
   * Complets the current transaction by rolling it back.
   */
  public static Value xa_rollback()
  {
    try {
      getUserTransaction().rollback();

      return NullValue.NULL;
    } catch (Exception e) {
      throw new QuercusModuleException(e);
    }
  }

  /**
   * Sets the rollback_only status for the current transaction.
   */
  public static Value xa_rollback_only(String msg)
  {
    try {
      getUserTransaction().setRollbackOnly();

      return NullValue.NULL;
    } catch (Exception e) {
      throw new QuercusModuleException(e);
    }
  }

  /**
   * Sets the timeout for the current distribued transaction.
   */
  public static Value xa_set_timeout(int timeoutSeconds)
  {
    try {
      getUserTransaction().setTransactionTimeout(timeoutSeconds);

      return NullValue.NULL;
    } catch (Exception e) {
      throw new QuercusModuleException(e);
    }
  }

  /**
   * Returns the JTA status code for the current transation.
   */
  public static int xa_status()
  {
    // XXX: should return a string
    try {
      return getUserTransaction().getStatus();
    } catch (Exception e) {
      throw new QuercusModuleException(e);
    }
  }

  /**
   * Returns the UserTransaction object.
   */
  private static UserTransaction getUserTransaction()
  {
    try {
      // XXX: this could be cached, since it's a constant for the
      // current environment
      
      Context ic = new InitialContext();
      
      return ((UserTransaction) ic.lookup("java:comp/UserTransaction"));
    } catch (NamingException e) {
      throw new QuercusModuleException(e);
    }
  }

  /**
   * Explode an object name into an array with key value pairs that
   * correspond to the keys and values in the object name.
   * The domain is stored in the returned array under the key named ":".
   */
  public ArrayValue mbean_explode(String name)
  {
    try {
      ArrayValueImpl exploded = new ArrayValueImpl();
      
      if (name == null)
        return exploded;
      
      ObjectName objectName = new ObjectName(name);

      exploded.put(":domain:", objectName.getDomain());

      Hashtable<String, String> entries = objectName.getKeyPropertyList();

      for (Map.Entry<String, String> entry : entries.entrySet()) {
	exploded.put(entry.getKey(), entry.getValue());
      }

      return exploded;
    } catch (MalformedObjectNameException e) {
      throw new QuercusModuleException(e);
    }
  }

  /**
   * Implode an array into an object name.  The array contains key value pairs
   * that become key vlaue pairs in the object name.  The key with the name
   * ":" becomes the domain of the object name.
   */
  public static String mbean_implode(@NotNull @ReadOnly ArrayValue exploded)
  {
    try {
      if (exploded == null)
	return null;

      String domain;

      Value domainValue = exploded.get(StringValue.create(":domain:"));

      if (domainValue.isNull())
	domain = "*";
      else
	domain = domainValue.toString();

      Hashtable<String, String> entries = new Hashtable<String, String>();

      for (Map.Entry<Value, Value> entry : exploded.entrySet()) {
	String key = entry.getKey().toString();
	String value = entry.getValue().toString();

	if (":domain:".equals(key))
	  continue;

	entries.put(key, value);
      }

      ObjectName objectName;

      if (entries.isEmpty())
	objectName = new ObjectName(domain + ":" + "*");
      else
	objectName = new ObjectName(domain, entries);

      return objectName.getCanonicalName();
    } catch (MalformedObjectNameException e) {
      throw new QuercusModuleException(e);
    }
  }

  /**
   * Prints a debug version of the variable
   *
   * @param env the quercus calling environment
   * @param v the variable to print
   * @return the escaped stringPhp
   */
  public static Value resin_var_dump(Env env, @ReadOnly Value v)
  {
    try {
      WriteStream out = Vfs.openWrite("stdout:");

      if (v != null)
	v.varDump(env, out, 0, new IdentityHashMap<Value,String>());

      out.println();

      out.close();

      return NullValue.NULL;
    } catch (IOException e) {
      throw new QuercusModuleException(e);
    }
  }
}
