/*
 * Copyright (c) 1998-2010 Caucho Technology -- all rights reserved
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

package com.caucho.cloud.topology;


/**
 * The triad owner is the triad ownership for a hashed item. Each owner has
 * a primary, secondary and tertiary owner.
 * 
 * The first three servers are the triad servers.
 */
public enum TriadOwner
{
  A_B {
    @Override
    public int getPrimary() { return 0; }
    
    @Override
    public int getSecondary() { return 1; }
    
    @Override
    public int getTertiary() { return 2; }
  },
  
  B_C {
    @Override
    public int getPrimary() { return 1; }
    
    @Override
    public int getSecondary() { return 2; }
    
    @Override
    public int getTertiary() { return 0; }
  },
  
  C_A {
    @Override
    public int getPrimary() { return 2; }
    
    @Override
    public int getSecondary() { return 0; }
    
    @Override
    public int getTertiary() { return 1; }
  },
  
  A_C {
    @Override
    public int getPrimary() { return 0; }
    
    @Override
    public int getSecondary() { return 2; }
    
    @Override
    public int getTertiary() { return 1; }
  },
  
  B_A {
    @Override
    public int getPrimary() { return 1; }
    
    @Override
    public int getSecondary() { return 0; }
    
    @Override
    public int getTertiary() { return 2; }
  },
  
  C_B {
    @Override
    public int getPrimary() { return 2; }
    
    @Override
    public int getSecondary() { return 1; }
    
    @Override
    public int getTertiary() { return 0; }
  };
  
  public final static TriadOwner []OWNER_VALUES
    = TriadOwner.class.getEnumConstants();
  
  abstract public int getPrimary();
  abstract public int getSecondary();
  abstract public int getTertiary();

  /**
   * Returns the owner for an index
   */
  public static TriadOwner getOwner(long index)
  {
    return OWNER_VALUES[(int) ((index & Long.MAX_VALUE) % OWNER_VALUES.length)];
  }
}
