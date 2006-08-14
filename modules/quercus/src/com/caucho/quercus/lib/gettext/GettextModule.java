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
import com.caucho.quercus.env.Value;

import com.caucho.quercus.module.AbstractQuercusModule;
import com.caucho.quercus.module.NotNull;
import com.caucho.quercus.module.ReturnNullAsFalse;
import com.caucho.quercus.module.Optional;

import com.caucho.util.L10N;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Module to find translated strings and return them in desired charset.
 * Translations are LRU cached.
 */
public class GettextModule
    extends AbstractQuercusModule
{

//  private static final Logger log
//    = Logger.getLogger(GettextModule.class.getName());
//  private static final L10N L = new L10N(GettextModule.class);

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
  public static StringValue bind_textdomain_codeset(Env env,
                          StringValue domain,
                          StringValue codeset)
  {
    throw new UnimplementedException("bind_textdomain_codeset");

//    return getGettext(env).bind_textdomain_codeset(env, domain, codeset);
  }

  /**
   * Changes root directory of domain.
   *
   * @param env
   * @param domain
   * @param directory
   * @return directory
   */
  public static StringValue bindtextdomain(Env env,
                          StringValue domain,
                          StringValue directory)
  {
    return getGettext(env).bindtextdomain(env, domain, directory);
  }

  /**
   * Same as gettext, but allows overriding of domain and category.
   *
   * @param env
   * @param domain
   * @param message
   * @param category
   */
  public static StringValue dcgettext(Env env,
                          StringValue domain,
                          StringValue message,
                          int category)
  {
    return getGettext(env).dcgettext(env, domain, message, category);
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
  public static StringValue dcngettext(Env env,
                          StringValue domain,
                          StringValue msgid1,
                          StringValue msgid2,
                          int n,
                          int category)
  {
    return
        getGettext(env).dcngettext(env, domain, msgid1, msgid2, n, category);
  }

  /**
   * Same as gettext, but allows overriding of current domain.
   *
   * @param env
   * @param domain
   * @param message
   */
  public static StringValue dgettext(Env env,
                          StringValue domain,
                          StringValue message)
  {
    return getGettext(env).dgettext(env, domain, message);
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
  public static StringValue dngettext(Env env,
                          StringValue domain,
                          StringValue msgid1,
                          StringValue msgid2,
                          int n)
  {
    return getGettext(env).dngettext(env, domain, msgid1, msgid2, n);
  }

  /**
   * Alias of gettext().
   *
   * @param env
   * @param message
   */
  public static StringValue _(Env env,
                          StringValue message)
  {
    return gettext(env, message);
  }

  /**
   * Returns translated string from current domain and default category.
   *
   * @param env
   * @param message
   */
  public static StringValue gettext(Env env,
                          StringValue message)
  {
    return getGettext(env).gettext(env, message);
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
  public static StringValue ngettext(Env env,
                          StringValue msgid1,
                          StringValue msgid2,
                          int n)
  {
    return getGettext(env).ngettext(env, msgid1, msgid2, n);
  }

  /**
   * Changes the current domain.
   *
   * @param env
   * @param text_domain
   * @return name of current domain after change.
   */
  public static StringValue textdomain(Env env,
                          @Optional Value text_domain)
  {
    return getGettext(env).textdomain(env, text_domain);
  }

  private static Gettext getGettext(Env env)
  {
    Object val = env.getSpecialValue("caucho.gettext");

    if (val == null) {
      Gettext gettext = new Gettext();
      env.setSpecialValue("caucho.gettext", gettext);

      return gettext;
    }

    return (Gettext)val;
  }

}
