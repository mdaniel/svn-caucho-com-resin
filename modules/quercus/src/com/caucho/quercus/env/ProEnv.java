/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.quercus.env;

import com.caucho.java.LineMap;
import com.caucho.java.ScriptStackTrace;
import com.caucho.java.WorkDir;
import com.caucho.loader.SimpleLoader;
import com.caucho.quercus.Location;
import com.caucho.quercus.ProQuercus;
import com.caucho.quercus.QuercusContext;
import com.caucho.quercus.expr.Expr;
import com.caucho.quercus.page.QuercusPage;
import com.caucho.util.L10N;
import com.caucho.util.Log;
import com.caucho.vfs.WriteStream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Represents the Quercus environment.
 */
public class ProEnv extends Env {
  private static final L10N L = new L10N(ProEnv.class);
  private static final Logger log
    = Logger.getLogger(ProEnv.class.getName());

  public ProEnv(QuercusContext quercus,
                QuercusPage page,
                WriteStream out,
                HttpServletRequest request,
                HttpServletResponse response)
  {
    super(quercus, page, out, request, response);
  }

  public ProEnv(QuercusContext quercus)
  {
    super(quercus);
  }

  /**
   * Returns the current execution location.
   *
   * Use with care, for compiled code this can be a relatively expensive
   * operation.
   */
  @Override
  public Location getLocation()
  {
    Location location = getLocationImpl();

    if (location != null)
      return location;

    // php/0d92
    Expr call = peekCall(0);

    if (call != null)
      return call.getLocation();
    else {
      Exception e = new Exception();
      e.fillInStackTrace();

      StackTraceElement []trace = e.getStackTrace();

      // ClassLoader loader = createWorkLoader();
      ClassLoader loader = getQuercus().getCompileClassLoader();

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

