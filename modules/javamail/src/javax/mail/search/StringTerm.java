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
 * Substring matching.
 */
public abstract class StringTerm extends SearchTerm {

  /**
   * Ignore case when comparing
   */
  protected boolean ignoreCase;

  /**
   * The pattern.
   */
  protected String pattern;

  protected StringTerm(String pattern)
  {
    this(pattern, false);
  }

  protected StringTerm(String pattern, boolean ignoreCase)
  {
    this.pattern = pattern;
    this.ignoreCase = ignoreCase;
  }

  /**
   * Equality comparison.
   */
  public boolean equals(Object obj)
  {
    if (! (obj instanceof StringTerm)) return false;
    return
      ((StringTerm)obj).pattern.equalsIgnoreCase(pattern);
  }

  /**
   * Return true if we should ignore case when matching.
   */
  public boolean getIgnoreCase()
  {
    return ignoreCase;
  }

  /**
   * Return the string to match with.
   */
  public String getPattern()
  {
    return pattern;
  }

  /**
   * Compute a hashCode for this object.
   */
  public int hashCode()
  {
    int hash = pattern.hashCode();

    hash = 65521 * hash + (ignoreCase ? 1 : 0);

    return hash;
  }

  protected boolean match(String s)
  {

    if (ignoreCase) {
      s = s.toLowerCase();
      return s.indexOf(pattern.toLowerCase()) != -1;
    }

    else {
      return s.indexOf(pattern) != -1;
    }

  }

}
