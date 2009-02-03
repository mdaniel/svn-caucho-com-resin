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
 * @author Scott Ferguson
 */

package com.caucho.quercus.lib;

import com.caucho.quercus.QuercusModuleException;
import com.caucho.quercus.annotation.Optional;
import com.caucho.quercus.env.*;
import com.caucho.quercus.lib.regexp.RegexpModule;
import com.caucho.quercus.module.AbstractQuercusModule;
import com.caucho.util.L10N;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * PHP functions implementing html code.
 */
public class HtmlModule extends AbstractQuercusModule {
  private static final L10N L = new L10N(HtmlModule.class);

  public static final int HTML_SPECIALCHARS = 0;
  public static final int HTML_ENTITIES = 1;

  public static final int ENT_HTML_QUOTE_NONE = 0;
  public static final int ENT_HTML_QUOTE_SINGLE = 1;
  public static final int ENT_HTML_QUOTE_DOUBLE = 2;

  public static final int ENT_COMPAT = ENT_HTML_QUOTE_DOUBLE;
  public static final int ENT_QUOTES = ENT_HTML_QUOTE_SINGLE | ENT_HTML_QUOTE_DOUBLE;
  public static final int ENT_NOQUOTES = ENT_HTML_QUOTE_NONE;

  private final static ArrayValue HTML_SPECIALCHARS_ARRAY
    = new ConstArrayValue();
  private final static ArrayValue HTML_ENTITIES_ARRAY
    = new ConstArrayValue();

  private static ArrayValueImpl HTML_ENTITIES_ARRAY_UNICODE;
  private static ArrayValueImpl HTML_SPECIALCHARS_ARRAY_UNICODE;

  public HtmlModule()
  {
  }

  private static ConstArrayValue toUnicodeArray(Env env, ArrayValue array)
  {
    ConstArrayValue copy = new ConstArrayValue();

    Iterator<Map.Entry<Value,Value>> iter = array.getIterator(env);

    while (iter.hasNext()) {
      Map.Entry<Value,Value> entry = iter.next();

      Value key = entry.getKey();
      Value value = entry.getValue();

      if (key.isString())
        key = key.toUnicodeValue(env);

      if (value.isString())
        value = value.toUnicodeValue(env);

      copy.put(key, value);
    }

    return copy;
  }

  /**
   * Returns HTML translation tables.
   */
  public Value get_html_translation_table(Env env,
                                          @Optional("HTML_SPECIALCHARS") int table,
                                          @Optional("ENT_COMPAT") int quoteStyle)
  {
    Value result;

    if (! env.isUnicodeSemantics()) {
      if (table == HTML_ENTITIES)
        result = HTML_ENTITIES_ARRAY.copy();
      else
        result = HTML_SPECIALCHARS_ARRAY.copy();
    }
    else {
      if (table == HTML_ENTITIES) {
        if (HTML_ENTITIES_ARRAY_UNICODE == null)
          HTML_ENTITIES_ARRAY_UNICODE = toUnicodeArray(env, HTML_ENTITIES_ARRAY);

        result = HTML_ENTITIES_ARRAY_UNICODE.copy();
      }
      else {
        if (HTML_SPECIALCHARS_ARRAY_UNICODE == null)
          HTML_SPECIALCHARS_ARRAY_UNICODE = toUnicodeArray(env, HTML_SPECIALCHARS_ARRAY);

        result = HTML_SPECIALCHARS_ARRAY_UNICODE.copy();
      }
    }

    if ((quoteStyle & ENT_HTML_QUOTE_SINGLE) != 0)
      result.put(env.createString('\''), env.createString("&apos;"));

    if ((quoteStyle & ENT_HTML_QUOTE_DOUBLE) != 0)
      result.put(env.createString('"'), env.createString("&quot;"));

    return result;
  }

  /*
   * Converts escaped HTML entities back to characters.
   * 
   * @param str escaped string
   * @param quoteStyle optional quote style used
   */
  public static StringValue htmlspecialchars_decode(Env env,
                                        StringValue str,
                                        @Optional("ENT_COMPAT") int quoteStyle)
  {
    int len = str.length();
    
    StringValue sb = str.createStringBuilder(len * 4 / 5);

    for (int i = 0; i < len; i++) {
      char ch = str.charAt(i);

      if (ch != '&') {
        sb.append(ch);
        
        continue;
      }
      
      switch (str.charAt(i + 1)) {
        case 'a':
          sb.append('&');
          if (i + 4 < len
              && str.charAt(i + 2) == 'm'
              && str.charAt(i + 3) == 'p'
              && str.charAt(i + 4) == ';') {
            i += 4;
          }
          break;
          
        case 'q':
          if ((quoteStyle & ENT_HTML_QUOTE_DOUBLE) != 0
              && i + 5 < len
              && str.charAt(i + 2) == 'u'
              && str.charAt(i + 3) == 'o'
              && str.charAt(i + 4) == 't'
              && str.charAt(i + 5) == ';') {
            i += 5;
            sb.append('"');
          }
          else
            sb.append('&');
          break;
          
        case '#':
          if ((quoteStyle & ENT_HTML_QUOTE_SINGLE) != 0
              && i + 5 < len
              && str.charAt(i + 2) == '0'
              && str.charAt(i + 3) == '3'
              && str.charAt(i + 4) == '9'
              && str.charAt(i + 5) == ';') {
            i += 5;
            sb.append('\'');
          }
          else
            sb.append('&');
          
          break;

        case 'l':
          if (i + 3 < len
              && str.charAt(i + 2) == 't'
              && str.charAt(i + 3) == ';') {
                i += 3;
                
                sb.append('<');
          }
          else
            sb.append('&');
          break;

        case 'g':
          if (i + 3 < len
              && str.charAt(i + 2) == 't'
              && str.charAt(i + 3) == ';') {
                i += 3;
                
                sb.append('>');
          }
          else
            sb.append('&');
          break;

        default:
          sb.append('&');
      }
    }

    return sb;
  }
  
