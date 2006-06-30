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

package javax.xml.stream.events;

/**
 * This describes the interface to Characters events. All text events get
 * reported as Characters events. Content, CData and whitespace are all
 * reported as Characters events. IgnorableWhitespace, in most cases, will be
 * set to false unless an element declaration of element content is present for
 * the current element. Version: 1.0 Author: Copyright (c) 2003 by BEA Systems.
 * All Rights Reserved.
 */
public interface Characters extends XMLEvent {

  /**
   * Get the character data of this event
   */
  abstract String getData();


  /**
   * Returns true if this is a CData section. If this event is CData its event
   * type will be CDATA If javax.xml.stream.isCoalescing is set to true CDATA
   * Sections that are surrounded by non CDATA characters will be reported as a
   * single Characters event. This method will return false in this case.
   */
  abstract boolean isCData();


  /**
   * Return true if this is ignorableWhiteSpace. If this event is
   * ignorableWhiteSpace its event type will be SPACE.
   */
  abstract boolean isIgnorableWhiteSpace();


  /**
   * Returns true if this set of Characters is all whitespace. Whitespace
   * inside a document is reported as CHARACTERS. This method allows checking
   * of CHARACTERS events to see if they are composed of only whitespace
   * characters
   */
  abstract boolean isWhiteSpace();

}

