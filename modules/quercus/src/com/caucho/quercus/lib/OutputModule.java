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

import java.io.StringWriter;
import java.io.IOException;
import java.io.InputStream;

import java.util.Map;
import java.util.HashMap;
import java.util.logging.Logger;

import java.net.URI;
import java.net.URISyntaxException;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Element;

import org.xml.sax.SAXException;

import com.caucho.xml.LooseHtml;
import com.caucho.xml.XmlPrinter;

import com.caucho.util.L10N;

import com.caucho.quercus.QuercusModuleException;

import com.caucho.quercus.program.AbstractFunction;

import com.caucho.quercus.lib.HttpModule;
import com.caucho.quercus.lib.session.SessionModule;

import com.caucho.quercus.module.AbstractQuercusModule;
import com.caucho.quercus.module.Optional;
import com.caucho.quercus.module.StaticFunction;

import com.caucho.quercus.env.Value;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.NullValue;
import com.caucho.quercus.env.BooleanValue;
import com.caucho.quercus.env.LongValue;
import com.caucho.quercus.env.StringValue;
import com.caucho.quercus.env.StringValueImpl;
import com.caucho.quercus.env.ArrayValue;
import com.caucho.quercus.env.ArrayValueImpl;
import com.caucho.quercus.env.Callback;
import com.caucho.quercus.env.CallbackFunction;
import com.caucho.quercus.env.OutputBuffer;

/**
 * PHP output routines.
 */
public class OutputModule extends AbstractQuercusModule {
  private static final L10N L = new L10N(OutputModule.class);
  private static final Logger log
    = Logger.getLogger(OutputModule.class.getName());

