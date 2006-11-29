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
 * @author Nam Nguyen
 */

package com.caucho.quercus.lib.gettext;

import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.BinaryBuilderValue;
import com.caucho.quercus.env.BinaryValue;
import com.caucho.quercus.env.BooleanValue;
import com.caucho.quercus.env.StringBuilderValue;
import com.caucho.quercus.env.StringValue;
import com.caucho.quercus.env.StringValueImpl;
import com.caucho.quercus.env.UnicodeValue;
import com.caucho.quercus.env.Value;

import com.caucho.quercus.lib.string.StringModule;

import com.caucho.quercus.module.AbstractQuercusModule;
import com.caucho.quercus.annotation.Optional;

import com.caucho.util.L10N;
import com.caucho.util.LruCache;
import com.caucho.vfs.Path;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.logging.Logger;


/**
 * Module to find translated strings and return them in desired charset.
 * Translations are LRU cached.
 */
public class GettextModule
    extends AbstractQuercusModule
{
  private LruCache<Object,GettextResource> _cache =
      new LruCache<Object,GettextResource>(16);

  private final Logger log
    = Logger.getLogger(GettextModule.class.getName());
  private final L10N L = new L10N(GettextModule.class);

  public String []getLoadedExtensions()
  {
    return new String[] { "gettext" };
  }

  /**
   * Sets charset of translated strings that are returned from this domain.
   *
   * @param env
   * @param domain
   * @param codeset
   * @return codeset
   */
  public StringValue bind_textdomain_codeset(Env env,
                              StringValue domain,
                              StringValue codeset)
  {
    return new StringValueImpl("UTF-16");
  }

  /**
   * Changes root directory of domain.
   *
   * @param env
   * @param domain
   * @param directory
   * @return directory
   */
  public Value bindtextdomain(Env env,
                              StringValue domain,
                              StringValue directory)
  {
    return setPath(env, domain, directory);
  }

  /**
   * Same as gettext, but allows overriding of domain and category.
   *
   * @param env
   * @param domain
   * @param message
   * @param category
   */
  public StringValue dcgettext(Env env,
                              StringValue domain,
                              StringValue message,
                              int category,
                              Value args[])
  {
    return translate(env,
                     domain,
                     getCategory(env, category),
                     message,
                     args);
  }

  /**
   * Same as ngettext, but allows overriding of domain and category.
   *
   * @param env
   * @param domain
   * @param msgid1
   * @param msgid2
   * @param n
   * @param category
   */
  public StringValue dcngettext(Env env,
                              StringValue domain,
                              StringValue msgid1,
                              StringValue msgid2,
                              int n,
                              int category,
                              Value args[])
  {
    return translate(env,
                     domain,
                     getCategory(env, category),
                     msgid1,
                     msgid2,
                     n,
                     args);
  }

  /**
   * Same as gettext, but allows overriding of current domain.
   *
   * @param env
   * @param domain
   * @param message
   */
  public StringValue dgettext(Env env,
                              StringValue domain,
                              StringValue message,
                              Value args[])
  {
    return translate(env,
                     domain,
                     "LC_MESSAGES",
                     message,
                     args);
  }

  /**
   * Same as ngettext, but allows overriding of current domain.
   *
   * @param env
   * @param domain
   * @param msgid1
   * @param msgid2
   * @param n
   */
  public StringValue dngettext(Env env,
                              StringValue domain,
                              StringValue msgid1,
                              StringValue msgid2,
                              int n,
                              Value args[])
  {
    return translate(env,
                     domain,
                     "LC_MESSAGES",
                     msgid1,
                     msgid2,
                     n,
                     args);
  }

  /**
   * Alias of gettext().
   *
   * @param env
   * @param message
   */
  public StringValue _(Env env, StringValue message, Value []args)
  {
    return gettext(env, message, args);
  }

  /**
   * Returns translated string from current domain and default category.
   *
   * @param env
   * @param message
   */
  public StringValue gettext(Env env, StringValue message, Value []args)
  {
    return translate(env,
                     getCurrentDomain(env),
                     "LC_MESSAGES",
                     message,
                     args);
  }

  /**
   * Returns translated plural string form from current domain and default
   * category.
   *
   * @param env
   * @param msgid1
   * @param msgid2
   * @param n
   * @return translated string, or original plural string if n == 1,
   *     else return original singular string
   */
  public StringValue ngettext(Env env,
                              StringValue msgid1,
                              StringValue msgid2,
                              int n,
                              Value args[])
  {
    return translate(env,
                     getCurrentDomain(env),
                     "LC_MESSAGES",
                     msgid1,
                     msgid2,
                     n,
                     args);
  }

  /**
   * Changes the current domain.
   *
   * @param env
   * @param domain
   * @return name of current domain after change.
   */
  public StringValue textdomain(Env env,
                              @Optional Value domain)
  {
    if (! domain.isNull())
      return setCurrentDomain(env, domain.toStringValue());

    return getCurrentDomain(env);
  }

  /**
   * Retrieves the translation for message.
   * 
   * @param env
   * @param domain
   * @param category
   * @param message
   *
   * @return translation found, else message
   */
  private StringValue translate(Env env,
                              StringValue domain,
                              CharSequence category,
                              StringValue message,
                              Value []args)
  {
    Locale locale = env.getLocaleInfo().getMessages();

    GettextResource resource = getResource(env,
                                           getPath(env, domain),
                                           locale,
                                           category,
                                           domain);

    StringValue translation = resource.getTranslation(message);

    if (translation == null)
      translation = message;

    return format(env, translation, args);
  }

  /**
   * Retrieves the plural translation for msgid1.
   * 
   * @param env
   * @param domain
   * @param category
   * @param msgid1
   * @param msgid2
   *
   * @return translation found, else msgid1 if n == 1, else msgid2
   */
  private StringValue translate(Env env,
                              StringValue domain,
                              CharSequence category,
                              StringValue msgid1,
                              StringValue msgid2,
                              int quantity,
                              Value []args)
  {
    Locale locale = env.getLocaleInfo().getMessages();

    GettextResource resource = getResource(env,
                                           getPath(env, domain),
                                           locale,
                                           category,
                                           domain);

    StringValue translation = resource.getTranslation(msgid1, quantity);

    if (translation == null)
      translation = errorReturn(msgid1, msgid2, quantity);

    return format(env, translation, args);
  }

  private GettextResource getResource(Env env,
                              Path path,
                              Locale locale,
                              CharSequence category,
                              StringValue domain)
  {
    ArrayList<Object> key = new ArrayList<Object>();

    key.add(path.getFullPath());
    key.add(locale);
    key.add(category);
    key.add(domain);

    GettextResource resource = _cache.get(key);

    if (resource == null) {
      resource = new GettextResource(env, path, locale, category, domain);
      _cache.put(key, resource);
    }

    return resource;
  }

  private Path getPath(Env env, StringValue domain)
  {
    Object val = env.getSpecialValue("caucho.gettext_paths");

    if (val == null) {
      val = new HashMap<StringValue,Path>();

      env.setSpecialValue("caucho.gettext_paths", val);
    }

    Path path = ((HashMap<StringValue,Path>)val).get(domain);

    if (path == null)
      return env.getPwd();

    return path;
  }

  private Value setPath(Env env,
                              StringValue domain,
                              StringValue directory)
  {
    Object val = env.getSpecialValue("caucho.gettext_paths");

    if (val == null) {
      val = new HashMap<StringValue,Path>();

      env.setSpecialValue("caucho.gettext_paths", val);
    }

    Path path = env.lookupPwd(directory);

    if (path == null)
      return BooleanValue.FALSE;

    ((HashMap<StringValue,Path>)val).put(domain, path);

    return directory;
  }

  private StringValue getCurrentDomain(Env env)
  {
    Object val = env.getSpecialValue("caucho.gettext_current");

    if (val == null)
      return setCurrentDomain(env, new StringValueImpl("messages"));

    return (StringValue)val;
  }

  private StringValue setCurrentDomain(Env env, StringValue currentDomain)
  {
    env.setSpecialValue("caucho.gettext_current", currentDomain);

    return currentDomain;
  }

  /**
   * Gets the name for this category.
   */
  private String getCategory(Env env, int category)
  {
    if (category == StringModule.LC_MESSAGES)
      return "LC_MESSAGES";
    else if (category == StringModule.LC_ALL)
      return "LC_ALL";
    else if (category == StringModule.LC_CTYPE)
      return "LC_CTYPE";
    else if (category == StringModule.LC_NUMERIC)
      return "LC_NUMERIC";
    else if (category == StringModule.LC_TIME)
      return "LC_TIME";
    else if (category == StringModule.LC_COLLATE)
      return "LC_COLLATE";
    else if (category == StringModule.LC_MONETARY)
      return "LC_MONETARY";
    else {
      env.warning(L.l("Invalid category. Please use named constants"));
      return "LC_MESSAGES";
    }
  }

  private static StringValue errorReturn(StringValue msgid1,
                              StringValue msgid2,
                              int n)
  {
    if (n == 1)
      return msgid1;
    else
      return msgid2;
  }

  private static StringValue format(Env env,
                              StringValue msg,
                              Value []args)
  {
    if (args.length == 0)
      return msg;
    else if (msg.isUnicode())
      return formatUnicode(env, msg, args);
    else
      return formatBinary(env, msg, args);
  }

  private static BinaryValue formatBinary(Env env,
                              StringValue msg,
                              Value []args)
  {
    BinaryBuilderValue sb = new BinaryBuilderValue();

    int i = 0;
    int length = msg.length();

    while (i < length) {
      char ch = msg.charAt(i);

      if (ch != '[' || i + 4 > length) {
        sb.appendByte(ch);
        i++;
      }
      else if (msg.charAt(i + 1) != '_') {
        sb.appendByte('[');
        i++;
      }
      else if (msg.charAt(i + 3) != ']') {
        sb.appendByte('[');
        sb.appendByte('_');
        i += 2;
      }
      else {
        ch = msg.charAt(i + 2);
        int argIndex = ch - '0';

        if (0 <= argIndex && argIndex < args.length) {
          args[argIndex].appendTo(sb);
          i += 4;
        }
        else {
          sb.appendByte('{');
          i++;
        }
      }
    }

    return sb;
  }

  private static UnicodeValue formatUnicode(Env env,
                              StringValue msg,
                              Value []args)
  {
    StringBuilderValue sb = new StringBuilderValue();

    int i = 0;
    int length = msg.length();

    while (i < length) {
      char ch = msg.charAt(i);

      if (ch != '[' || i + 4 > length) {
        sb.append(ch);
        i++;
      }
      else if (msg.charAt(i + 1) != '_') {
        sb.append(ch);
        i++;
      }
      else if (msg.charAt(i + 3) != ']') {
        sb.append(ch);
        i++;
      }
      else {
        ch = msg.charAt(i + 2);
        int argIndex = ch - '0';

        if (0 <= argIndex && argIndex < args.length) {
          args[argIndex].appendTo(sb);
          i += 4;
        }
        else {
          sb.append('[');
          i++;
        }
      }
    }

    return sb;
  }
}
