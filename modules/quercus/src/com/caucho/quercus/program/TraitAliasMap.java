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

public class TraitAliasMap
{
  private final HashMap<StringValue,TraitAlias> _aliasMap
    = new HashMap<StringValue,TraitAlias>();

  public void put(StringValue funName,
                  StringValue funNameAlias,
                  String traitName)
  {
    TraitAlias alias = new TraitAlias(traitName, funNameAlias);

    _aliasMap.put(funName.toLowerCase(Locale.ENGLISH), alias);
  }

  public StringValue get(StringValue funName, String traitName)
  {
    TraitAlias alias = _aliasMap.get(funName.toLowerCase(Locale.ENGLISH));

    if (alias == null) {
      return null;
    }
    else if (alias.getTraitName().equalsIgnoreCase(traitName)) {
      return alias.getFunNameAlias();
    }
    else {
      return null;
    }
  }

  public Set<Map.Entry<StringValue,TraitAlias>> entrySet()
  {
    return _aliasMap.entrySet();
  }

  static class TraitAlias {
    private final String _traitName;
    private final StringValue _funNameAlias;

    public TraitAlias(String traitName, StringValue funNameAlias)
    {
      _traitName = traitName;
      _funNameAlias = funNameAlias;
    }

    public String getTraitName()
    {
      return _traitName;
    }

    public StringValue getFunNameAlias()
    {
      return _funNameAlias;
    }
  }
}
