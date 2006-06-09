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
import java.util.*;

/**
 * This class implements comparisons for Dates
 * See Also:Serialized Form
 */
public abstract class DateTerm extends ComparisonTerm {

  /**
   * The date.
   */
  protected Date date;

  /**
   * Constructor.
   * comparison - the comparison typedate - The Date to be compared against
   */
  protected DateTerm(int comparison, Date date)
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
   * Return the type of comparison.
   */
  public int getComparison()
  {
    throw new UnsupportedOperationException("not implemented");
  }

  /**
   * Return the Date to compare with.
   */
  public Date getDate()
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
   * The date comparison method.
   */
  protected boolean match(Date d)
  {
    throw new UnsupportedOperationException("not implemented");
  }

}
