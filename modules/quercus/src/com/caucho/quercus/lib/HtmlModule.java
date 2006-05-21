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

import com.caucho.quercus.env.ArrayValue;
import com.caucho.quercus.env.ConstArrayValue;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.StringValueImpl;
import com.caucho.quercus.env.Value;
import com.caucho.quercus.env.StringValue;
import com.caucho.quercus.module.AbstractQuercusModule;
import com.caucho.quercus.module.Optional;
import com.caucho.util.L10N;

import java.util.HashMap;

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
  public static final int ENT_QUOTES = ENT_HTML_QUOTE_SINGLE|ENT_HTML_QUOTE_DOUBLE;
  public static final int ENT_NOQUOTES = ENT_HTML_QUOTE_NONE;

  private final static ArrayValue HTML_SPECIALCHARS_ARRAY
    = new ConstArrayValue();
  private final static ArrayValue HTML_ENTITIES_ARRAY
    = new ConstArrayValue();

  private final static HashMap<String,String> HTML_DECODE
    = new HashMap<String,String>();

  private final ArrayValue _htmlQuotesArray = new ConstArrayValue();
  private final ArrayValue _htmlNoQuotesArray = new ConstArrayValue();

  public HtmlModule()
  {
  }

  /**
   * Returns HTML translation tables.
   */
  public Value get_html_translation_table(@Optional("HTML_SPECIALCHARS") int table,
                                          @Optional("ENT_COMPAT") int quoteStyle)
  {
    Value result;

    if (table == HTML_ENTITIES)
      result = HTML_ENTITIES_ARRAY.copy();
    else
      result = HTML_SPECIALCHARS_ARRAY.copy();

    if ((quoteStyle & ENT_HTML_QUOTE_SINGLE) != 0)
      result.put(new StringValueImpl("'"), new StringValueImpl("&apos;"));

    if ((quoteStyle & ENT_HTML_QUOTE_DOUBLE) != 0)
      result.put(new StringValueImpl("\""), new StringValueImpl("&quot;"));

    return result;
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
  public static Value htmlspecialchars(Env env,
				       Value stringV,
                                       @Optional Value quoteStyleV,
                                       @Optional Value charsetV)
  {
      // XXX: quotestyle and charset
    String string = stringV.toString();
    StringBuilder sb = new StringBuilder();

    int len = string.length();
    for (int i = 0; i < len; i++) {
      char ch = string.charAt(i);

      switch (ch) {
      case '&':
        sb.append("&amp;");
        break;
      case '"':
        sb.append("&quot;");
        break;
      case '\'':
        sb.append("'");
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

    return new StringValueImpl(sb.toString());
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
				   Value stringV,
                                   @Optional Value quoteStyleV,
                                   @Optional Value charsetV)
  {
    // XXX: other entities
    return htmlspecialchars(env, stringV, quoteStyleV, charsetV);
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
      return StringValue.EMPTY;

   // StringBuilder result = new StringBuilder();

   // int i = 0;
    //int length = string.length();

    // generate keys & values for preg_replace
   // ArrayValue decodedArray = new ArrayValueImpl();
    //ArrayValue encodedArray = new ArrayValueImpl();

    Value[] keys = HTML_SPECIALCHARS_ARRAY.getKeyArray();
    Value[] values = HTML_SPECIALCHARS_ARRAY.getValueArray(env);
    int length = keys.length;
    for (int i = 0; i < length; i++) {
      string = RegexpModule.ereg_replace(env,
					 values[i].toStringValue(),
					 keys[i].toStringValue(),
					 string).toStringValue();
    }
    /*
    Value value = QuercusRegexpModule.preg_replace(env,
                                                   encodedArray,
                                                   decodedArray,
                                                   new StringValueImpl(string),
                                                   -1,
                                                   null);*/
    return string;
  }

  /**
   * Replaces newlines with HTML breaks.
   *
   * @param env the calling environment
   */
  public static Value nl2br(Env env, Value stringV)
  {
    String string = stringV.toString();

    int strLen = string.length();

    StringBuilder sb = new StringBuilder();

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

    return new StringValueImpl(sb.toString());
  }

  private static void entity(int ch, String entity)
  {
    HTML_ENTITIES_ARRAY.put("&" + (char) ch + ";", entity);
    HTML_DECODE.put(entity, String.valueOf((char) ch));
  }

  static {
    HTML_SPECIALCHARS_ARRAY.put("<", "&lt;");
    HTML_SPECIALCHARS_ARRAY.put(">", "&gt;");
    HTML_SPECIALCHARS_ARRAY.put("&", "&amp;");

    entity('<', "lt");
    entity('>', "gt");
    entity('&', "amp");

    entity(160, "nbsp");
    entity(161, "iexcl");
    entity(162, "cent");
    entity(163, "pound");
    entity(164, "curren");
    entity(165, "yen");
    entity(166, "brvbar");
    entity(167, "sect");
    entity(168, "uml");
    entity(169, "copy");
    entity(170, "ordf");
    entity(171, "laquo");
    entity(172, "not");
    entity(173, "shy");
    entity(174, "reg");
    entity(175, "macr");
    entity(176, "deg");
    entity(177, "plusmn");
    entity(178, "sup2");
    entity(179, "sup3");
    entity(180, "acute");
    entity(181, "micro");
    entity(182, "para");
    entity(183, "middot");
    entity(184, "cedil");
    entity(185, "sup1");
    entity(186, "ordm");
    entity(187, "raquo");
    entity(188, "frac14");
    entity(189, "frac12");
    entity(190, "frac34");
    entity(191, "iquest");
    entity(192, "Agrave");
    entity(193, "Aacute");
    entity(194, "Acirc");
    entity(195, "Atilde");
    entity(196, "Auml");
    entity(197, "Aring");
    entity(198, "AElig");
    entity(199, "Ccedil");
    entity(200, "Egrave");
    entity(201, "Eacute");
    entity(202, "Ecirc");
    entity(203, "Euml");
    entity(204, "Igrave");
    entity(205, "Iacute");
    entity(206, "Icirc");
    entity(207, "Iuml");
    entity(208, "ETH");
    entity(209, "Ntilde");
    entity(210, "Ograve");
    entity(211, "Oacute");
    entity(212, "Ocirc");
    entity(213, "Otilde");
    entity(214, "Ouml");
    entity(215, "times");
    entity(216, "Oslash");
    entity(217, "Ugrave");
    entity(218, "Uacute");
    entity(219, "Ucirc");
    entity(220, "Uuml");
    entity(221, "Yacute");
    entity(222, "THORN");
    entity(223, "szlig");
    entity(224, "agrave");
    entity(225, "aacute");
    entity(226, "acirc");
    entity(227, "atilde");
    entity(228, "auml");
    entity(229, "aring");
    entity(230, "aelig");
    entity(231, "ccedil");
    entity(232, "egrave");
    entity(233, "eacute");
    entity(234, "ecirc");
    entity(235, "euml");
    entity(236, "igrave");
    entity(237, "iacute");
    entity(238, "icirc");
    entity(239, "iuml");
    entity(240, "eth");
    entity(241, "ntilde");
    entity(242, "ograve");
    entity(243, "oacute");
    entity(244, "ocirc");
    entity(245, "otilde");
    entity(246, "ouml");
    entity(247, "divide");
    entity(248, "oslash");
    entity(249, "ugrave");
    entity(250, "uacute");
    entity(251, "ucirc");
    entity(252, "uuml");
    entity(253, "yacute");
    entity(254, "thorn");
    entity(255, "yuml");
  }
}

