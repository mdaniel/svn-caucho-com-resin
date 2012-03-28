/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
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

package com.caucho.quercus.env;

import com.caucho.quercus.QuercusContext;
import com.caucho.quercus.function.AbstractFunction;
import com.caucho.quercus.page.QuercusPage;
import com.caucho.quercus.program.ClassDef;
import com.caucho.util.L10N;
import com.caucho.vfs.Path;

import java.util.HashMap;
import java.util.logging.Logger;

/**
 * Represents a compiled function
 */
public class ProfilePage extends QuercusPage {
  private static final Logger log
    = Logger.getLogger(ProfilePage.class.getName());
  private static final L10N L = new L10N(ProfilePage.class);

  private final QuercusPage _page;
  private final int _id;
  
  public ProfilePage(QuercusPage page, int id)
  {
    _page = page;
    _id = id;
  }

  /**
   * Returns true if the page is modified.
   */
  public boolean isModified()
  {
    return _page.isModified();
  }

  /**
   * Returns the page's path.
   */
  public Path getSelfPath(Env env)
  {
    return _page.getSelfPath(env);
  }
  
  /**
   * Finds a function.
   */
  public AbstractFunction findFunction(String name)
  {
    return _page.findFunction(name);
  }

  /**
   * Finds a function.
   */
  public ClassDef findClass(String name)
  {
    return _page.findClass(name);
  }

  /**
   * Returns the class map.
   */
  public HashMap<String,ClassDef> getClassMap()
  {
    return _page.getClassMap();
  }
  
  /**
   * Returns the pwd according to the source page.
   */
  public Path getPwd(Env env)
  {
    return _page.getPwd(env);
  }

  /**
   * Execute the program
   *
   * @param env the calling environment
   */
  public Value execute(Env env)
  {
    long startTime = System.nanoTime();

    env.pushProfile(_id);

    try {
      return _page.execute(env);
    } finally {
      env.popProfile(System.nanoTime() - startTime);
    }
  }

  /**
   * Initialize the program
   *
   * @param quercus the owning engine
   */
  public void init(QuercusContext quercus)
  {
    _page.init(quercus);
  }

  /**
   * Initialize the environment
   *
   * @param quercus the owning engine
   */
  public void init(Env env)
  {
    _page.init(env);
  }

  /**
   * Imports the page definitions.
   */
  public void importDefinitions(Env env)
  {
    _page.importDefinitions(env);
  }

  public String toString()
  {
    return getClass().getSimpleName() + "[" + _page + "," + _id + "]";
  }
}

