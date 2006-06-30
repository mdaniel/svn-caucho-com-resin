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
 * An interface for the start document event Version: 1.0 Author: Copyright (c)
 * 2003 by BEA Systems. All Rights Reserved.
 */
public interface StartDocument extends XMLEvent {

  /**
   * Returns true if CharacterEncodingScheme was set in the encoding
   * declaration of the document
   */
  abstract boolean encodingSet();


  /**
   * Returns the encoding style of the XML data
   */
  abstract String getCharacterEncodingScheme();


  /**
   * Returns the system ID of the XML data
   */
  abstract String getSystemId();


  /**
   * Returns the version of XML of this XML stream
   */
  abstract String getVersion();


  /**
   * Returns if this XML is standalone
   */
  abstract boolean isStandalone();


  /**
   * Returns true if the standalone attribute was set in the encoding
   * declaration of the document.
   */
  abstract boolean standaloneSet();

}

