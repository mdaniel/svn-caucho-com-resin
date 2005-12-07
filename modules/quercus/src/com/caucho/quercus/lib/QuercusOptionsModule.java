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

import java.util.Map;
import java.util.HashMap;

import com.caucho.util.L10N;

import com.caucho.quercus.Quercus;

import com.caucho.quercus.module.AbstractQuercusModule;
import com.caucho.quercus.module.Optional;

import com.caucho.quercus.program.PhpProgram;

import com.caucho.quercus.env.*;

/**
 * PHP options
 */
public class QuercusOptionsModule extends AbstractQuercusModule {
  private static final L10N L = new L10N(QuercusOptionsModule.class);

  // XXX: get real value
  public static final String PHP_OS = "Linux";

  private static final HashMap<String,StringValue> _iniMap
    = new HashMap<String,StringValue>();

  /**
   * Returns the default quercus.ini values.
   */
  public Map<String,StringValue> getDefaultIni()
  {
    return _iniMap;
  }

  /**
   * Checks the assertion
   */
  public static boolean quercus_assert(Env env, String code)
    throws Throwable
  {
    Quercus quercus = env.getPhp();

    PhpProgram program = quercus.parseCode(code);

    Value value = program.execute(env);

    return value.toBoolean();
  }

  /**
   * Returns true if the given extension is loaded
   */
  public static boolean extension_loaded(Env env, String name)
  {
    return env.isExtensionLoaded(name);
  }

  /**
   * Returns the environment value.
   */
  public static Value getenv(Env env, String var)
  {
    if ("REMOTE_ADDR".equals(var))
      return new StringValue(env.getRequest().getRemoteAddr());

    return NullValue.NULL;
  }

  /**
   * Returns extension function swith a given name
   */
  public static Value get_extension_funcs(Env env, String name)
  {
    return env.getExtensionFuncs(name);
  }

  /**
   * Gets the magic quotes value.
   */
  public static Value get_magic_quotes_gpc(Env env)
  {
    // XXX: stub

    return env.getOption("magic_quotes_gpc");
  }

  /**
   * Returns an initialization value.
   */
  public static String ini_get(Env env, String varName)
  {
    StringValue v = env.getIni(varName);

    if (v != null)
      return v.toString();
    else
      return null;
  }

  /**
   * Sets an initialization value.
   */
  public static Value ini_set(Env env, String varName, String value)
  {
    return env.setIni(varName, value);
  }

  /**
   * Gets the magic quotes value.
   */
  public static Value magic_quotes_runtime(Env env)
  {
    return env.getOption("magic_quotes_runtime");
  }

  /**
   * Returns the sapi type.
   */
  public static String quercus_php_sapi_name()
  {
    return "apache";
  }

  /**
   * Returns system information
   */
  public static String quercus_php_uname(@Optional("'a'") String mode)
  {
    // XXX: stubbed

    if (mode == null || mode.equals(""))
      mode = "a";

    switch (mode.charAt(0)) {
    case 's':
      return PHP_OS;

    case 'n':
      return "localhost";

    case 'r':
      return "2.4.0";

    case 'v':
      return "Version 2.4.0";

    case 'm':
      return "i386";

    case 'a':
    default:
      return (quercus_php_uname("s") + " " +
              quercus_php_uname("n") + " " +
              quercus_php_uname("r") + " " +
              quercus_php_uname("v") + " " +
              quercus_php_uname("m"));
    }
  }

  /**
   * Returns the quercus version.
   */
  public static String quercusversion(@Optional String module)
  {
    return "5.0.0";
  }

  /**
   * Sets the magic quotes value.
   */
  public static Value set_magic_quotes_runtime(Env env, Value value)
  {
    env.setOption("magic_quotes_runtime", value);

    return value;
  }

  /**
   * Sets the time limit
   */
  public static Value set_time_limit(Env env, long seconds)
  {
    env.setTimeLimit(seconds * 1000L);

    return NullValue.NULL;
  }

  /**
   * Compares versions
   */
  public static Value version_compare(String version1,
                                      String version2,
                                      @Optional String operator)
  {
    // XXX: incomplete

    int cmp = version1.compareTo(version2);

    if (cmp == 0)
      return new LongValue(0);
    else if (cmp < 0)
      return new LongValue(-1);
    else
      return new LongValue(1);
  }

  static {
    addIni(_iniMap, "assert.active", "1", PHP_INI_ALL);
    addIni(_iniMap, "assert.bail", "0", PHP_INI_ALL);
    addIni(_iniMap, "assert.warning", "1", PHP_INI_ALL);
    addIni(_iniMap, "assert.callback", null, PHP_INI_ALL);
    addIni(_iniMap, "assert.quiet_eval", "0", PHP_INI_ALL);
    addIni(_iniMap, "enable_dl", "1", PHP_INI_SYSTEM);
    addIni(_iniMap, "max_execution_time", "30", PHP_INI_ALL);
    addIni(_iniMap, "max_input_time", "-1", PHP_INI_PERDIR);
    addIni(_iniMap, "magic_quotes_gpc", "1", PHP_INI_PERDIR);
    addIni(_iniMap, "magic_quotes_runtime", "0", PHP_INI_ALL);
  }

  //@todo mixed   assert_options(int what [, mixed value])
  //@todo boolean assert(mixed assertion)
  //@todo int     dl(string library)
  //@todo boolean extension_loaded(string name)
  //@todo string  get_cfg_var(string varname)
  //@todo string  get_current_user()
  //@todo array   get_defined_constants([mixed categorize])
  //@todo array   get_extension_funcs(string module_name)
  //@todo string  get_include_path()
  //@todo array   get_included_files()
  //@todo array   get_loaded_extensions()
  //@todo int     get_magic_quotes_runtime()
  //@todo array   get_required_files() ALIAS of get_included_files
  //@todo int     getlastmod()
  //@todo int     getmygid()
  //@todo int     getmyinode()
  //@todo int     getmypid()
  //@todo int     getmyuid()
  //@todo array   getopt(string options [,array longopts])
  //@todo array   getrusage([int who])
  //@todo string  ini_alter(string varname, string newvalue) ALIAS of ini_set
  //@todo array   ini_get_all([string extension])
  //@todo void    ini_restore(string varname)
  //XXX main is Dummy for main()
  //@todo int     memory_get_usage()
  //@todo string  quercus_ini_scanned_files()
  //@todo string  quercus_logo_guid()
  //@todo string  quercus_uname([string mode])
  //@todo boolean quercuscredits([int flag])
  //@todo boolean quercusinfo([int what])
  //@todo boolean pupenv(string setting)
  //@todo void    restore_include_path()
  //@todo string  set_include_path(string new_include_path)
  //@todo string  zend_logo_guid()
  //@todo string  zend_version()


}

