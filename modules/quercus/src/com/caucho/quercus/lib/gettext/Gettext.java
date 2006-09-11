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

import com.caucho.quercus.UnimplementedException;

import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.StringValue;
import com.caucho.quercus.env.StringValueImpl;
import com.caucho.quercus.env.Value;

import com.caucho.quercus.lib.i18n.IconvUtility;
import com.caucho.quercus.lib.string.StringModule;

import com.caucho.quercus.module.AbstractQuercusModule;
import com.caucho.quercus.module.NotNull;
import com.caucho.quercus.module.ReturnNullAsFalse;
import com.caucho.quercus.module.Optional;

import com.caucho.util.L10N;
import com.caucho.util.LruCache;
import com.caucho.vfs.Path;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.HashMap;
import java.util.Locale;

public class Gettext
{
  private static final Logger log =
          Logger.getLogger(Gettext.class.getName());
  private static final L10N L = new L10N(Gettext.class);

  private StringValue currentDomain = new StringValueImpl("messages");

  private HashMap<StringValue,Path> domainPaths =
          new HashMap<StringValue,Path>();
  private HashMap<StringValue,String> domainCharsets =
          new HashMap<StringValue,String>();

  private LruCache<ArrayList<Object>,GettextResource> resourceCache =
          new LruCache<ArrayList<Object>,GettextResource>(16);

  public StringValue bind_textdomain_codeset(Env env,
                              StringValue domain,
                              StringValue codeset)
  {
    domainCharsets.put(domain, codeset.toString());
    return codeset;
  }

  public StringValue bindtextdomain(Env env,
                              StringValue domain,
                              StringValue directory)
  {
    domainPaths.put(domain, env.lookupPwd(directory));
    return directory;
  }

  public StringValue dcgettext(Env env,
                              StringValue domain,
                              StringValue message,
                              int category)
  {
    return translate(env,
                domain,
                getCategory(env, category),
                message);
  }

  public StringValue dcngettext(Env env,
                              StringValue domain,
                              StringValue msgid1,
                              StringValue msgid2,
                              int n,
                              int category)
  {
    return translate(env,
                domain,
                getCategory(env, category),
                msgid1,
                msgid2,
                n);
  }

  public StringValue dgettext(Env env,
                              StringValue domain,
                              StringValue message)
  {
    return translate(env,
                domain,
                "LC_MESSAGES",
                message);
  }

  public StringValue dngettext(Env env,
                              StringValue domain,
                              StringValue msgid1,
                              StringValue msgid2,
                              int n)
  {
    return translate(env,
                domain,
                "LC_MESSAGES",
                msgid1,
                msgid2,
                n);
  }

  public StringValue gettext(Env env,
                              StringValue message)
  {
    return translate(env,
                currentDomain,
                "LC_MESSAGES",
                message);
  }

  public StringValue ngettext(Env env,
                              StringValue msgid1,
                              StringValue msgid2,
                              int n)
  {
    return translate(env,
                currentDomain,
                "LC_MESSAGES",
                msgid1,
                msgid2,
                n);
  }

  public StringValue textdomain(Env env, Value text_domain)
  {
    if (text_domain.isNull())
      return currentDomain;

    currentDomain = text_domain.toStringValue();
    return currentDomain;
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
                              StringValue message)
  {
    Locale locale = env.getLocaleInfo().getMessages();

    ArrayList<Object> key = new ArrayList<Object>();

    key.add(locale);
    key.add(domain);
    key.add(category);

    GettextResource resource = resourceCache.get(key);

    if (resource == null) {
      resource = new GettextResource(env,
                  domainPaths.get(domain),
                  locale,
                  category,
                  domain);

      resourceCache.put(key, resource);
    }

    StringValue translation = resource.getTranslation(message);

    if (translation == null)
      return message;

    String charset = domainCharsets.get(domain);

    try {
      if (charset != null)
        translation = IconvUtility.encode(translation, charset);
    } catch (UnsupportedEncodingException e) {
      env.warning(L.l(e.getMessage()));
      log.log(Level.FINE, e.getMessage(), e);
    }

    return translation;
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
                              int quantity)
  {

    Locale locale = env.getLocaleInfo().getMessages();

    ArrayList<Object> key = new ArrayList<Object>();

    key.add(locale);
    key.add(domain);
    key.add(category);

    GettextResource resource = resourceCache.get(key);

    if (resource == null) {
      resource = new GettextResource(env,
                  domainPaths.get(domain),
                  locale,
                  category,
                  domain);

      resourceCache.put(key, resource);
    }

    StringValue translation = resource.getTranslation(msgid1, quantity);

    if (translation == null)
      return errorReturn(msgid1, msgid2, quantity);

    String charset = domainCharsets.get(domain);

    try {
      if (charset != null)
        translation = IconvUtility.encode(translation, charset);
    } catch (UnsupportedEncodingException e) {
      env.warning(L.l(e.getMessage()));
      log.log(Level.FINE, e.getMessage(), e);
    }

    return translation;
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
}
