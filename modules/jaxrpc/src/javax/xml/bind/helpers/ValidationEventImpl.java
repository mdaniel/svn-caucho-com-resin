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

package javax.xml.bind.helpers;
import javax.xml.bind.*;

/**
 * Default implementation of the ValidationEvent interface. JAXB providers are
 * allowed to use whatever class that implements the ValidationEvent interface.
 * This class is just provided for a convenience. Since: JAXB1.0 Version:
 * $Revision: 1.1 $ Author: Kohsuke Kawaguchi, Sun Microsystems, Inc. See
 * Also:Validator, ValidationEventHandler, ValidationEvent,
 * ValidationEventLocator
 */
public class ValidationEventImpl implements ValidationEvent {

    /** XXX: remove this -- it is only here to help compiles */
    public ValidationEventImpl()
    {
    }

  /**
   * Create a new ValidationEventImpl. Parameters:_severity - The severity
   * value for this event. Must be one of ValidationEvent.WARNING,
   * ValidationEvent.ERROR, or ValidationEvent.FATAL_ERROR_message - The text
   * message for this event - may be null._locator - The locator object for
   * this event - may be null. Throws: IllegalArgumentException - if an illegal
   * severity field is supplied
   */
  public ValidationEventImpl(int _severity, String _message, ValidationEventLocator _locator)
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Create a new ValidationEventImpl. Parameters:_severity - The severity
   * value for this event. Must be one of ValidationEvent.WARNING,
   * ValidationEvent.ERROR, or ValidationEvent.FATAL_ERROR_message - The text
   * message for this event - may be null._locator - The locator object for
   * this event - may be null._linkedException - An optional linked exception
   * that may provide additional information about the event - may be null.
   * Throws: IllegalArgumentException - if an illegal severity field is supplied
   */
  public ValidationEventImpl(int _severity, String _message, ValidationEventLocator _locator, Throwable _linkedException)
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Description copied from interface: Retrieve the linked exception for this
   * warning/error.
   */
  public Throwable getLinkedException()
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Description copied from interface: Retrieve the locator for this
   * warning/error.
   */
  public ValidationEventLocator getLocator()
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Description copied from interface: Retrieve the text message for this
   * warning/error.
   */
  public String getMessage()
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Description copied from interface: Retrieve the severity code for this
   * warning/error. Must be one of ValidationError.WARNING,
   * ValidationError.ERROR, or ValidationError.FATAL_ERROR.
   */
  public int getSeverity()
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Set the linked exception field of this event.
   */
  public void setLinkedException(Throwable _linkedException)
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Set the locator object for this event.
   */
  public void setLocator(ValidationEventLocator _locator)
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Set the message field of this event.
   */
  public void setMessage(String _message)
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Set the severity field of this event.
   */
  public void setSeverity(int _severity)
  {
    throw new UnsupportedOperationException();
  }


  /**
   * Returns a string representation of this object in a format helpful to
   * debugging.
   */
  public String toString()
  {
    throw new UnsupportedOperationException();
  }

}

