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

package javax.xml.bind.util;
import javax.xml.bind.*;

/**
 * ValidationEventHandler implementation that collects all events. To use this
 * class, create a new instance and pass it to the setEventHandler method of
 * the Validator, Unmarshaller, Marshaller class. After the call to validate or
 * unmarshal completes, call the getEvents method to retrieve all the reported
 * errors and warnings. Since: JAXB1.0 Version: $Revision: 1.2 $ Author:
 * Kohsuke Kawaguchi, Sun Microsystems, Inc.Ryan Shoemaker, Sun Microsystems,
 * Inc.Joe Fialli, Sun Microsystems, Inc. See Also:Validator,
 * ValidationEventHandler, ValidationEvent, ValidationEventLocator
 */
public class ValidationEventCollector implements ValidationEventHandler {
  public ValidationEventCollector()
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Return an array of ValidationEvent objects containing a copy of each of
   * the collected errors and warnings.
   */
  public ValidationEvent[] getEvents()
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Description copied from interface: Receive notification of a validation
   * warning or error. The ValidationEvent will have a embedded in it that
   * indicates where the error or warning occurred. If an unchecked runtime
   * exception is thrown from this method, the JAXB provider will treat it as
   * if the method returned false and interrupt the current unmarshal,
   * validate, or marshal operation.
   */
  public boolean handleEvent(ValidationEvent event)
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Returns true if this event collector contains at least one ValidationEvent.
   */
  public boolean hasEvents()
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Clear all collected errors and warnings.
   */
  public void reset()
  {
    throw new UnsupportedOperationException();
  }

}

