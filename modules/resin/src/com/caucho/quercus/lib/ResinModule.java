/*
 * Copyright (c) 1998-2008 Caucho Technology -- all rights reserved
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

import com.caucho.Version;
import com.caucho.config.inject.InjectManager;
import com.caucho.naming.Jndi;
import com.caucho.quercus.QuercusModuleException;
import com.caucho.quercus.annotation.NotNull;
import com.caucho.quercus.annotation.Optional;
import com.caucho.quercus.annotation.ReadOnly;
import com.caucho.quercus.annotation.Name;
import com.caucho.quercus.env.*;
import com.caucho.quercus.module.AbstractQuercusModule;
import com.caucho.util.LruCache;
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
import java.util.logging.Level;
import java.util.logging.Logger;

public class ResinModule
  extends AbstractQuercusModule
{
  private static final L10N L = new L10N(ResinModule.class);

  private static final Logger log
    = Logger.getLogger(ResinModule.class.getName());
  
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

  private static LruCache<String,SaveState> _saveState;

  /**
   * Converts a string into its binary representation, according to the
   * given encoding, if given, or the script encoding if not given.
   */
  public static Value resin_string_to_binary(Env env, String string, 
					     @Optional String encoding)
  {
    if (encoding == null || encoding.length() == 0)
      encoding = env.getScriptEncoding();

    try {
      byte[] bytes = string.getBytes(encoding);

      return env.createBinaryBuilder(bytes);
    } catch (UnsupportedEncodingException e) {

      env.error(e);

      return BooleanValue.FALSE;
    }
  }

  /**
   * Returns the matchding webbeans.
   */
  public static Object java_bean(String name)
  {
    InjectManager webBeans = InjectManager.create();

    return webBeans.getInstanceByName(name);
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
  public static boolean xa_begin(Env env)
  {
    try {
      getUserTransaction().begin();

      return true;
    } catch (Exception e) {
      log.log(Level.FINE, e.getMessage(), e);
      env.warning(e);
      
      return false;
    }
  }

  /**
   * Commits the current transaction.
   */
  public static boolean xa_commit(Env env)
  {
    try {
      getUserTransaction().commit();

      return true;
    } catch (Exception e) {
      log.log(Level.FINE, e.getMessage(), e);
      env.warning(e);
      
      return false;
    }
  }

  /**
   * Complets the current transaction by rolling it back.
   */
  public static boolean xa_rollback(Env env)
  {
    try {
      getUserTransaction().rollback();

      return true;
    } catch (Exception e) {
      log.log(Level.FINE, e.getMessage(), e);
      env.warning(e);
      
      return false;
    }
  }

  /**
   * Sets the rollback_only status for the current transaction.
   */
  public static boolean xa_rollback_only(Env env)
  {
    try {
      getUserTransaction().setRollbackOnly();

      return true;
    } catch (Exception e) {
      log.log(Level.FINE, e.getMessage(), e);
      env.warning(e);
      
      return false;
    }
  }

  /**
   * Sets the timeout for the current distribued transaction.
   */
  public static boolean xa_set_timeout(Env env, int timeoutSeconds)
  {
    try {
      getUserTransaction().setTransactionTimeout(timeoutSeconds);

      return true;
    } catch (Exception e) {
      log.log(Level.FINE, e.getMessage(), e);
      env.warning(e);
      
      return false;
    }
  }

  /**
   * Returns the JTA status code for the current transation.
   */
  public static int xa_status()
  {
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
   * The domain is stored in the returned array under the key named ":domain:".
   */
  public static ArrayValue mbean_explode(String name)
  {
    try {
      ArrayValueImpl exploded = new ArrayValueImpl();

      if (name == null)
        name = "";
      
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
   * ":domain:" becomes the domain of the object name.
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
  public static Value resin_var_dump(Env env, @ReadOnly Value []args)
  {
    try {
      WriteStream out = Vfs.openWrite("stdout:");
      
      out.setNewlineString("\n");

      if (args != null) {
	for (Value v : args) {
	  if (v != null)
	    v.varDump(env, out, 0, new IdentityHashMap<Value,String>());

	  out.println();
	}
      }

      out.close();

      return NullValue.NULL;
    } catch (IOException e) {
      throw new QuercusModuleException(e);
    }
  }

  /**
   * Restore the current state
   */
  public static boolean resin_restore_state(Env env)
  {
    if (_saveState == null)
      return false;

    SaveState saveState = _saveState.get(env.getSelfPath().getURL());

    if (saveState != null && ! saveState.isModified()) {
      env.restoreState(saveState);
      
      return true;
    }
    else
      return false;
  }

  /**
   * Save the current state
   */
  public static boolean resin_save_state(Env env)
  {
    if (_saveState == null)
      _saveState = new LruCache<String,SaveState>(256);

    SaveState saveState = env.saveState();

    if (saveState != null) {
      _saveState.put(env.getSelfPath().getURL(), saveState);
    
      return true;
    }
    else
      return false;
  }
  
  /**
   * Clears the current state
   */
  public static boolean resin_clear_state(Env env)
  {
    if (_saveState == null)
      return false;
    
    String url = env.getSelfPath().getURL();
    
    SaveState saveState = _saveState.get(url);
    
    if (saveState != null) {
      _saveState.remove(url);
      return true;
    }
    else
      return false;
  }
  
  /**
   * Clears all states.
   */
  public static void resin_clear_states()
  {
    if (_saveState != null)
      _saveState.clear();
  }
}