  /**
   * Escapes HTML
   *
   * @param env the calling environment
   * @param string the string to be trimmed
   * @param quoteStyleV optional quote style
   * @param charsetV optional charset style
   * @return the trimmed string
   */
  public static Value htmlspecialchars(Env env,
                                       StringValue string,
                                       @Optional("ENT_COMPAT") int quoteStyle,
                                       @Optional String charset)
  {
    int len = string.length();
    
    StringValue sb = string.createStringBuilder(len * 5 / 4);

    for (int i = 0; i < len; i++) {
      char ch = string.charAt(i);

      switch (ch) {
      case '&':
        sb.append("&amp;");
        break;
      case '"':
        if ((quoteStyle & ENT_HTML_QUOTE_DOUBLE) != 0)
          sb.append("&quot;");
        else
          sb.append(ch);
        break;
      case '\'':
        if ((quoteStyle & ENT_HTML_QUOTE_SINGLE) != 0)
          sb.append("&#039;");
        else
          sb.append(ch);
        break;
      case '<':
        sb.append("&lt;");
        break;
      case '>':
        sb.append("&gt;");
        break;
      default:
        sb.append(ch);
        break;
      }
    }

    return sb;
  }

  /**
   * Escapes HTML
   *
   * @param env the calling environment
   * @param stringV the string to be trimmed
   * @param quoteStyleV optional quote style
   * @param charsetV optional charset style
   * @return the trimmed string
   */
  public static Value htmlentities(Env env,
                                   StringValue string,
                                   @Optional("ENT_COMPAT") int quoteStyle,
                                   @Optional String charset)
  {
    if (charset == null || charset.length() == 0)
      charset = "ISO-8859-1";
    
    Reader reader;
    
    try {
      reader = string.toReader(charset);
    } catch (UnsupportedEncodingException e) {
      env.warning(e);
      
      reader = new StringReader(string.toString());
    }
    
    StringValue sb = string.createStringBuilder(string.length() * 5 / 4);

    ArrayValue entitiesArray;
    
    if (env.isUnicodeSemantics()) {
      if (HTML_ENTITIES_ARRAY_UNICODE == null)
        HTML_ENTITIES_ARRAY_UNICODE = toUnicodeArray(env, HTML_ENTITIES_ARRAY);
      
      entitiesArray = HTML_ENTITIES_ARRAY_UNICODE;
    }
    else {
      entitiesArray = HTML_ENTITIES_ARRAY;
    }
    
    int ch;
    try {
      while ((ch = reader.read()) >= 0) {
        StringValue chV = env.createString((char) ch);
        
        Value entity = entitiesArray.get(chV);
        
        if (entity.isNull())
          entity = chV;
        
        if (ch == '"') {
          if ((quoteStyle & ENT_HTML_QUOTE_DOUBLE) != 0) {
            entity = env.createString("&quot;");
          }
          else
            entity = chV;
        }
        else if (ch == '\'') {
          if ((quoteStyle & ENT_HTML_QUOTE_SINGLE) != 0) {
            entity = env.createString("&#039;");
          }
          else
            entity = chV;
        }
        
        sb.append(entity);
      }
    } catch (IOException e) {
      throw new QuercusModuleException(e);
    }

    return sb;
  }

  /**
   * Escapes HTML
   *
   * @param string the string to be trimmed
   * @param quoteStyle optional quote style
   * @param charset optional charset style
   * @return the trimmed string
   */
  public static StringValue html_entity_decode(Env env,
					       StringValue string,
					       @Optional int quoteStyle,
					       @Optional String charset)
  {
    if (string.length() == 0)
      return env.getEmptyString();

    Iterator<Map.Entry<Value,Value>> iter;

    if (env.isUnicodeSemantics()) {
      if (HTML_ENTITIES_ARRAY_UNICODE == null)
        HTML_ENTITIES_ARRAY_UNICODE = toUnicodeArray(env, HTML_ENTITIES_ARRAY);
      
      iter = HTML_ENTITIES_ARRAY_UNICODE.getIterator(env);
    }
    else
      iter = HTML_ENTITIES_ARRAY.getIterator(env);

    while (iter.hasNext()) {
      Map.Entry<Value,Value> entry = iter.next();
      StringValue key = entry.getKey().toStringValue();
      StringValue value = entry.getValue().toStringValue();

      string = RegexpModule.ereg_replace(env,
                                         value,
                                         key,
                                         string).toStringValue();
    }

    return string;
  }

