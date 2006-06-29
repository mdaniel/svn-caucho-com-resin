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
import org.w3c.dom.*;

/**
 * A representation of a node (element) in an XML document. This interface
 * extnends the standard DOM Node interface with methods for getting and
 * setting the value of a node, for getting and setting the parent of a node,
 * and for removing a node.
 */
public interface Node extends org.w3c.dom.Node {

  /**
   * Removes this Node object from the tree.
   */
  abstract void detachNode();


  /**
   * Returns the parent element of this Node object. This method can throw an
   * UnsupportedOperationException if the tree is not kept in memory.
   */
  abstract SOAPElement getParentElement();


  /**
   * Returns the value of this node if this is a Text node or the value of the
   * immediate child of this node otherwise. If there is an immediate child of
   * this Node that it is a Text node then it's value will be returned. If
   * there is more than one Text node then the value of the first Text Node
   * will be returned. Otherwise null is returned.
   */
  abstract String getValue();


  /**
   * Notifies the implementation that this Node object is no longer being used
   * by the application and that the implementation is free to reuse this
   * object for nodes that may be created later. Calling the method recycleNode
   * implies that the method detachNode has been called previously.
   */
  abstract void recycleNode();


  /**
   * Sets the parent of this Node object to the given SOAPElement object.
   */
  abstract void setParentElement(SOAPElement parent) throws SOAPException;


  /**
   * If this is a Text node then this method will set its value, otherwise it
   * sets the value of the immediate (Text) child of this node. The value of
   * the immediate child of this node can be set only if, there is one child
   * node and that node is a Text node, or if there are no children in which
   * case a child Text node will be created.
   */
  abstract void setValue(String value);

}

