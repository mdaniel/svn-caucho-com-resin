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

package javax.xml.soap;
import javax.xml.namespace.*;
import java.util.*;

/**
 * A container for DetailEntry objects. DetailEntry objects give detailed error
 * information that is application-specific and related to the SOAPBody object
 * that contains it. A Detail object, which is part of a SOAPFault object, can
 * be retrieved using the method SOAPFault.getDetail. The Detail interface
 * provides two methods. One creates a new DetailEntry object and also
 * automatically adds it to the Detail object. The second method gets a list of
 * the DetailEntry objects contained in a Detail object. The following code
 * fragment, in which sf is a SOAPFault object, gets its Detail object (d),
 * adds a new DetailEntry object to d, and then gets a list of all the
 * DetailEntry objects in d. The code also creates a Name object to pass to the
 * method addDetailEntry. The variable se, used to create the Name object, is a
 * SOAPEnvelope object. Detail d = sf.getDetail(); Name name =
 * se.createName("GetLastTradePrice", "WOMBAT",
 * "http://www.wombat.org/trader"); d.addDetailEntry(name); Iterator it =
 * d.getDetailEntries();
 */
public interface Detail extends SOAPFaultElement {

  /**
   * Creates a new DetailEntry object with the given name and adds it to this
   * Detail object.
   */
  abstract DetailEntry addDetailEntry(Name name) throws SOAPException;


  /**
   * Creates a new DetailEntry object with the given QName and adds it to this
   * Detail object. This method is the preferred over the one using Name.
   */
  abstract DetailEntry addDetailEntry(QName qname) throws SOAPException;


  /**
   * Gets an Iterator over all of the DetailEntrys in this Detail object.
   */
  abstract Iterator getDetailEntries();

}

