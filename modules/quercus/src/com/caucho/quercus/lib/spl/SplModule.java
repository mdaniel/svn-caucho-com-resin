/*
 * Copyright (c) 1998-2007 Caucho Technology -- all rights reserved
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

package com.caucho.quercus.lib.spl;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import com.caucho.quercus.annotation.Optional;
import com.caucho.quercus.env.ArrayValue;
import com.caucho.quercus.env.ArrayValueImpl;
import com.caucho.quercus.env.BooleanValue;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.QuercusClass;
import com.caucho.quercus.env.Value;
import com.caucho.quercus.module.AbstractQuercusModule;
import com.caucho.quercus.program.AbstractFunction;
import com.caucho.util.CharBuffer;

/*
 * XXX: Not finished.
 */
public class SplModule extends AbstractQuercusModule
{
  private static String DEFAULT_EXTENSIONS = ".php,.inc";
  
  public String []getLoadedExtensions()
  {
    return new String[] { "SPL" };
  }
  
  public static boolean spl_autoload_register(Env env,
                                              @Optional String fun)
  {
    if (fun == null || fun.length() == 0)
      fun = "spl_autoload";
    
    env.addAutoloadFunction(fun);
    
    return true;
  }
  
  public static boolean spl_autoload_unregister(Env env,
                                                String fun)
  {
    env.removeAutoloadFunction(fun);
    
    return true;
  }
  
  public static Value spl_autoload_functions(Env env)
  {
    LinkedHashMap<String, AbstractFunction> funMap
      = env.getAutoloadFunctions();
    
    if (funMap == null)
      return BooleanValue.FALSE;
    
    ArrayValue array = new ArrayValueImpl();
    
    for (Map.Entry<String, AbstractFunction> entry : funMap.entrySet()) {
      array.put(entry.getKey());
    }
    
    return array;
  }
  
  public static String spl_autoload_extensions(Env env,
                                               @Optional String extensions)
  {
    String oldExtensions = getAutoloadExtensions(env);
    
    if (extensions != null)
      env.setSpecialValue("caucho.spl_autoload", extensions);
    
    return oldExtensions;
  }
  
  private static String getAutoloadExtensions(Env env)
  {
    Object obj = env.getSpecialValue("caucho.spl_autoload");
    
    if (obj == null)
      return DEFAULT_EXTENSIONS;
    else
      return (String) obj;
  }
  
  public static void spl_autoload(Env env,
                                  String className,
                                  @Optional String extensions)
  {
    if (env.findClass(className, false) != null)
      return;
    
    ArrayList<String> extensionList = new ArrayList<String>();
    
    if (extensions == null) {
      extensions = getAutoloadExtensions(env);
    }
    
    if (extensions == DEFAULT_EXTENSIONS) {
      extensionList.add(".php");
      extensionList.add(".inc");
    }
    else {
      int len = extensions.length();
      
      CharBuffer cb = CharBuffer.allocate();
      for (int i = 0; i < len; i++) {
        char ch = extensions.charAt(i);
        
        if (ch == ',')
          extensionList.add(cb.toString());
          cb.clear();
      }
      
      if (cb.length() > 0)
        extensionList.add(cb.toString());
      
      cb.free();
    }
    
    String filePrefix = className.toLowerCase();
    
    for (String ext : extensionList) {
      String filename = filePrefix + ext;
      
      env.include(filename);
      
      QuercusClass cls = env.findClass(className, false);
      
      if (cls != null)
        return;
    }
  }
}
