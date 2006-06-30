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
 * An interface that contains information about a namespace. Namespaces are
 * accessed from a StartElement. Version: 1.0 Author: Copyright (c) 2003 by BEA
 * Systems. All Rights Reserved. See Also:StartElement
 */
public interface Namespace extends Attribute {

  /**
   * Gets the uri bound to the prefix of this namespace
   */
  abstract String getNamespaceURI();


  /**
   * Gets the prefix, returns "" if this is a default namespace declaration.
   */
  abstract String getPrefix();


  /**
   * returns true if this attribute declares the default namespace
   */
  abstract boolean isDefaultNamespaceDeclaration();

}

