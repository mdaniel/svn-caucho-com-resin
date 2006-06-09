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
 * This class implements comparisons for Message Flags.
 * See Also:Serialized Form
 */
public final class FlagTerm extends SearchTerm {

  /**
   * Flags object containing the flags to test.
   */
  protected Flags flags;

  /**
   * Indicates whether to test for the presence or absence of the
   * specified Flag. If true, then test whether all the specified
   * flags are present, else test whether all the specified flags are
   * absent.
   */
  protected boolean set;

  /**
   * Constructor.  flags - Flags object containing the flags to check
   * forset - the flag setting to check for
   */
  public FlagTerm(Flags flags, boolean set)
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
   * Return the Flags to test.
   */
  public Flags getFlags()
  {
    throw new UnsupportedOperationException("not implemented");
  }

  /**
   * Return true if testing whether the flags are set.
   */
  public boolean getTestSet()
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

  /**
   * The comparison method.
   */
  public boolean match(Message msg)
  {
    throw new UnsupportedOperationException("not implemented");
  }

}