  /**
   * Sets the implicit flush.
   */
  public Value flush(Env env)
  {
    env.flush();

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

    if (ob != null)
      ob.clean();

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
      return ob.getLength();
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
   * Returns a list of all the output handlers in use.
   */
  public static Value ob_list_handlers(Env env)
  {
    OutputBuffer ob = env.getBottomOutputBuffer();
    ArrayValue handlers = new ArrayValueImpl();

    for (; ob != null; ob = ob.getPrevious()) {
      Callback callback = ob.getCallback();
      if (callback != null) 
        handlers.put(new StringValueImpl(callback.getCallbackName()));
      else
        handlers.put(new StringValueImpl("default output handler".intern()));
    }

    return handlers;
  }

  /**
   * Gets the status of the current output buffer(s)
   */ 
  public static Value ob_get_status(Env env, @Optional boolean full_status)
  {
    OutputBuffer ob;
    if (full_status)
      ob = env.getBottomOutputBuffer();
    else 
      ob = env.getOutputBuffer();

    ArrayValue result = new ArrayValueImpl();

    if (ob != null) {
      if (full_status) {
        for (; ob != null; ob = ob.getPrevious()) {
          ArrayValue element = new ArrayValueImpl();

          element.put(new StringValueImpl("chunk_size"), 
                      LongValue.create(ob.getChunkSize()));
          
          // XXX: Not sure why we even need to list a size -- c-php doesn't 
          // even seem to respect it.  -1 => infinity?  
          // (Note: "size" == "capacity")
          element.put(new StringValueImpl("size"), LongValue.create(-1));
          element.put(new StringValueImpl("block_size"), LongValue.create(-1));

          LongValue type = LongValue.ONE;
          Callback callback = ob.getCallback();
          if (callback != null && callback.isInternal()) {
            type = LongValue.ZERO;
          }
          element.put(new StringValueImpl("type"), type);

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
          
          result.put(element);
        }
      } else {
        result.put(new StringValueImpl("level"), 
                   LongValue.create(ob.getLevel()));

        LongValue type = LongValue.ONE;
        Callback callback = ob.getCallback();
        if (callback != null && callback.isInternal()) {
          type = LongValue.ZERO;
        }
        result.put(new StringValueImpl("type"), type);

        LongValue status = ob.haveFlushed() ? LongValue.ONE : LongValue.ZERO;
        result.put(new StringValueImpl("status"), status);

        StringValue name;
        if (callback != null)
          name = new StringValueImpl(callback.getCallbackName());
        else
          name = new StringValueImpl("default output handler".intern());
        result.put(new StringValueImpl("name".intern()), name);

        Value del = ob.getEraseFlag() ? BooleanValue.TRUE : BooleanValue.FALSE;
        result.put(new StringValueImpl("del"), del);
      }
    } // returns an empty array when no output buffer exists

    return result;
  }

  /**
   * Makes the original "output buffer" flush on every write.
   */
  public static Value ob_implicit_flush(Env env, @Optional("true") boolean flag)
  {
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
    env.pushOutputBuffer(callback, chunkSize, erase);

    return true;
  }

  /**
   * Adds a variable to the list for rewritten URLs.
   */
  public static boolean output_add_rewrite_var(Env env, 
                                               String name, String value)
  {
    boolean pushUrlRewriter = true;
    OutputBuffer ob = env.getOutputBuffer();
    for (; ob != null; ob = ob.getNext()) {
      Callback callback = ob.getCallback();
      if (callback != null && 
          "URL-Rewriter".equals(callback.getCallbackName()) && 
          callback.isInternal()) {
        pushUrlRewriter = false;
      }
    }

    if (pushUrlRewriter) {
      StaticFunction urlRewriterFunction= 
        env.getQuercus().findFunction("_internal_url_rewriter");
      // Change the name to be compatible with PHP
      Callback urlRewriter = 
        new CallbackFunction(urlRewriterFunction, "URL-Rewriter");
      ob_start(env, urlRewriter, 0, true);
    }
    
    env.addRewriteVar(name, value);
    return true;
  }

  /**
   * Clears the list of variables for rewritten URLs.
   */
  public static boolean output_reset_rewrite_vars(Env env)
  {
    env.resetRewriteVars();
    return true;
  }

  private enum Encoding {NONE, GZIP, DEFLATE};
  /**
   * Output buffering compatible callback that automatically compresses
   * the output.  This is a very simple implementation of this idea.
   * Specifically, repeated calls to this function (e.g. because of flushing
   * the buffer twice, etc.) create multiple gzip/deflate headers.  This
   * is the same functionality provided by the C-based PHP.
   */
  public static Value ob_gzhandler(Env env, StringValue buffer, int mode)
  {
    final StringValue HTTP_ACCEPT_ENCODING = 
      new StringValueImpl("HTTP_ACCEPT_ENCODING");

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

    switch (encoding) {
      case GZIP:
        HttpModule.header(env, "Vary: Accept-Encoding", true, 0);
        HttpModule.header(env, "Content-Encoding: gzip", true, 0);
        AbstractFunction gzcompress = env.getFunction("gzencode");
        return gzcompress.call(env, buffer);
      case DEFLATE:
        HttpModule.header(env, "Vary: Accept-Encoding", true, 0);
        HttpModule.header(env, "Content-Encoding: deflate", true, 0);
        AbstractFunction gzdeflate = env.getFunction("gzdeflate");
        return gzdeflate.call(env, buffer);
    }
    
    return buffer;
  }

  private static class Token {
    private int _startPosition;
    private int _endPosition;
    private String _token;

    public Token(String token, int startPosition, int endPosition) 
    {
      _token = token;
      _startPosition = startPosition;
      _endPosition = endPosition;
    }

    public int getStartPosition()
    {
      return _startPosition;
    }

    public int getEndPosition()
    {
      return _endPosition;
    }

    public String getToken()
    {
      return _token;
    }

    /**
     * Helper function for the URL rewriter below.
     */
    public static Token nextToken(StringBuffer buffer, 
                                   int startPosition, int endIndex)
    {
      while (startPosition < endIndex && startPosition < buffer.length() &&
             Character.isWhitespace(buffer.charAt(startPosition))) {
        startPosition++;
      }
      int endPosition = startPosition;
      while (endPosition < endIndex && endPosition < buffer.length() &&
             !Character.isWhitespace(buffer.charAt(endPosition))) {
        endPosition++;
      }
      if (startPosition == endPosition)
        return null;
      return new Token(buffer.substring(startPosition, endPosition), 
                       startPosition, endPosition);
    }
    
    public static Token nextToken(StringBuffer buffer, int startPosition)
    {
      return nextToken(buffer, startPosition, Integer.MAX_VALUE);
    }
  }
  
  /**
   * Callback function to rewrite URLs to include session information.
   */
  public static Value _internal_url_rewriter(Env env, Value buffer)
  {
    boolean includeSessionInfo = false;
    String sessionName = null;
    String sessionId = null;

    if (env.getSession() != null) {
      includeSessionInfo = true;
      sessionName = env.getIni("session.name").toString();
      sessionId = SessionModule.session_id(env, null);
    }

    if (includeSessionInfo == false && env.getRewriteVars().isEmpty())
      return buffer;

    String [] tagPairs = env.getIni("url_rewriter.tags").toString().split(",");
    HashMap<String,String> tags = new HashMap<String,String>();
    for (String tagPair : tagPairs) {
      String [] tagAttribute = tagPair.split("=");
      switch (tagAttribute.length) {
        case 1:
          tags.put(tagAttribute[0], null);
          break;
        case 2:
          tags.put(tagAttribute[0], tagAttribute[1]);
          break;
        default:
          break;
      }
    }

    StringBuffer output = new StringBuffer(buffer.toString());

    int [] ref = new int[1];
    for (int i = 0; i < output.length();) {
      // get the first token after a '<'
      i = output.indexOf("<", i);
      if (i == -1) break;
      Token token = Token.nextToken(output, i+1);
      if (token == null) break;
      i = token.getEndPosition();

      if (tags.containsKey(token.getToken())) {
        String attribute = tags.get(token.getToken());
        if (attribute == null) {
          i = output.indexOf(">", i);
          if (i == -1) break;

          if (includeSessionInfo) {
            String inputTag = 
              "<input type=\"hidden\" name=\"" + sessionName + "\"" +
              " value=\"" + sessionId + "\" />";
            output.insert(i+1, inputTag);
            i += inputTag.length();
          }
          for (String[] entry : env.getRewriteVars()) {
            String inputTag = 
              "<input type=\"hidden\" name=\"" + entry[0] + "\"" +
              " value=\"" + entry[1] + "\" />";
            output.insert(i+1, inputTag);
            i += inputTag.length();
          }
        } else {
          int tagEnd = output.indexOf(">", i);
          for (token = Token.nextToken(output, i, tagEnd); 
               token != null;
               token = Token.nextToken(output, i, tagEnd)) {
            i = token.getStartPosition();
            int j = token.getToken().indexOf("=");
            if (j == -1) {
              i = token.getEndPosition();
              continue;
            }

            String attributeName = token.getToken().substring(0, j);

            j++; // skip the '='

            // unquote
            if (token.getToken().charAt(j) == '"' ||
                token.getToken().charAt(j) == '\'') 
              j++;

            int valueEnd;
            int quotePad = 0;
            if (token.getToken().endsWith("'") ||
                token.getToken().endsWith("\"")) {
              valueEnd = token.getToken().length() - 1;
              quotePad = 1;
            }
            else 
              valueEnd = token.getToken().length();

            String value = token.getToken().substring(j, valueEnd);

            if (attributeName.equalsIgnoreCase(attribute)) {
              // according to php documentation, it only adds tags to the
              // end of relative URLs, but according to RFC 2396, any
              // URI beginning with '/' (e.g. <a href="/foo">link</a>) is
              // absolute.  Nonetheless, php does add session ids to these
              // links.  Thus php must be defining "relative" as relative
              // to the host, not the hierarchy.  Thus we only check to make
              // sure that the scheme and authority are undefined, not that
              // the first character of the path begins with '/'.

              URI uri;
              try {
                uri = new URI(value);
              } catch (URISyntaxException e) {
                i = token.getEndPosition();
                continue;
              }
              if ((uri.getScheme() != null) || (uri.getAuthority() != null)) {
                i = token.getEndPosition();
                continue;
              }

              StringBuffer query = new StringBuffer();
              if (uri.getQuery() != null) {
                query.append(uri.getQuery());
                query.append("&");
              } 
              else
                query.append("?");

              if (includeSessionInfo) {
                query.append(sessionName);
                query.append("=");
                query.append(sessionId);
              }

              String varsQuery = env.getRewriteVarQuery();
              if (! "".equals(varsQuery)) {
                if (includeSessionInfo)
                  query.append("&");
                query.append(varsQuery);
              }

              StringBuffer newUri = new StringBuffer();
              if (uri.getPath() != null)
                newUri.append(uri.getPath());
              newUri.append(query);
              if (uri.getFragment() != null) {
                newUri.append("#");
                newUri.append(uri.getFragment());
              }

              output.replace(i + j, i + j + value.length(), newUri.toString());
              i += j + newUri.length() + quotePad;
            } 
            else
              i = token.getEndPosition();
          }
        }
      }
    }

    return new StringValueImpl(output.toString());
  }
}
