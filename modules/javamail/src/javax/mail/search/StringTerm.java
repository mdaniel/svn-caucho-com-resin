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
 * This class implements the match method for Strings. The current
 * implementation provides only for substring matching. We could add
 * comparisons (like strcmp ...).  See Also:Serialized Form
 */
public abstract class StringTerm extends SearchTerm {

  /**
   * Ignore case when comparing?
   */
  protected boolean ignoreCase;

  /**
   * The pattern.
   */
  protected String pattern;

  /**
   * XXX: remove this once subclass constructors are written
   */
  StringTerm()
  {
    throw new Error("this method should be removed; it was a hack " +
		    "to get half-written code to compile");
  }

  protected StringTerm(String pattern)
  {
    throw new UnsupportedOperationException("not implemented");
  }

  protected StringTerm(String pattern, boolean ignoreCase)
  {
    throw new UnsupportedOperationException("not implemented");
  }

  /**
   * Equality comparison.
   */
  public boolean equals(Object obj)
  {
    throw new UnsupportedOperationException("not implemented");
  }

  /**
   * Return true if we should ignore case when matching.
   */
  public boolean getIgnoreCase()
  {
    throw new UnsupportedOperationException("not implemented");
  }

  /**
   * Return the string to match with.
   */
  public String getPattern()
  {
    throw new UnsupportedOperationException("not implemented");
  }

  /**
   * Compute a hashCode for this object.
   */
  public int hashCode()
  {
    throw new UnsupportedOperationException("not implemented");
  }

  protected boolean match(String s)
  {
    throw new UnsupportedOperationException("not implemented");
  }

}
