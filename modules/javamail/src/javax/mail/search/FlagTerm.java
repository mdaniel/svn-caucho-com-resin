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
 */
public final class FlagTerm extends SearchTerm {

  /**
   * Flags object containing the flags to test.
   */
  protected Flags flags;

  /**
   * Indicates whether to test for the presence or absence of the
   * specified Flag.
   */
  protected boolean set;

  /**
   * Constructor.  flags - Flags object containing the flags to check
   * forset - the flag setting to check for
   */
  public FlagTerm(Flags flags, boolean set)
  {
    this.flags = flags;
    this.set = set;
  }

  /**
   * Equality comparison.
   */
  public boolean equals(Object obj)
  {
    if (! (obj instanceof FlagTerm))
      return false;

    FlagTerm flagTerm = (FlagTerm)obj;
    if (! flags.equals(flagTerm.getFlags()))
      return false;

    if (set != flagTerm.getTestSet())
      return false;

    return true;
  }

  /**
   * Return the Flags to test.
   */
  public Flags getFlags()
  {
    return flags;
  }

  /**
   * Return true if testing whether the flags are set.
   */
  public boolean getTestSet()
  {
    return set;
  }

  /**
   * Compute a hashCode for this object.
   */
  public int hashCode()
  {
    int hash = flags.hashCode();

    if (set) hash = ~hash;

    return hash;
  }

  /**
   * The comparison method.
   */
  public boolean match(Message msg)
  {
    try {
      Flags f = msg.getFlags();
      
      Flags.Flag[] systemFlags = flags.getSystemFlags();
      String[]     userFlags   = flags.getUserFlags();
      
      for(int i=0; i<systemFlags.length; i++)
	if (f.contains(systemFlags[i]) != set)
	  return false;
      
      for(int i=0; i<userFlags.length; i++)
	if (f.contains(userFlags[i]) != set)
	  return false;
      
      return true;
    }

    catch (MessagingException e) {
      throw new RuntimeException(e);
    }
  }

}
