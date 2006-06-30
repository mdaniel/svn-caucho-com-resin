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

package javax.xml.bind.annotation.adapters;
import java.lang.annotation.*;

/**
 * Use an adapter that implements XmlAdapter for custom marshaling. Usage: The
 * @XmlJavaTypeAdapter annotation can be used with the following program
 * elements: a JavaBean property field parameter package from within
 * XmlJavaTypeAdapters When @XmlJavaTypeAdapter annotation is defined on a
 * class, it applies to all references to the class. When @XmlJavaTypeAdapter
 * annotation is defined at the package level it applies to all references from
 * within the package to @XmlJavaTypeAdapter.type(). When @XmlJavaTypeAdapter
 * annotation is defined on the field, property or parameter, then the
 * annotation applies to the field, property or the parameter only. A
 * @XmlJavaTypeAdapter annotation on a field, property or parameter overrides
 * the @XmlJavaTypeAdapter annotation associated with the class being
 * referenced by the field, property or parameter. A @XmlJavaTypeAdapter
 * annotation on a class overrides the @XmlJavaTypeAdapter annotation specified
 * at the package level for that class. This annotation can be used with the
 * following other annotations: XmlElement, XmlAttribute, XmlElementRef,
 * XmlElementRefs, XmlAnyElement. This can also be used at the package level
 * with the following annotations: XmlAccessorType, XmlSchema, XmlSchemaType,
 * XmlSchemaTypes. Example: See example in XmlAdapter Since: JAXB2.0 Version:
 * $Revision: 1.10 $ Author: Sekhar Vajjhala, Sun Microsystems Inc. Kohsuke
 * Kawaguchi, Sun Microsystems Inc. See Also:XmlAdapter
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PACKAGE,
           ElementType.FIELD,
           ElementType.METHOD,
           ElementType.TYPE,
           ElementType.PARAMETER})
public @interface XmlJavaTypeAdapter {

    Class<? extends XmlAdapter> value();
    Class type();

}

