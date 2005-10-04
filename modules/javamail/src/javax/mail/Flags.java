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
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package javax.mail;

import java.util.Hashtable;
import java.util.Enumeration;
import java.util.ArrayList;

/**
 * Represents a mail message flags.
 */
public class Flags implements Cloneable, java.io.Serializable {
  private int system_flags;
  private Hashtable user_flags;
  
  /**
   * Create a flag set.
   */
  public Flags()
  {
  }

  /**
   * Create a new flag set.
   */
  public Flags(Flags flags)
  {
    this.system_flags = flags.system_flags;
    
    if (flags.user_flags != null)
      this.system_flags = new Hashtable(flags.user_flags);
  }

  /**
   * Create a new flag from the system flag
   */
  public Flags(Flag flag)
  {
    add(flag);
  }

  /**
   * Create a new flag from the user flag
   */
  public Flags(String flag)
  {
    add(flag);
  }

  /**
   * Returns the system flags.
   */
  public Flag []getSystemFlags()
  {
    ArrayList flagList = new ArrayList();

    if ((this.system_flags & Flag.ANSWERED.getMask()) != 0)
      flagList.add(Flag.ANSWERED);
    if ((this.system_flags & Flag.DELETED.getMask()) != 0)
      flagList.add(Flag.DELETED);
    if ((this.system_flags & Flag.DRAFT.getMask()) != 0)
      flagList.add(Flag.DRAFT);
    if ((this.system_flags & Flag.FLAGGED.getMask()) != 0)
      flagList.add(Flag.FLAGGED);
    if ((this.system_flags & Flag.RECENT.getMask()) != 0)
      flagList.add(Flag.RECENT);
    if ((this.system_flags & Flag.SEEN.getMask()) != 0)
      flagList.add(Flag.SEEN);
    if ((this.system_flags & Flag.USER.getMask()) != 0)
      flagList.add(Flag.USER);

    Flag []flags = new Flag[flagList.size()];

    flagList.toArray(flags);

    return flags;
  }

  /**
   * Returns the user flags.
   */
  public String []getUserFlags()
  {
    if (this.user_flags == null || this.user_flags.size() == 0)
      return new String[0];

    ArrayList flagList = new ArrayList();

    Enumeration e = this.user_flags.keys();
    while (e.hasMoreElements()) {
      flagList.add(e.nextElement());
    }

    String []flags = new String[flagList.size()];

    flagList.toArray(flags);

    return flags;
  }

  /**
   * Adds a user flag.
   */
  public void add(String flag)
  {
    if (this.user_flags == null)
      this.user_flags = new Hashtable();

    this.user_flags.put(flag, flag);
  }

  /**
   * Adds a system flag.
   */
  public void add(Flag flag)
  {
    this.system_flags |= flag.getMask();
  }

  /**
   * Adds all the flags to this flag set
   */
  public void add(Flags flags)
  {
    this.system_flags |= flags.system_flags;

    if (flags.user_flags != null) {
      if (this.user_flags == null)
	this.user_flags = new Hashtable();

      this.user_flags.putAll(flags.user_flags);
    }
  }

  /**
   * Removes a user flag.
   */
  public void remove(String flag)
  {
    if (this.user_flags != null) {
      this.user_flags.remove(flag);
    }
  }

  /**
   * Removes a system flag.
   */
  public void remove(Flag flag)
  {
    this.system_flags &= ~ flag.getMask();
  }

  /**
   * Adds all the flags to this flag set
   */
  public void remove(Flags flags)
  {
    this.system_flags &= ~flags.system_flags;

    if (this.user_flags != null && flags.user_flags != null) {
      Enumeration e = flags.user_flags.keys();

      while (e.hasMoreElements()) {
	String name = (String) e.nextElement();

	this.user_flags.remove(name);
      }
    }
  }

  /**
   * Returns true if the user flag exists.
   */
  public boolean contains(String flag)
  {
    return (this.user_flags != null && this.user_flags.get(flag) != null);
  }

  /**
   * Returns true if the system flag is set.
   */
  public boolean contains(Flag flag)
  {
    return (this.system_flags & flag.getMask()) == flag.getMask();
  }

  /**
   * Adds all the flags to this flag set
   */
  public boolean contains(Flags flags)
  {
    if ((this.system_flags & flags.system_flags) != flags.system_flags)
      return false;

    if (flags.user_flags == null || flags.user_flags.size() == 0)
      return true;

    if (this.user_flags == null)
      return false;

    Enumeration e = flags.user_flags.keys();
    while (e.hasMoreElements()) {
      String name = (String) e.nextElement();

      if (this.user_flags.get(name) == null)
	return false;
    }

    return true;
  }

  /**
   * Return true for equality.
   */
  public boolean equals(Object o)
  {
    if (this == o)
      return true;
    else if (! (o instanceof Flags))
      return false;

    Flags flags = (Flags) o;

    if (this.system_flags != flags.system_flags)
      return false;

    if (this.user_flags == null && flags.user_flags == null)
      return true;

    return contains(flags) && flags.contains(this);
  }

  public int hashCode()
  {
    if (this.user_flags == null)
      return this.system_flags;
    else
      return this.system_flags * 65521 + this.user_flags.hashCode();
  }

  public Object clone()
  {
    return new Flags(this);
  }
    
  public static final class Flag {
    private final static Flag ANSWERED = new Flag(0x01);
    private final static Flag DELETED = new Flag(0x02);
    private final static Flag DRAFT = new Flag(0x04);
    private final static Flag FLAGGED = new Flag(0x08);
    private final static Flag RECENT = new Flag(0x10);
    private final static Flag SEEN = new Flag(0x20);
    private final static Flag USER = new Flag(0x40);
						  
    private final int _mask;

    private Flag(int mask)
    {
      _mask = mask;
    }

    private int getMask()
    {
      return _mask;
    }

    public String toString()
    {
      if (this == ANSWERED)
	return "answered";
      else if (this == DELETED)
	return "deleted";
      else if (this == DRAFT)
	return "draft";
      else if (this == FLAGGED)
	return "flagged";
      else if (this == RECENT)
	return "recent";
      else if (this == SEEN)
	return "seen";
      else if (this == USER)
	return "user";
      else
	return super.toString();
    }
  }
}
