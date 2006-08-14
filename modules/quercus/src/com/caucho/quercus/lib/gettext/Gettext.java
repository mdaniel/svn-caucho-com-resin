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
import com.caucho.quercus.env.LocaleInfo;
import com.caucho.quercus.env.StringValue;
import com.caucho.quercus.env.StringValueImpl;
import com.caucho.quercus.env.Value;

import com.caucho.quercus.lib.string.StringModule;

import com.caucho.quercus.module.AbstractQuercusModule;
import com.caucho.quercus.module.NotNull;
import com.caucho.quercus.module.ReturnNullAsFalse;
import com.caucho.quercus.module.Optional;

import com.caucho.util.L10N;
import com.caucho.util.LruCache;
import com.caucho.vfs.Path;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;

public class Gettext
{
  private static final Logger log
    = Logger.getLogger(Gettext.class.getName());
  private static final L10N L = new L10N(Gettext.class);

  private String currentCharset;
  private StringValue currentDomain = new StringValueImpl("messages");

  private HashMap<StringValue,Path> domainPaths = new HashMap();
  private HashMap<StringValue,String> domainCharsets = new HashMap();

  private LruCache<String,StringValue> cache = new LruCache(128);

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

  public StringValue textdomain(Env env,
                          Value text_domain)
  {
    if (text_domain.isNull())
      return currentDomain;

    currentDomain = text_domain.toStringValue();
    return currentDomain;
  }

  private StringValue translate(Env env,
                          CharSequence domain,
                          CharSequence category,
                          StringValue message)
  {
    Path path = getPath(env, category, domain);

    if (path == null) {
      env.warning(L.l("Translation MO file was not found."));
      return message;
    }

    String id = getCacheId(path, message);
    StringValue translation = cache.get(id);

    if (translation != null)
      return translation;

    translation = MOFileParser.translate(env, path, message);

    cache.put(id, translation);
    return translation;
  }

  private StringValue translate(Env env,
                          CharSequence domain,
                          CharSequence category,
                          StringValue msgid1,
                          StringValue msgid2,
                          int n)
  {
    Path path = getPath(env, category, domain);

    if (path == null) {
      env.warning(L.l("Translation MO file was not found."));
      return MOFileParser.errorReturn(msgid1, msgid2, n);
    }

    String id = getCacheId(path, msgid1, msgid2, n);
    StringValue translation = cache.get(id);

    if (translation != null)
      return translation;

    translation = MOFileParser.translate(env, path, msgid1, msgid2, n);

    cache.put(id, translation);
    return translation;
  }

  private Path getPath(Env env, CharSequence category, CharSequence domain)
  {
    Locale locale = env.getLocaleInfo().getMessages();
    Path path = domainPaths.get(domain);

    StringBuilder sb = new StringBuilder(locale.toString());
    sb.append('/');
    sb.append(category);
    sb.append('/');
    sb.append(domain);
    sb.append(".mo");

    if (path != null)
      return path.lookup(sb.toString());
    else
      return env.lookup(sb.toString());
  }

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

  private static String getCacheId(Path path,
                          StringValue message)
  {
    StringBuilder sb = new StringBuilder();

    sb.append(path.toString());
    sb.append('\u0000');
    sb.append(path.getLastModified());
    sb.append('\u0000');
    sb.append(message);

    return sb.toString();
  }

  private static String getCacheId(Path path,
                          StringValue msgid1,
                          StringValue msgid2,
                          int n)
  {
    StringBuilder sb = new StringBuilder();

    sb.append(path.toString());
    sb.append('\u0000');
    sb.append(path.getLastModified());
    sb.append('\u0000');
    sb.append(msgid1);
    sb.append('\u0000');
    sb.append(msgid2);
    sb.append(n);

    return sb.toString();
  }

}
