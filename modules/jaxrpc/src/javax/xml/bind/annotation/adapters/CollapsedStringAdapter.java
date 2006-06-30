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

package javax.xml.bind.annotation.adapters;

/**
 * Built-in XmlAdapter to handle xs:token and its derived types. This adapter
 * removes leading and trailing whitespaces, then truncate any sequnce of tab,
 * CR, LF, and SP by a single whitespace character ' '. Since: JAXB 2.0 Author:
 * Kohsuke Kawaguchi
 */
public class CollapsedStringAdapter extends XmlAdapter<String,String> {
  public CollapsedStringAdapter()
  {
    throw new UnsupportedOperationException();
  }


  /**
   * returns true if the specified char is a white space character.
   */
  protected static boolean isWhiteSpace(char ch)
  {
    throw new UnsupportedOperationException();
  }


  /**
   * No-op. Just return the same string given as the parameter.
   */
  public String marshal(String s)
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Removes leading and trailing whitespaces of the string given as the
   * parameter, then truncate any sequnce of tab, CR, LF, and SP by a single
   * whitespace character ' '.
   */
  public String unmarshal(String text)
  {
    throw new UnsupportedOperationException();
  }

}

