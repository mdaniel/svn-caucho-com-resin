/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
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

package com.caucho.quercus.env;

import com.caucho.java.LineMap;
import com.caucho.java.ScriptStackTrace;
import com.caucho.java.WorkDir;
import com.caucho.loader.SimpleLoader;
import com.caucho.quercus.*;
import com.caucho.quercus.expr.Expr;
import com.caucho.quercus.lib.ErrorModule;
import com.caucho.quercus.lib.VariableModule;
import com.caucho.quercus.lib.OptionsModule;
import com.caucho.quercus.lib.file.FileModule;
import com.caucho.quercus.lib.file.PhpStderr;
import com.caucho.quercus.lib.file.PhpStdin;
import com.caucho.quercus.lib.file.PhpStdout;
import com.caucho.quercus.lib.regexp.RegexpState;
import com.caucho.quercus.lib.string.StringModule;
import com.caucho.quercus.lib.string.StringUtility;
import com.caucho.quercus.module.ModuleContext;
import com.caucho.quercus.module.ModuleStartupListener;
import com.caucho.quercus.module.IniDefinition;
import com.caucho.quercus.page.QuercusPage;
import com.caucho.quercus.program.*;
import com.caucho.quercus.function.AbstractFunction;
import com.caucho.quercus.resources.StreamContextResource;
import com.caucho.util.*;
import com.caucho.vfs.ByteToChar;
import com.caucho.vfs.Encoding;
import com.caucho.vfs.MemoryPath;
import com.caucho.vfs.NullPath;
import com.caucho.vfs.Path;
import com.caucho.vfs.ReadStream;
import com.caucho.vfs.WriteStream;
import com.caucho.vfs.TempBuffer;
import com.caucho.vfs.i18n.EncodingReader;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.servlet.ServletContext;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.IdentityHashMap;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Represents the Quercus environment.
 */
public class ProGoogleEnv extends GoogleEnv {

  public ProGoogleEnv(QuercusContext quercus,
		   QuercusPage page,
		   WriteStream out,
		   HttpServletRequest request,
		   HttpServletResponse response)
  {
    super(quercus, page, out, request, response);
  }

  public ProGoogleEnv(QuercusContext quercus)
  {
    super(quercus);
  }

  /**
   * Returns the current execution location.
   *
   * Use with care, for compiled code this can be a relatively expensive
   * operation.
   */
  public Location getLocation()
  {
    Expr call = peekCall(0);

    if (call != null)
      return call.getLocation();
    else {
      Exception e = new Exception();
      e.fillInStackTrace();

      StackTraceElement []trace = e.getStackTrace();

      ClassLoader loader = SimpleLoader.create(WorkDir.getLocalWorkDir());

      for (int i = 0; i < trace.length; i++) {
        String className = trace[i].getClassName();

        if (className.startsWith("_quercus")) {
          LineMap lineMap = ScriptStackTrace.getScriptLineMap(className,
                                                              loader);

          LineMap.Line line = null;

          if (lineMap != null)
            line = lineMap.getLine(trace[i].getLineNumber());

          if (line != null) {
            int sourceLine = line.getSourceLine(trace[i].getLineNumber());

            // XXX: need className and functionName info
            return new Location(line.getSourceFilename(), sourceLine, null, null);
          }
        }
      }
    }

    return Location.UNKNOWN;
  }

  @Override
  public int getSourceLine(String className, int javaLine)
  {
    if (className.startsWith("_quercus")) {
      ClassLoader loader = getQuercus().getCompileClassLoader();
      
      LineMap lineMap
        = ScriptStackTrace.getScriptLineMap(className, loader);

      LineMap.Line line = null;

      if (lineMap != null)
        line = lineMap.getLine(javaLine);

      if (line != null) {
        return line.getSourceLine(javaLine);
      }
    }

    return javaLine;
  }
}
