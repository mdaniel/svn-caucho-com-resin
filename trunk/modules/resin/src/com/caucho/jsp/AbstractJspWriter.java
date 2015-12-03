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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.jsp;

import com.caucho.vfs.FlushBuffer;

import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.BodyContent;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.logging.Logger;

/**
 * A buffered JSP writer encapsulating a Writer.
 */
abstract class AbstractJspWriter extends BodyContent implements FlushBuffer {
  private JspWriter _parent;

  /**
   * Creates a new QJspWriter
   */
  AbstractJspWriter()
  {
    super(null);

    this.autoFlush = true;
  }

  /**
   * Creates a new QJspWriter
   */
  AbstractJspWriter(int bufferSize, boolean isAutoFlush)
  {
    super(null);

    this.bufferSize = bufferSize;
    this.autoFlush = isAutoFlush;
  }

  /**
   * Sets the parent.
   */
  public void setParent(JspWriter parent)
  {
    _parent = parent;
  }

  /**
   * Returns the parent JSP writer.
   */
  @Override
  public final JspWriter getEnclosingWriter()
  {
    return _parent;
  }

  @Override
  public void writeOut(Writer writer) throws IOException
  {
    throw new UnsupportedOperationException();
  }

  @Override
  public String getString()
  {
    throw new UnsupportedOperationException();
  }

  @Override
  public Reader getReader()
  {
    throw new UnsupportedOperationException();
  }

  @Override
  public void clearBody()
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Returns the autoFlush flag.
   */
  @Override
  final public boolean isAutoFlush()
  {
    return this.autoFlush;
  }

  /**
   * Pops the enclosing writer.
   */
  AbstractJspWriter popWriter()
  {
    return (AbstractJspWriter) _parent;
  }
}
