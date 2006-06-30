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
 * This is an element marker interface. Under certain circumstances, it is
 * necessary for the binding compiler to generate derived java content classes
 * that implement this interface. In those cases, client applications must
 * supply element instances rather than types of elements. For more detail, see
 * section 5.7 "Element Declaration" and 5.7.1 "Bind to Java Element Interface"
 * of the specification. Since: JAXB1.0 Version: $Revision: 1.1 $ Author: Ryan
 * Shoemaker, Sun Microsystems, Inc.Kohsuke Kawaguchi, Sun Microsystems,
 * Inc.Joe Fialli, Sun Microsystems, Inc.
 */
public interface Element {
}

