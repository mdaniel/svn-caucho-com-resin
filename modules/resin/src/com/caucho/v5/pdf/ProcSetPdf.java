/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * This file is part of Resin(R)
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

package com.caucho.v5.pdf;

/**
 * pdf object oriented API facade
 */
public class ProcSetPdf {

  private int _id;
  private String _set = "";

  ProcSetPdf()
  {
  }

  void setId(int id)
  {
    _id = id;
  }

  public int getId()
  {
    return _id;
  }

  public void add(String proc)
  {
    if (_set.indexOf(proc) >= 0)
      return;

    if (_set.length() > 0)
      _set = _set + ' ' + proc;
    else
      _set = proc;
  }

  String getResourceName()
  {
    return "/ProcSet";
  }
  
  String getResource()
  {
    return ("[" + _set + "]");
  }

  public int hashCode()
  {
    return _set.hashCode();
  }

  public boolean equals(Object o)
  {
    if (this == o)
      return true;
    else if (! (o instanceof ProcSetPdf))
      return false;

    ProcSetPdf set = (ProcSetPdf) o;

    return _set.equals(set._set);
  }

  public String toString()
  {
    return "PDFProcSet[" + _set + "]";
  }
}
