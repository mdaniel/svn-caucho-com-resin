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

package javax.script;

import java.io.Writer;
import java.io.Reader;

/**
 * Context information from an engine to the namespace.
 */
public interface ScriptContext {
  public static int ENGINE_SCOPE = 100;
  public static int GLOBAL_SCOPE = 200;

  /**
   * Associates a namespace with a scope.
   */
  public void setNamespace(Namespace namespace, int scope);

  /**
   * Returns the namespace associated with a scope.
   */
  public Namespace getNamespace(int scope);

  /**
   * Sets an attribute in a scope.
   */
  public void setAttribute(String name, Object value, int scope);

  /**
   * Gets an attribute in a scope.
   */
  public Object getAttribute(String name, int scope);

  /**
   * Gets an attribute in a scope.
   */
  public Object removeAttribute(String name, int scope);

  /**
   * Gets an attribute in the lowest scope.
   */
  public Object getAttribute(String name);

  /**
   * Gets an attribute in the lowest scope.
   */
  public int getAttributesScope(String name);

  /**
   * Returns a reader for input.
   */
  public Reader getReader();

  /**
   * Sets a reader for input.
   */
  public void setReader(Reader reader);

  /**
   * Returns the script's writer.
   */
  public Writer getWriter();

  /**
   * Sets the script's writer.
   */
  public void setWriter(Writer writer);

  /**
   * Returns the script's error writer.
   */
  public Writer getErrorWriter();

  /**
   * Returns the script's error writer.
   */
  public void setErrorWriter(Writer writer);
}

