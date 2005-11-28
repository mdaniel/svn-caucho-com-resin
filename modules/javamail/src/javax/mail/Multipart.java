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

package javax.mail;

import java.io.OutputStream;
import java.io.IOException;

import java.util.Vector;

/**
 * Represents a multipart message
 */
public abstract class Multipart {
  protected String contentType;
  protected Part parent;
  protected Vector parts;

  protected Multipart()
  {
  }

  /**
   * Sets the multipart from the data source.
   */
  protected void setMultipartDataSource(MultipartDataSource mp)
    throws MessagingException
  {
    // XXX:
    
    throw new UnsupportedOperationException();
  }

  /**
   * Returns the content type.
   */
  public String getContentType()
  {
    return this.contentType;
  }

  /**
   * Returns the number of body parts.
   */
  public int getCount()
    throws MessagingException
  {
    return this.parts.size();
  }

  /**
   * Returns the indexed body part.
   */
  public BodyPart getBodyPart(int index)
    throws MessagingException
  {
    return (BodyPart) this.parts.get(index);
  }

  /**
   * Returns the indexed body part.
   */
  public boolean removeBodyPart(BodyPart part)
    throws MessagingException
  {
    return this.parts.remove(part);
  }

  /**
   * Returns the indexed body part.
   */
  public boolean removeBodyPart(int index)
    throws MessagingException
  {
    return this.parts.remove(index) != null;
  }

  /**
   * Appends a body part.
   */
  public void addBodyPart(BodyPart part)
    throws MessagingException
  {
    this.parts.add(part);
  }

  /**
   * Appends a body part.
   */
  public void addBodyPart(BodyPart part, int index)
    throws MessagingException
  {
    this.parts.add(index, part);
  }

  /**
   * Writes the bytestream to the output stream.
   */
  public abstract void writeTo(OutputStream os)
    throws IOException, MessagingException;

  /**
   * Returns the container.
   */
  public Part getParent()
  {
    return this.parent;
  }

  /**
   * Sets the container.
   */
  public void setParent(Part parent)
  {
    this.parent = parent;
  }
}
