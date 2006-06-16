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

package javax.activation;

import java.util.HashMap;
import java.util.Enumeration;
import java.util.Collections;

import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.OutputStream;
import java.io.Serializable;
import java.io.Externalizable;
import java.io.IOException;

/**
 * Parameter list of a mime type.
 */
public class MimeType implements Serializable, Externalizable {
  private String _primary;
  private String _sub;
  private MimeTypeParameterList _parameters;

  /**
   * Default constructor.
   */  
  public MimeType()
  {
    _primary = "application";
    _sub = "*";
    _parameters = new MimeTypeParameterList();
  }

  /**
   * Constructor that builds a MimeType from a String.
   */
  public MimeType(String rawData)
    throws MimeTypeParseException
  {
    int slash = rawData.indexOf('/');

    if (slash == -1)
      throw new MimeTypeParseException("Unable to find a sub type.");

    _primary = rawData.substring(0, slash);
    _sub = rawData.substring(slash+1);

    int semicolon = _sub.indexOf(';');

    if (semicolon != -1) {
      _parameters =
	new MimeTypeParameterList(_sub.substring(semicolon));
      _sub = _sub.substring(0, semicolon);
    }
    else {
      _parameters = new MimeTypeParameterList();
    }
  }

  /**
   * Constructor that builds a MimeType with the given primary and sub
   * type but has an empty parameter list.
   */
  public MimeType(String primary, String sub)
    throws MimeTypeParseException
  {
    _primary = primary;
    _sub = sub;
    
    _parameters = new MimeTypeParameterList();
  }

  /**
   * Retrieve the primary type of this object.
   */
  public String getPrimaryType()
  {
    return _primary;
  }

  /**
   * Set the primary type for this object to the given String.
   */
  public void setPrimaryType(String primary)
    throws MimeTypeParseException
  {
    _primary = primary;
  }

  /**
   * Retrieve the sub type of this object.
   */
  public String getSubType()
  {
    return _sub;
  }

  /**
   * Set the sub type for this object to the given String.
   */
  public void setSubType(String sub)
    throws MimeTypeParseException
  {
    _sub = sub;
  }

  /**
   * Retrieve this object's parameter list.
   */
  public MimeTypeParameterList getParameters()
  {
    return _parameters;
  }

  /**
   * Retrieve the value associated with the given name, or null if
   * there is no current association.
   */
  public String getParameter(String name)
  {
    return getParameters().get(name);
  }

  /**
   * Set the value to be associated with the given name, replacing any
   * previous association.
   */
  public void setParameter(String name, String value)
  {
    getParameters().set(name, value);
  }

  /**
   * Remove any value associated with the given name.
   */
  public void removeParameter(String name)
  {
    getParameters().remove(name);
  }

  /**                                                                      
   * Return the String representation of this object.                      
   */
  public String toString()
  {
    return getBaseType() + _parameters;
  }

  /**
   * Return a String representation of this object without the
   * parameter list.
   */
  public String getBaseType()
  {
    return getPrimaryType() + "/" + getSubType();
  }

 /**
   * Determine if the primary and sub type of this object is the same
   * as what is in the given type.
   */
  public boolean match(MimeType type)
    throws MimeTypeParseException
  {
    return getBaseType().equals(type.getBaseType());
  }

  /**
   * Determine if the primary and sub type of this object is the same
   * as the content type described in rawdata.
   */
  public boolean match(String rawdata)
    throws MimeTypeParseException
  {
     return match(new MimeType(rawdata));
  }

  /**
   * The object implements the writeExternal method to save its contents
   * by calling the methods of DataOutput for its primitive values or
   * calling the writeObject method of ObjectOutput for objects, strings   
   * and arrays.
   *                                                                       
   * @param out the ObjectOutput object to write to
   * @exception java.io.IOException Includes any I/O exceptions that may occur
   */
  public void writeExternal(ObjectOutput out)
    throws IOException
  {
    out.writeUTF(getPrimaryType());
    out.writeUTF(getSubType());
  }

  /**
   * The object implements the readExternal method to restore its
   * contents by calling the methods of DataInput for primitive
   * types and readObject for objects, strings and arrays.

   * The readExternal method must read the values in the same sequence
   * and with the same types as were written by writeExternal.
   *                                                                       
   * @param in the ObjectInput object to read from                         
   * @exception ClassNotFoundException If the class for an object being
   *            stored cannot be found.
   * @exception java.io.IOException                                        
   */
  public void readExternal(ObjectInput in)
    throws IOException
  {
    _primary = in.readUTF();
    _sub = in.readUTF();
  }
}
