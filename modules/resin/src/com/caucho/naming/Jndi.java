/*
 * Copyright (c) 1998-2004 Caucho Technology -- all rights reserved
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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.naming;

import java.util.logging.*;

import javax.naming.*;

import com.caucho.util.L10N;
import com.caucho.log.Log;

/**
 * Static utility functions.
 */
public class Jndi {
  private static Logger log = Log.open(Jndi.class);
  private static L10N L = new L10N(Jndi.class);

  public static void bindDeepShort(String name, Object obj)
    throws NamingException
  {
    if (name.startsWith("java:comp"))
      bindDeep(name, obj);
    else
      bindDeep("java:comp/env/" + name, obj);
  }

  public static void bindDeep(String name, Object obj)
    throws NamingException
  {
    bindDeep(new InitialContext(), name, obj, name);
  }
  
  public static void bindDeep(Context context, String name,
			      Object obj, String fullName)
    throws NamingException
  {
    NameParser parser = context.getNameParser("");
    Name parsedName = parser.parse(name);

    if (parsedName.size() == 1) {
      if (context.lookup(name) != null) {
        log.warning(L.l("`{0}' is a conflicting JNDI resource for `{1}'",
                        fullName, obj));
      }
      
      context.rebind(name, obj);
      return;
    }

    Object sub = context.lookup(parsedName.get(0));

    if (sub == null)
      sub = context.createSubcontext(parsedName.get(0));
      
    if (sub instanceof Context)
      bindDeep((Context) sub, parsedName.getSuffix(1).toString(), obj,
               fullName);

    else
      throw new NamingException(L.l("`{0}' is an invalid JNDI name because `{1} is not a Context.  One of the subcontexts is not a Context as expected.",
                                    fullName, sub));
  }

  /**
   * Binds the object into JNDI without warnings if an old
   * object exists.  The name may be a full name or the short
   * form.
   */
  public static void rebindDeepShort(String name, Object obj)
    throws NamingException
  {
    if (name.startsWith("java:comp"))
      rebindDeep(name, obj);
    else
      rebindDeep("java:comp/env/" + name, obj);
  }

  /**
   * Returns the full name.
   */
  public static String getFullName(String shortName)
  {
    if (shortName.startsWith("java:comp"))
      return shortName;
    else
      return "java:comp/env/" + shortName;
  }

  /**
   * Binds the object into JNDI without warnings if an old
   * object exists, using the full JNDI name.
   */
  public static void rebindDeep(String name, Object obj)
    throws NamingException
  {
    rebindDeep(new InitialContext(), name, obj, name);
  }

  /**
   * Binds the object into JNDI without warnings if an old
   * object exists.
   */
  public static void rebindDeep(Context context, String name,
				Object obj, String fullName)
    throws NamingException
  {
    NameParser parser = context.getNameParser("");
    Name parsedName = parser.parse(name);

    if (parsedName.size() == 1) {
      context.rebind(name, obj);
      return;
    }

    Object sub = context.lookup(parsedName.get(0));

    if (sub == null)
      sub = context.createSubcontext(parsedName.get(0));
      
    if (sub instanceof Context)
      rebindDeep((Context) sub, parsedName.getSuffix(1).toString(), obj,
               fullName);

    else
      throw new NamingException(L.l("`{0}' is an invalid JNDI name because `{1} is not a Context.  One of the subcontexts is not a Context as expected.",
                                    fullName, sub));
  }

  // For EL
  public static Object lookup(String name)
  {
    try {
      Object value = new InitialContext().lookup(name);
        
      if (value != null)
        return value;
    } catch (NamingException e) {
    }

    if (! name.startsWith("java:comp/env")) {
      try {
        Object value = new InitialContext().lookup("java:comp/env/" + name);
        
        if (value != null)
          return value;
      } catch (NamingException e) {
      }
    }

    return null;
  }

  private Jndi() {}
}

