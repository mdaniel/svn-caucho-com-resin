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
 * An interface for handling Entity Declarations This interface is used to
 * record and report unparsed entity declarations. Version: 1.0 Author:
 * Copyright (c) 2003 by BEA Systems. All Rights Reserved.
 */
public interface EntityDeclaration extends XMLEvent {

  /**
   * Get the base URI for this reference or null if this information is not
   * available
   */
  abstract String getBaseURI();


  /**
   * The entity's name
   */
  abstract String getName();


  /**
   * The name of the associated notation.
   */
  abstract String getNotationName();


  /**
   * The entity's public identifier, or null if none was given
   */
  abstract String getPublicId();


  /**
   * The replacement text of the entity. This method will only return non-null
   * if this is an internal entity.
   */
  abstract String getReplacementText();


  /**
   * The entity's system identifier.
   */
  abstract String getSystemId();

}

