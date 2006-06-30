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

package javax.xml.stream;

/**
 * Provides information on the location of an event. All the information
 * provided by a Location is optional. For example an application may only
 * report line numbers. Version: 1.0 Author: Copyright (c) 2003 by BEA Systems.
 * All Rights Reserved.
 */
public interface Location {

  /**
   * Return the byte or character offset into the input source this location is
   * pointing to. If the input source is a file or a byte stream then this is
   * the byte offset into that stream, but if the input source is a character
   * media then the offset is the character offset. Returns -1 if there is no
   * offset available.
   */
  abstract int getCharacterOffset();


  /**
   * Return the column number where the current event ends, returns -1 if none
   * is available.
   */
  abstract int getColumnNumber();


  /**
   * Return the line number where the current event ends, returns -1 if none is
   * available.
   */
  abstract int getLineNumber();


  /**
   * Returns the public ID of the XML
   */
  abstract String getPublicId();


  /**
   * Returns the system ID of the XML
   */
  abstract String getSystemId();

}

