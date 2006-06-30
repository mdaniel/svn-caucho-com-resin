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

package javax.xml.bind;

/**
 * This event indicates that a problem was encountered resolving an ID/IDREF.
 * Since: JAXB1.0 Version: $Revision: 1.1 $ Author: Ryan Shoemaker, Sun
 * Microsystems, Inc.Kohsuke Kawaguchi, Sun Microsystems, Inc.Joe Fialli, Sun
 * Microsystems, Inc. See Also:Validator, ValidationEventHandler
 */
public interface NotIdentifiableEvent extends ValidationEvent {
}

