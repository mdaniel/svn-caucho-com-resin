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
 * @author Scott Ferguson
 */

package com.caucho.quercus.lib;

import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;

import com.caucho.util.L10N;

import com.caucho.quercus.Quercus;

import com.caucho.quercus.module.AbstractQuercusModule;
import com.caucho.quercus.module.Optional;

import com.caucho.quercus.program.PhpProgram;

import com.caucho.quercus.env.*;

/**
 * PHP options
 */
public class OptionsModule extends AbstractQuercusModule {
  private static final L10N L = new L10N(OptionsModule.class);

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
  public static Value get_loaded_extensions(Env env)
  {
    ArrayValue value = new ArrayValueImpl();

    for (String ext : env.getLoadedExtensions()) {
      value.put(ext);
    }

    return value;
  }

  /**
   * Stubs the dl.
   */
  public static boolean dl(Env env, String dl)
  {
    env.stub("dl is stubbed for dl(" + dl + ")");
    
    return false;
  }
  
  /**
   * Returns the environment value.
   */
  public static Value getenv(Env env, String var)
  {
    if ("REMOTE_ADDR".equals(var))
      return new StringValueImpl(env.getRequest().getRemoteAddr());

    return NullValue.NULL;
  }

  /**
   * Returns the configuration value of a configuration.
   */
  public static Value get_cfg_var(Env env, String name)
  {
    Value value = env.getConfigVar(name);

    if (value != null)
      return value;
    else
      return NullValue.NULL;
  }

  /**
   * Returns the constants as an array
   */
  public static Value get_defined_constants(Env env)
  {
    return env.getDefinedConstants();
  }

  /**
   * Returns the include path
   */
  public static Value get_include_path(Env env)
  {
    Value value = env.getIni("include_path");

    if (value != null)
      return value;
    else
      return StringValue.EMPTY;
  }

  /**
   * Returns extension function with a given name.
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
    return LongValue.create(env.getIni("magic_quotes_gpc").toLong());
  }

  /**
   * Gets the magic quotes runtime value.
   */
  public static Value get_magic_quotes_runtime(Env env)
  {
    return LongValue.ZERO;
  }

  /**
   * Returns the gid for the script path.
   */
  public static Value getlastmod(Env env)
  {
    return FileModule.filemtime(env, env.getSelfPath());
  }

  /**
   * Returns the gid for the script path.
   */
  public static Value getmygid(Env env)
  {
    return FileModule.filegroup(env, env.getSelfPath());
  }

  /**
   * Returns the inode for the script path.
   */
  public static Value getmyinode(Env env)
  {
    return FileModule.fileinode(env, env.getSelfPath());
  }

  /**
   * Returns the uid for the script path.
   */
  public static Value getmyuid(Env env)
  {
    return QuercusFileModule.fileowner(env, env.getSelfPath());
  }

  /**
   * Returns the thread for the script.
   */
  public static long getmypid(Env env)
  {
    return Thread.currentThread().getId();
  }


