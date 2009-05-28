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

package com.caucho.config.gen;

import com.caucho.java.JavaWriter;
import java.io.*;
import java.lang.reflect.*;
import java.util.*;

/**
 * Represents a filter for invoking a method
 */
abstract public class AbstractCallChain implements EjbCallChain {
  private EjbCallChain _next;

  AbstractCallChain(EjbCallChain next)
  {
    if (next == null)
      throw new NullPointerException();
    
    _next = next;
  }
  
  /**
   * Returns true if this filter will generate code.
   */
  abstract public boolean isEnhanced();

  /**
   * Introspects the method for the default values
   */
  public void introspect(ApiMethod apiMethod, ApiMethod implMethod)
  {
  }

  /**
   * Generates the static class prologue
   */
  public void generatePrologue(JavaWriter out, HashMap map)
    throws IOException
  {
    _next.generatePrologue(out, map);
  }

  /**
   * Generates initialization in the constructor
   */
  public void generateConstructor(JavaWriter out, HashMap map)
    throws IOException
  {
    _next.generateConstructor(out, map);
  }

  /**
   * Generates the method interception code
   */
  public void generateCall(JavaWriter out)
    throws IOException
  {
    _next.generateCall(out);
  }
}
