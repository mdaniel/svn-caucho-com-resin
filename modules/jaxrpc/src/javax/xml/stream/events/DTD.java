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
import java.util.*;

/**
 * This is the top level interface for events dealing with DTDs Version: 1.0
 * Author: Copyright (c) 2003 by BEA Systems. All Rights Reserved.
 */
public interface DTD extends XMLEvent {

  /**
   * Returns the entire Document Type Declaration as a string, including the
   * internal DTD subset. This may be null if there is not an internal subset.
   * If it is not null it must return the entire Document Type Declaration
   * which matches the doctypedecl production in the XML 1.0 specification
   */
  abstract String getDocumentTypeDeclaration();


  /**
   * Return a List containing the general entities, both external and internal,
   * declared in the DTD. This list must contain EntityDeclaration events.
   */
  abstract List getEntities();


  /**
   * Return a List containing the notations declared in the DTD. This list must
   * contain NotationDeclaration events.
   */
  abstract List getNotations();


  /**
   * Returns an implementation defined representation of the DTD. This method
   * may return null if no representation is available.
   */
  abstract Object getProcessedDTD();

}

