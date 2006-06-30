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

package javax.xml.stream;

/**
 * This interface declares the constants used in this API. Numbers in the range
 * 0 to 256 are reserved for the specification, user defined events must use
 * event codes outside that range.
 */
public interface XMLStreamConstants {

  /**
   * Indicates an event is an attribute See Also:Attribute, Constant Field
   * Values
   */
  static final int ATTRIBUTE=10;


  /**
   * Indicates an event is a CDATA section See Also:Characters, Constant Field
   * Values
   */
  static final int CDATA=12;


  /**
   * Indicates an event is characters See Also:Characters, Constant Field Values
   */
  static final int CHARACTERS=4;


  /**
   * Indicates an event is a comment See Also:Comment, Constant Field Values
   */
  static final int COMMENT=5;


  /**
   * Indicates an event is a DTD See Also:DTD, Constant Field Values
   */
  static final int DTD=11;


  /**
   * Indicates an event is an end document See Also:EndDocument, Constant Field
   * Values
   */
  static final int END_DOCUMENT=8;


  /**
   * Indicates an event is an end element See Also:EndElement, Constant Field
   * Values
   */
  static final int END_ELEMENT=2;


  /**
   * Indicates a Entity Declaration See Also:NotationDeclaration, Constant
   * Field Values
   */
  static final int ENTITY_DECLARATION=15;


  /**
   * Indicates an event is an entity reference See Also:EntityReference,
   * Constant Field Values
   */
  static final int ENTITY_REFERENCE=9;


  /**
   * Indicates the event is a namespace declaration See Also:Namespace,
   * Constant Field Values
   */
  static final int NAMESPACE=13;


  /**
   * Indicates a Notation See Also:NotationDeclaration, Constant Field Values
   */
  static final int NOTATION_DECLARATION=14;


  /**
   * Indicates an event is a processing instruction See
   * Also:ProcessingInstruction, Constant Field Values
   */
  static final int PROCESSING_INSTRUCTION=3;


  /**
   * The characters are white space (see [XML], 2.10 "White Space Handling").
   * Events are only reported as SPACE if they are ignorable white space.
   * Otherwise they are reported as CHARACTERS. See Also:Characters, Constant
   * Field Values
   */
  static final int SPACE=6;


  /**
   * Indicates an event is a start document See Also:StartDocument, Constant
   * Field Values
   */
  static final int START_DOCUMENT=7;


  /**
   * Indicates an event is a start element See Also:StartElement, Constant
   * Field Values
   */
  static final int START_ELEMENT=1;

}