  /**
   * Replaces newlines with HTML breaks.
   *
   * @param env the calling environment
   */
  public static Value nl2br(Env env, StringValue string)
  {
    int strLen = string.length();

    StringValue sb = string.createStringBuilder(strLen * 5 / 4);

    for (int i = 0; i < strLen; i++) {
      char ch = string.charAt(i);

      if (ch == '\n') {
        sb.append("<br />\n");
      }
      else if (ch == '\r') {
        if (i + 1 < strLen && string.charAt(i + 1) == '\n') {
          sb.append("<br />\r\n");
          i++;
        }
        else {
          sb.append("<br />\r");
        }
      }
      else {
        sb.append(ch);
      }
    }

    return sb;
  }

  private static void entity(int ch, String entity)
  {
    // XXX: i18n and optimize static variables usuage
    HTML_ENTITIES_ARRAY.put("" + (char) ch, entity);
  }

  static {
    HTML_SPECIALCHARS_ARRAY.put("<", "&lt;");
    HTML_SPECIALCHARS_ARRAY.put(">", "&gt;");
    HTML_SPECIALCHARS_ARRAY.put("&", "&amp;");

    entity('<', "&lt;");
    entity('>', "&gt;");
    entity('&', "&amp;");

    entity(160, "&nbsp;");
    entity(161, "&iexcl;");
    entity(162, "&cent;");
    entity(163, "&pound;");
    entity(164, "&curren;");
    entity(165, "&yen;");
    entity(166, "&brvbar;");
    entity(167, "&sect;");
    entity(168, "&uml;");
    entity(169, "&copy;");
    entity(170, "&ordf;");
    entity(171, "&laquo;");
    entity(172, "&not;");
    entity(173, "&shy;");
    entity(174, "&reg;");
    entity(175, "&macr;");
    entity(176, "&deg;");
    entity(177, "&plusmn;");
    entity(178, "&sup2;");
    entity(179, "&sup3;");
    entity(180, "&acute;");
    entity(181, "&micro;");
    entity(182, "&para;");
    entity(183, "&middot;");
    entity(184, "&cedil;");
    entity(185, "&sup1;");
    entity(186, "&ordm;");
    entity(187, "&raquo;");
    entity(188, "&frac14;");
    entity(189, "&frac12;");
    entity(190, "&frac34;");
    entity(191, "&iquest;");
    entity(192, "&Agrave;");
    entity(193, "&Aacute;");
    entity(194, "&Acirc;");
    entity(195, "&Atilde;");
    entity(196, "&Auml;");
    entity(197, "&Aring;");
    entity(198, "&AElig;");
    entity(199, "&Ccedil;");
    entity(200, "&Egrave;");
    entity(201, "&Eacute;");
    entity(202, "&Ecirc;");
    entity(203, "&Euml;");
    entity(204, "&Igrave;");
    entity(205, "&Iacute;");
    entity(206, "&Icirc;");
    entity(207, "&Iuml;");
    entity(208, "&ETH;");
    entity(209, "&Ntilde;");
    entity(210, "&Ograve;");
    entity(211, "&Oacute;");
    entity(212, "&Ocirc;");
    entity(213, "&Otilde;");
    entity(214, "&Ouml;");
    entity(215, "&times;");
    entity(216, "&Oslash;");
    entity(217, "&Ugrave;");
    entity(218, "&Uacute;");
    entity(219, "&Ucirc;");
    entity(220, "&Uuml;");
    entity(221, "&Yacute;");
    entity(222, "&THORN;");
    entity(223, "&szlig;");
    entity(224, "&agrave;");
    entity(225, "&aacute;");
    entity(226, "&acirc;");
    entity(227, "&atilde;");
    entity(228, "&auml;");
    entity(229, "&aring;");
    entity(230, "&aelig;");
    entity(231, "&ccedil;");
    entity(232, "&egrave;");
    entity(233, "&eacute;");
    entity(234, "&ecirc;");
    entity(235, "&euml;");
    entity(236, "&igrave;");
    entity(237, "&iacute;");
    entity(238, "&icirc;");
    entity(239, "&iuml;");
    entity(240, "&eth;");
    entity(241, "&ntilde;");
    entity(242, "&ograve;");
    entity(243, "&oacute;");
    entity(244, "&ocirc;");
    entity(245, "&otilde;");
    entity(246, "&ouml;");
    entity(247, "&divide;");
    entity(248, "&oslash;");
    entity(249, "&ugrave;");
    entity(250, "&uacute;");
    entity(251, "&ucirc;");
    entity(252, "&uuml;");
    entity(253, "&yacute;");
    entity(254, "&thorn;");
    entity(255, "&yuml;");
  }
}

