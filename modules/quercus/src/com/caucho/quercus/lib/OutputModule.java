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

import com.caucho.quercus.annotation.Optional;
import com.caucho.quercus.env.*;
import com.caucho.quercus.module.AbstractQuercusModule;
import com.caucho.quercus.module.ModuleStartupListener;
import com.caucho.util.L10N;
import com.caucho.vfs.StreamImplOutputStream;
import com.caucho.vfs.TempStream;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPOutputStream;

/**
 * PHP output routines.
 */
public class OutputModule extends AbstractQuercusModule 
  implements ModuleStartupListener {
  private static final L10N L = new L10N(OutputModule.class);
  private static final Logger log
    = Logger.getLogger(OutputModule.class.getName());
  private static final StringValue HTTP_ACCEPT_ENCODING 
    = new StringValueImpl("HTTP_ACCEPT_ENCODING");

  private static final HashMap<String,StringValue> _iniMap
    = new HashMap<String,StringValue>();

  // ob_gzhandler related variables/types
  private enum Encoding {NONE, GZIP, DEFLATE};

  private static class GZOutputPair {
    public TempStream tempStream;
    public OutputStream outputStream;
  }

  private static HashMap<Env,GZOutputPair> _gzOutputPairs 
    = new HashMap<Env,GZOutputPair>();


  /**
   * Returns the default php.ini values.
   */
  public Map<String,StringValue> getDefaultIni()
  {
    return _iniMap;
  }

  public void startup(Env env)
  {
    String handlerName = env.getConfigVar("output_handler").toString();

    if (! "".equals(handlerName) && env.getFunction(handlerName) != null) {
      Callback callback = env.createCallback(new StringValueImpl(handlerName));

      ob_start(env, callback, 0, true);
    } else if (env.getConfigVar("output_buffering").toBoolean()) {
      ob_start(env, null, 0, true);
    }

    ob_implicit_flush(env, env.getConfigVar("output_buffering").toBoolean());
  }

  /**
   * Flushes the original output buffer.
   */
  public Value flush(Env env)
  {
    try {
      // XXX: conflicts with dragonflycms install
      env.getOriginalOut().flushBuffer();
    } catch (IOException e) {
    }

    return NullValue.NULL;
  }

  /**
   * Clears the output buffer.
   */
  public static Value ob_clean(Env env)
  {
    OutputBuffer ob = env.getOutputBuffer();

    if (ob != null) {
      ob.clean();

      return BooleanValue.TRUE;
    }
    else
      return BooleanValue.FALSE;
  }

  /**
   * Pops the output buffer, discarding the contents.
   */
  public static boolean ob_end_clean(Env env)
  {
    OutputBuffer ob = env.getOutputBuffer();

    if (ob != null) {
      ob.clean();

      Callback callback = ob.getCallback();

      if (callback != null) {
        if (callback.getCallbackName().equals("ob_gzhandler"))
          _gzOutputPairs.remove(env);

        ob.setCallback(null);
      }
    }

    return env.popOutputBuffer();
  }

  /**
   * Pops the output buffer.
   */
  public static boolean ob_end_flush(Env env)
  {
    return env.popOutputBuffer();
  }

  /**
   * Returns the contents of the output buffer, emptying it afterwards.
   */
  public static Value ob_get_clean(Env env)
  {
    OutputBuffer ob = env.getOutputBuffer();

    if (ob != null) {
      Value result = ob.getContents();

      ob.clean();

      return result;
    }
    else
      return BooleanValue.FALSE;
  }

  /**
   * Returns the contents of the current output buffer.
   */
  public static Value ob_get_contents(Env env)
  {
    OutputBuffer ob = env.getOutputBuffer();

    if (ob != null)
      return ob.getContents();
    else
      return BooleanValue.FALSE;
  }

  /**
   * Pops the output buffer and returns the contents.
   */
  public static Value ob_get_flush(Env env)
  {
    OutputBuffer ob = env.getOutputBuffer();

    Value result = BooleanValue.FALSE;
    if (ob != null) {
      result = ob.getContents();
    }

    env.popOutputBuffer();

    return result;
  }

  /**
   * Flushes this output buffer into the next one on the stack or
   * to the default "output buffer" if no next output buffer exists.
   * The callback associated with this buffer is also called with
   * appropriate parameters.
   */
  public static Value ob_flush(Env env)
  {
    OutputBuffer ob = env.getOutputBuffer();

    if (ob != null) {
      ob.flush();

      return BooleanValue.TRUE;
    } 
    else
      return BooleanValue.FALSE;
  }

  /**
   * Pushes the output buffer
   */
  public static Value ob_get_length(Env env)
  {
    OutputBuffer ob = env.getOutputBuffer();

    if (ob != null)
      return LongValue.create(ob.getLength());
    else
      return BooleanValue.FALSE;
  }

  /**
   * Gets the nesting level of the current output buffer
   */
  public static Value ob_get_level(Env env)
  {
    OutputBuffer ob = env.getOutputBuffer();

    if (ob != null)
      return LongValue.create(ob.getLevel());
    else
      return LongValue.ZERO;
  }

  /**
   * Helper recursive function that ensures the handlers are listed
   * in the correct order in the array.
   */
  private static void listHandlers(OutputBuffer ob, ArrayValue handlers)
  {
    if (ob == null)
      return;

    listHandlers(ob.getNext(), handlers);

    Callback callback = ob.getCallback();

    if (callback != null) 
      handlers.put(new StringValueImpl(callback.getCallbackName()));
    else
      handlers.put(new StringValueImpl("default output handler"));
  }
  
  /**
   * Returns a list of all the output handlers in use.
   */
  public static Value ob_list_handlers(Env env)
  {
    OutputBuffer ob = env.getOutputBuffer();
    ArrayValue handlers = new ArrayValueImpl();

    listHandlers(ob, handlers);

    return handlers;
  }

  /**
   * Inserts the common values for ob_get_status into an array.  Used
   * by getFullStatus() and ob_get_status().
   */
  private static void putCommonStatus(ArrayValue element, OutputBuffer ob,
                                      Env env, boolean fullStatus)
  {
    LongValue type = LongValue.ONE;
    Callback callback = ob.getCallback();

    if (callback != null && callback.isInternal())
      type = LongValue.ZERO;

    element.put(new StringValueImpl("type"), type);

    // the rewriter is a special case where it includes a field
    // "buffer_size" right in the middle of the common elements, 
    // but only when called with full status.  It appears always 
    // to be 0 and there is no interface to change this buffer_size
    // and no indication of its meaning.
    if (fullStatus && callback != null &&
        callback == UrlRewriterCallback.getInstance(env))
      element.put(new StringValueImpl("buffer_size"), LongValue.ZERO);

    // Technically, there are supposed to be three possible values
    // for status: 
    //   0 if the stream has never been flushed (PHP_OUTPUT_HANDLER_START)
    //   1 if the stream has been flushed (PHP_OUTPUT_HANDLER_CONT)
    //   2 if the stream was flushed at the end (PHP_OUTPUT_HANDLER_END)
    // However, there is no way to access the buffer after it has ended, 
    // so the final case doesn't seem to be an issue!  (Even calling
    // ob_get_status() in the handler on a ob_end_flush() does not
    // invoke this state.)
    LongValue status = ob.haveFlushed() ? LongValue.ONE : LongValue.ZERO;
    element.put(new StringValueImpl("status"), status);

    StringValue name;

    if (callback != null)
      name = new StringValueImpl(callback.getCallbackName());
    else
      name = new StringValueImpl("default output handler".intern());

    element.put(new StringValueImpl("name".intern()), name);

    Value del = ob.getEraseFlag() ? BooleanValue.TRUE : 
      BooleanValue.FALSE;
    element.put(new StringValueImpl("del"), del);
  }

  /**
   * Gets the status for all the output buffers on the stack.
   * Recursion ensures the results are ordered correctly in the array.
   */
  private static void getFullStatus(OutputBuffer ob, Env env, ArrayValue result)
  {
    if (ob == null)
      return;

    getFullStatus(ob.getNext(), env, result);

    ArrayValue element = new ArrayValueImpl();

    element.put(new StringValueImpl("chunk_size"), 
                LongValue.create(ob.getChunkSize()));
    
    // XXX: Not sure why we even need to list a size -- PHP doesn't 
    // even seem to respect it.  -1 => infinity?  
    // (Note: "size" == "capacity")
    element.put(new StringValueImpl("size"), LongValue.create(-1));
    element.put(new StringValueImpl("block_size"), LongValue.create(-1));

    putCommonStatus(element, ob, env, true);
   
    result.put(element);
  }

  /**
   * Gets the status of the current output buffer(s)
   */ 
  public static Value ob_get_status(Env env, @Optional boolean full_status)
  {
    if (full_status) {
      OutputBuffer ob = env.getOutputBuffer();
      ArrayValue result = new ArrayValueImpl();

      getFullStatus(ob, env, result);

      return result;
    }

    OutputBuffer ob = env.getOutputBuffer();
    ArrayValue result = new ArrayValueImpl();

    if (ob != null) {
      result.put(new StringValueImpl("level"), 
                 LongValue.create(ob.getLevel()));

      putCommonStatus(result, ob, env, false);
    }

    // returns an empty array when no output buffer exists
    return result;
  }

  /**
   * Makes the original "output buffer" flush on every write.
   */
  public static Value ob_implicit_flush(Env env, @Optional("true") boolean flag)
  {
    if (env.getOriginalOut() != null)
      env.getOriginalOut().setImplicitFlush(flag);

    return NullValue.NULL;
  }

  /**
   * Pushes the output buffer
   */
  public static boolean ob_start(Env env,
                                 @Optional Callback callback,
                                 @Optional int chunkSize,
                                 @Optional("true") boolean erase)
  {
    if (callback != null && callback.getCallbackName().equals("ob_gzhandler")) {
      OutputBuffer ob = env.getOutputBuffer();

      for (; ob != null; ob = ob.getNext()) {
        Callback cb = ob.getCallback();

        if (cb.getCallbackName().equals("ob_gzhandler")) {
          env.warning(L.l("output handler 'ob_gzhandler' cannot be used twice"));
          return false;
        }
      }
    }
    
    env.pushOutputBuffer(callback, chunkSize, erase);

    return true;
  }

  /**
   * Pushes a new UrlRewriter callback onto the output buffer stack
   * if one does not already exist.
   */
  public static UrlRewriterCallback pushUrlRewriter(Env env)
  {
    UrlRewriterCallback rewriter = UrlRewriterCallback.getInstance(env);

    if (rewriter == null) {
      OutputBuffer ob = env.getOutputBuffer();
      rewriter = new UrlRewriterCallback(env);

      // PHP installs the URL rewriter into the top output buffer if
      // its callback is null
      if (ob != null && ob.getCallback() == null)
        ob.setCallback(rewriter);
      else 
        ob_start(env, rewriter, 0, true);
    }

    return rewriter;
  }

  /**
   * Adds a variable to the list for rewritten URLs.
   */
  public static boolean output_add_rewrite_var(Env env, 
                                               String name, String value)
  {
    UrlRewriterCallback rewriter = pushUrlRewriter(env);
   
    rewriter.addRewriterVar(name, value);

    return true;
  }

  /**
   * Clears the list of variables for rewritten URLs.
   */
  public static boolean output_reset_rewrite_vars(Env env)
  {
    UrlRewriterCallback rewriter = UrlRewriterCallback.getInstance(env); 

    rewriter.resetRewriterVars();

    return true;
  }

  /**
   * Output buffering compatible callback that automatically compresses
   * the output.  The output of this function depends on the value of 
   * state.  Specifically, if the PHP_OUTPUT_HANDLER_START bit is on
   * in the state field, the function supplies a header with the output
   * and initializes a gzip/deflate stream which will be used for 
   * subsequent calls.
   */
  public static Value ob_gzhandler(Env env, StringValue buffer, int state)
  {
    Encoding encoding = Encoding.NONE;
    Value _SERVER = env.getSpecialRef("_SERVER");

    String [] acceptedList = 
      _SERVER.get(HTTP_ACCEPT_ENCODING).toString().split(",");

    for (String accepted : acceptedList) {
      accepted = accepted.trim();

      if (accepted.equalsIgnoreCase("gzip")) {
        encoding = Encoding.GZIP;
        break;
      } else if (accepted.equalsIgnoreCase("deflate")) {
        encoding = Encoding.DEFLATE;
        break;
      }
    }

    if (encoding == Encoding.NONE)
      return NullValue.NULL;

    GZOutputPair pair = null;

    if ((state & (1 << OutputBuffer.PHP_OUTPUT_HANDLER_START)) != 0) {
      HttpModule.header(env, "Vary: Accept-Encoding", true, 0);

      int encodingFlag = 0;

      pair = new GZOutputPair();
      pair.tempStream = new TempStream();
      StreamImplOutputStream siout = 
        new StreamImplOutputStream(pair.tempStream);

      try {
        if (encoding == Encoding.GZIP) {
          HttpModule.header(env, "Content-Encoding: gzip", true, 0);

          pair.outputStream = new GZIPOutputStream(siout);
        } else if (encoding == Encoding.DEFLATE) {
          HttpModule.header(env, "Content-Encoding: deflate", true, 0);

          pair.outputStream = new DeflaterOutputStream(siout);
        }
      } catch (IOException e) {
        return NullValue.NULL;
      }

      _gzOutputPairs.put(env, pair);
    } else {
      pair = _gzOutputPairs.get(env);
      
      if (pair == null)
        return NullValue.NULL;
    }

    try {
      pair.outputStream.write(buffer.toString().getBytes());
      pair.outputStream.flush();

      if ((state & (1 << OutputBuffer.PHP_OUTPUT_HANDLER_END)) != 0) {
        pair.outputStream.close();

        _gzOutputPairs.remove(env);
      }
      
    } catch (IOException e) {
      return NullValue.NULL;
    }

    Value result = new TempBufferStringValue(pair.tempStream.getHead());

    pair.tempStream.discard();

    return result;
  }

  static {
    addIni(_iniMap, "output_buffering", "0", PHP_INI_PERDIR);
    addIni(_iniMap, "output_handler", "", PHP_INI_PERDIR);
    addIni(_iniMap, "implicit_flush", "0", PHP_INI_ALL);
  }
}
