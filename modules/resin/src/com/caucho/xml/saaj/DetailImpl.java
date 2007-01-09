/*
* Copyright (c) 1998-2007 Caucho Technology -- all rights reserved
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

package com.caucho.xml.saaj;

import javax.xml.namespace.*;
import javax.xml.soap.*;
import java.util.*;

public class DetailImpl extends SOAPElementImpl
                        implements Detail 
{
  private static final NameImpl DETAIL_NAME = new NameImpl("detail");

  private ArrayList<DetailEntry> _entries = new ArrayList<DetailEntry>();

  DetailImpl(SOAPFactory factory)
    throws SOAPException
  {
    this(factory, DETAIL_NAME);
  }

  DetailImpl(SOAPFactory factory, NameImpl name)
    throws SOAPException
  {
    super(factory, name);
  }

  public DetailEntry addDetailEntry(Name name) 
    throws SOAPException
  {
    DetailEntry entry = new DetailEntryImpl(_factory, NameImpl.fromName(name));

    _entries.add(entry);

    return entry;
  }

  public DetailEntry addDetailEntry(QName qname)
    throws SOAPException
  {
    DetailEntry entry = new DetailEntryImpl(_factory, qname);

    _entries.add(entry);

    return entry;
  }

  public Iterator getDetailEntries()
  {
    return _entries.iterator();
  }
}
