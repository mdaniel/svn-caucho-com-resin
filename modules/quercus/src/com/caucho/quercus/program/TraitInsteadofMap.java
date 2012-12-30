/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
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
 * @author Nam Nguyen
 */

package com.caucho.quercus.program;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.caucho.quercus.env.StringValue;

public class TraitInsteadofMap
{
  public static final int USE_NEW_TRAIT = 1;
  public static final int USE_EXISTING_TRAIT = 0;
  public static final int NULL = -1;

  private final HashMap<StringValue,TraitInsteadof> _insteadofMap
    = new HashMap<StringValue,TraitInsteadof>();

  public void put(StringValue funName,
                  String traitName,
                  String insteadofTraitName)
  {
    TraitInsteadof insteadof
      = new TraitInsteadof(traitName, insteadofTraitName);

    _insteadofMap.put(funName.toLowerCase(Locale.ENGLISH), insteadof);
  }

  public int get(StringValue funName,
                 String newTraitName,
                 String existingTraitName)
  {
    TraitInsteadof insteadof
      = _insteadofMap.get(funName.toLowerCase(Locale.ENGLISH));

    if (insteadof == null) {
      return NULL;
    }
    else {
      return insteadof.isInsteadOf(newTraitName, existingTraitName);
    }
  }

  public Set<Map.Entry<StringValue,TraitInsteadof>> insteadOfEntrySet()
  {
    return _insteadofMap.entrySet();
  }

  static class TraitInsteadof {
    private final String _traitName;
    private final String _insteadofTraitName;

    public TraitInsteadof(String traitName, String insteadofTraitName)
    {
      _traitName = traitName;
      _insteadofTraitName = insteadofTraitName;
    }

    public String getTraitName()
    {
      return _traitName;
    }

    public String getInsteadofTraitName()
    {
      return _insteadofTraitName;
    }

    public int isInsteadOf(String newTraitName, String existingTraitName)
    {
      if (_traitName.equalsIgnoreCase(newTraitName)
          && _insteadofTraitName.equalsIgnoreCase(existingTraitName)) {
        return USE_NEW_TRAIT;
      }
      else if (_traitName.equalsIgnoreCase(existingTraitName)
               && _insteadofTraitName.equalsIgnoreCase(newTraitName)) {
        return USE_EXISTING_TRAIT;
      }
      else {
        return NULL;
      }
    }
  }
}
