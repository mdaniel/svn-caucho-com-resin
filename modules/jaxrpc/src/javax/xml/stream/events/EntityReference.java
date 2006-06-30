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
 * An interface for handling Entity events. This event reports entities that
 * have not been resolved and reports their replacement text unprocessed (if
 * available). This event will be reported if
 * javax.xml.stream.isReplacingEntityReferences is set to false. If
 * javax.xml.stream.isReplacingEntityReferences is set to true entity
 * references will be resolved transparently. Entities are handled in two
 * possible ways: (1) If javax.xml.stream.isReplacingEntityReferences is set to
 * true all entity references are resolved and reported as markup
 * transparently. (2) If javax.xml.stream.isReplacingEntityReferences is set to
 * false Entity references are reported as an EntityReference Event. Version:
 * 1.0 Author: Copyright (c) 2003 by BEA Systems. All Rights Reserved.
 */
public interface EntityReference extends XMLEvent {

  /**
   * Return the declaration of this entity.
   */
  abstract EntityDeclaration getDeclaration();


  /**
   * The name of the entity
   */
  abstract String getName();

}

