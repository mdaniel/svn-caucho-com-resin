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
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.loader.enhancer;

import java.io.IOException;

import java.util.ArrayList;

import java.util.logging.Logger;
import java.util.logging.Level;

import com.caucho.util.L10N;
import com.caucho.util.CauchoSystem;

import com.caucho.log.Log;

import com.caucho.config.ConfigException;

import com.caucho.loader.DynamicClassLoader;
import com.caucho.loader.EnvironmentClassLoader;
import com.caucho.loader.SimpleLoader;
import com.caucho.loader.Loader;
import com.caucho.loader.ClassEntry;

import com.caucho.bytecode.JavaClass;

import com.caucho.java.CompileClassNotFound;

import com.caucho.java.gen.GenClass;

/**
 * Class loader which enhances classes.
 */
abstract public class Enhancer {
  private static final L10N L = new L10N(Enhancer.class);
  private static final Logger log = Log.open(Enhancer.class);

  private static final int ACC_PUBLIC = 0x1;
  private static final int ACC_PRIVATE = 0x2;
  private static final int ACC_PROTECTED = 0x4;
  
  private String _baseSuffix = ""; // "__ResinBase"
  
  /**
   * Enhances the class.
   */
  protected void preEnhance(JavaClass baseClass)
    throws Exception
  {
  }
  
  /**
   * Enhances the class.
   */
  protected void enhance(GenClass genClass,
			 JavaClass baseClass,
			 String extClassName)
    throws Exception
  {
  }
  
  /**
   * Enhances the class.
   */
  protected void postEnhance(JavaClass baseClass)
    throws Exception
  {
  }
}
