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

package javax.mail.search;
import javax.mail.*;

/**
 * This class implements comparisons for Message headers.
 */
public final class HeaderTerm extends StringTerm {

  /**
   * The name of the header.
   */
  protected String headerName;

  /**
   * Constructor.  headerName - The name of the headerpattern - The
   * pattern to search for
   */
  public HeaderTerm(String headerName, String pattern)
  {
    super(pattern);
    this.headerName = headerName;
  }

  /**
   * Equality comparison.
   */
  public boolean equals(Object obj)
  {
    if (! (obj instanceof HeaderTerm))
      return false;

    return super.equals(obj);
  }

  /**
   * Return the name of the header to compare with.
   */
  public String getHeaderName()
  {
    return headerName;
  }

  /**
   * Compute a hashCode for this object.
   */
  public int hashCode()
  {
    int hash = super.hashCode();

    hash = 65521 * hash + headerName.hashCode();

    return hash;
  }

  /**
   * The header match method.
   */
  public boolean match(Message msg)
  {
    // XXX: do tests -- how should this work?
    throw new UnsupportedOperationException("not implemented");
  }

}
