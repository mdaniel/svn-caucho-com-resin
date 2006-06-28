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

package com.caucho.quercus.lib.i18n;

import java.util.HashMap;
import java.util.Map;

import com.caucho.quercus.env.ArrayValue;
import com.caucho.quercus.env.ArrayValueImpl;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.StringValue;
import com.caucho.quercus.env.StringValueImpl;

import com.caucho.quercus.module.AbstractQuercusModule;
import com.caucho.quercus.module.Optional;

/**
 * Unicode handling.  Also includes iconv, etc.
 */
public class UnicodeModule extends AbstractQuercusModule {
  private static final HashMap<String,StringValue> _iniMap
    = new HashMap<String,StringValue>();

  public static final int U_INVALID_STOP = 0;
  public static final int U_INVALID_SKIP = 1;
  public static final int U_INVALID_SUBSTITUTE = 2;
  public static final int U_INVALID_ESCAPE = 3;

  /**
   * Returns the current encoding.
   */
  public static ArrayValue iconv_get_encoding(Env env)
  {
    ArrayValueImpl result = new ArrayValueImpl();

    result.put(new StringValueImpl("input_encoding"),
	       env.getHttpInputEncoding());
    result.put(new StringValueImpl("output_encoding"),
	       env.getOutputEncoding());
    result.put(new StringValueImpl("runtime_encoding"),
	       env.getRuntimeEncoding());

    return result;
  }

  /**
   * Sets the current encoding.
   */
  public static boolean iconv_set_encoding(Env env,
					   String type,
					   StringValue charset)
  {
    if ("input_encoding".equals(type)) {
      env.setIni("unicode.http_input_encoding", charset);
      
      return true;
    }
    else if ("output_encoding".equals(type)) {
      env.setIni("unicode.output_encoding", charset);
      
      return true;
    }
    else if ("internal_encoding".equals(type)) {
      env.setIni("unicode.runtimet_encoding", charset);
      
      return true;
    }
    else
      return false;
  }

  /**
   * Returns the default quercus.ini values.
   */
  public Map<String,StringValue> getDefaultIni()
  {
    return _iniMap;
  }

  static {
    addIni(_iniMap, "unicode.fallback_encoding", null, PHP_INI_ALL);
    addIni(_iniMap, "unicode.from_error_mode", "2", PHP_INI_ALL);
    addIni(_iniMap, "unicode.from_error_subst_char", "3f", PHP_INI_ALL);
    addIni(_iniMap, "unicode.http_input_encoding", null, PHP_INI_ALL);
    addIni(_iniMap, "unicode.output_encoding", null, PHP_INI_ALL);
    addIni(_iniMap, "unicode.runtime_encoding", null, PHP_INI_ALL);
    addIni(_iniMap, "unicode.script_encoding", null, PHP_INI_ALL);
    addIni(_iniMap, "unicode.semantics", "on", PHP_INI_ALL);
  }
}

