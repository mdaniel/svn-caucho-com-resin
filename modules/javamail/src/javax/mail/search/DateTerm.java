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
 */
public abstract class DateTerm extends ComparisonTerm {

  /**
   * The date.
   */
  protected Date date;

  /**
   * Constructor.
   */
  protected DateTerm(int comparison, Date date)
  {
    super(comparison);

    this.date = date;
  }

  /**
   * Equality comparison.
   */
  public boolean equals(Object obj)
  {
    if (! (obj instanceof DateTerm))
      return false;

    return super.equals(obj);
  }

  /**
   * Return the type of comparison.
   */
  public int getComparison()
  {
    return comparison;
  }

  /**
   * Return the Date to compare with.
   */
  public Date getDate()
  {
    return date;
  }

  /**
   * Compute a hashCode for this object.
   */
  public int hashCode()
  {
    int hash = super.hashCode();

    hash = hash * 65521 + date.hashCode();

    return hash;
  }

  /**
   * The date comparison method.
   */
  protected boolean match(Date d)
  {
    int comp = d.compareTo(date);

    switch(comparison)
      {
	case EQ:
	  return comp == 0;

	case GE:
	  return comp >= 0;

	case GT:
	  return comp >  0;

	case LE:
	  return comp <= 0;

	case LT:
	  return comp <  0;

	case NE:
	  return comp != 0;
      }
    return false;
  }

}