  /**
   * Sets an initialization value.
   */
  public static Value ini_alter(Env env, String varName, String value)
  {
    return ini_set(env, varName, value);
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
   * Restore the initial configuration value
   */
  public static Value ini_restore(Env env, String name)
  {
    Value value = env.getConfigVar(name);

    if (value != null)
      env.setIni(name, value.toString());

    return NullValue.NULL;
  }

  /**
   * Sets an initialization value.
   */
  public static Value ini_set(Env env, String varName, String value)
  {
    Value oldValue = env.setIni(varName, value);

    if (oldValue != null)
      return oldValue;
    else
      return NullValue.NULL;
  }

  /**
   * Gets the magic quotes value.
   */
  public static Value magic_quotes_runtime(Env env)
  {
    return env.getIni("magic_quotes_runtime");
  }

  /**
   * Returns the sapi type.
   */
  public static String php_sapi_name()
  {
    return "apache";
  }

  /**
   * Returns system information
   */
  public static String php_uname(@Optional("'a'") String mode)
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
      return (php_uname("s") + " " +
              php_uname("n") + " " +
              php_uname("r") + " " +
              php_uname("v") + " " +
              php_uname("m"));
    }
  }

  /**
   * Returns the quercus version.
   */
  public static String phpversion(@Optional String module)
  {
    return "5.0.0";
  }

  /**
   * Sets the include path
   */
  public static String set_include_path(Env env, String includePath)
  {
    return env.setIncludePath(includePath);
  }

  /**
   * Sets the include path
   */
  public static Value restore_include_path(Env env)
  {
    env.restoreIncludePath();

    return NullValue.NULL;
  }

  /**
   * Sets the magic quotes value.
   */
  public static Value set_magic_quotes_runtime(Env env, Value value)
  {
    if (value.toBoolean())
      throw new UnsupportedOperationException("magic_quotes_runtime");

    return BooleanValue.FALSE;
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
                                      @Optional("cmp") String op)
  {
    ArrayList<Value> expanded1 = expandVersion(version1);
    ArrayList<Value> expanded2 = expandVersion(version2);

    int cmp = compareTo(expanded1, expanded2);

    if ("eq".equals(op) || "==".equals(op) || "=".equals(op))
      return cmp == 0 ? BooleanValue.TRUE : BooleanValue.FALSE;
    else if ("ne".equals(op) || "!=".equals(op) || "<>".equals(op))
      return cmp != 0 ? BooleanValue.TRUE : BooleanValue.FALSE;
    else if ("lt".equals(op) || "<".equals(op))
      return cmp < 0 ? BooleanValue.TRUE : BooleanValue.FALSE;
    else if ("le".equals(op) || "<=".equals(op))
      return cmp <= 0 ? BooleanValue.TRUE : BooleanValue.FALSE;
    else if ("gt".equals(op) || ">".equals(op))
      return cmp > 0 ? BooleanValue.TRUE : BooleanValue.FALSE;
    else if ("ge".equals(op) || ">=".equals(op))
      return cmp >= 0 ? BooleanValue.TRUE : BooleanValue.FALSE;
    else {
      if (cmp == 0)
	return new LongValue(0);
      else if (cmp < 0)
	return new LongValue(-1);
      else
	return new LongValue(1);
    }
  }

  private static ArrayList<Value> expandVersion(String version)
  {
    ArrayList<Value> expand = new ArrayList<Value>();

    int len = version.length();
    int i = 0;

    while (i < len) {
      char ch = version.charAt(i);

      if ('0' <= ch && ch <= '9') {
	int value = 0;

	for (; i < len && '0' <= (ch = version.charAt(i)) && ch <= '9'; i++) {
	  value = 10 * value + ch - '0';
	}

	expand.add(new LongValue(value));
      }
      else if (Character.isLetter((char) ch)) {
	StringBuilder sb = new StringBuilder();

	for (; i < len && Character.isLetter(version.charAt(i)); i++) {
	  sb.append((char) ch);
	}

	String s = sb.toString();

	if (s.equals("dev"))
	  s = "a";
	else if (s.equals("alpha") || s.equals("a"))
	  s = "b";
	else if (s.equals("beta") || s.equals("b"))
	  s = "c";
	else if (s.equals("RC"))
	  s = "d";
	else if (s.equals("pl"))
	  s = "e";
	else
	  s = "z" + s;

	expand.add(new StringValueImpl(s));
      }
      else
	i++;
    }

    return expand;
  }

  private static int compareTo(ArrayList<Value> a, ArrayList<Value> b)
  {
    int i = 0;

    while (true) {
      if (a.size() <= i && b.size() <= i)
	return 0;
      else if (a.size() <= i)
	return -1;
      else if (b.size() <= i)
	return 1;

      int cmp = compareTo(a.get(i), b.get(i));

      if (cmp != 0)
	return cmp;

      i++;
    }
  }

  private static int compareTo(Value a, Value b)
  {
    if (a.equals(b))
      return 0;
    else if (a.isLong() && ! b.isLong())
      return -1;
    else if (b.isLong() && ! a.isLong())
      return 1;
    else if (a.lt(b))
      return -1;
    else
      return 1;
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
    //    addIni(_iniMap, "magic_quotes_gpc", "1", PHP_INI_PERDIR);
    addIni(_iniMap, "magic_quotes_gpc", "0", PHP_INI_PERDIR);
    addIni(_iniMap, "magic_quotes_runtime", "0", PHP_INI_ALL);
  }

  //@todo mixed   assert_options(int what [, mixed value])
  //@todo boolean assert(mixed assertion)
  //@todo int     dl(string library)
  //@todo boolean extension_loaded(string name)
  //@todo string  get_cfg_var(string varname)
  //@todo string  get_current_user()
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

