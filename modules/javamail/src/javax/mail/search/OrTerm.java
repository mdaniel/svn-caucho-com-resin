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
 * This class implements the logical AND operator on individual SearchTerms.
 */
public final class OrTerm extends SearchTerm {

  /**
   * The array of terms on which the AND operator should be applied.
   */
  protected SearchTerm[] terms;

  /**
   * Constructor that takes an array of SearchTerms.
   */
  public OrTerm(SearchTerm[] t)
  {
    this.terms = t;
  }

  /**
   * Constructor that takes two terms.
   */
  public OrTerm(SearchTerm t1, SearchTerm t2)
  {
    this(new SearchTerm[] { t1, t2 });
  }

  // XXX: test if Sun's OrTerm-equality is commutative
  /**
   * Equality comparison.
   */
  public boolean equals(Object obj)
  {
    if (! (obj instanceof OrTerm))
      return false;

    SearchTerm[] objTerms = ((OrTerm)obj).terms;

    if (terms.length != objTerms.length)
      return false;

    // XXX: need a test to determine if this should be .equals()

    OUTER: for(int i=0; i<terms.length; i++) {

      for(int j=0; j<objTerms.length; j++) {

	if (objTerms[j] == terms[i])
	  continue OUTER;
      }
      return false;

    }

    return true;
  }

  /**
   * Return the search terms.
   */
  public SearchTerm[] getTerms()
  {
    return terms;
  }

  /**
   * Compute a hashCode for this object.
   */
  public int hashCode()
  {
    int hash = 0;

    // we have to use a commutative operator here
    for(int i=0; i<terms.length; i++)
      hash ^= terms[i].hashCode();

    return hash;
  }

  /**
   * The AND operation.
   */
  public boolean match(Message msg)
  {
    for(int i=0; i<terms.length; i++)
      if (terms[i].match(msg))
	return true;

    return false;
  }

}
