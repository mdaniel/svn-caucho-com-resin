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

package com.caucho.loader;

import com.caucho.config.ConfigException;
import com.caucho.lifecycle.Lifecycle;
import com.caucho.loader.enhancer.ByteCodeEnhancer;
import com.caucho.loader.enhancer.EnhancerRuntimeException;
import com.caucho.make.AlwaysModified;
import com.caucho.make.DependencyContainer;
import com.caucho.make.Make;
import com.caucho.make.MakeContainer;
import com.caucho.management.server.*;
import com.caucho.server.util.CauchoSystem;
import com.caucho.util.ByteBuffer;
import com.caucho.util.L10N;
import com.caucho.util.TimedCache;
import com.caucho.vfs.Dependency;
import com.caucho.vfs.JarPath;
import com.caucho.vfs.Path;
import com.caucho.vfs.ReadStream;

import javax.annotation.PostConstruct;
import java.io.FilePermission;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.*;
import java.lang.instrument.*;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Temporary class loader for class enhancement
 */
public class TempDynamicClassLoader extends DynamicClassLoader
{
  private DynamicClassLoader _owner;
  
  /**
   * Create a new class loader.
   *
   * @param parent parent class loader
   */
  public TempDynamicClassLoader(DynamicClassLoader owner)
  {
    super(owner.getParent());

    _owner = owner;
  }

  public ArrayList<Loader> getLoaders()
  {
    return _owner.getLoaders();
  }
}
