/*
 * Copyright (c) 1998-2004 Caucho Technology -- all rights reserved
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
  
  public MimeType()
  {
    _parameters = new MimeTypeParameterList();
  }

  public MimeType(String rawData)
    throws MimeTypeParseException
  {
  }

  public MimeType(String primary, String sub)
    throws MimeTypeParseException
  {
    _primary = primary;
    _sub = sub;
    
    _parameters = new MimeTypeParameterList();
  }

  public String getPrimaryType()
  {
    return _primary;
  }

  public void setPrimaryType(String primary)
    throws MimeTypeParseException
  {
    _primary = primary;
  }

  public String getSubType()
  {
    return _sub;
  }

  public void setSubType(String sub)
    throws MimeTypeParseException
  {
    _sub = sub;
  }

  public MimeTypeParameterList getParameters()
  {
    return _parameters;
  }

  public String getParameter(String name)
  {
    return getParameters().get(name);
  }

  public void setParameter(String name, String value)
  {
    getParameters().set(name, value);
  }

  public void removeParameter(String name)
  {
    getParameters().remove(name);
  }

  public String getBaseType()
  {
    return getPrimaryType() + "/" + getSubType();
  }

  public boolean match(String rawData)
    throws MimeTypeParseException
  {
    return getBaseType().equals(rawData);
  }

  public void writeExternal(ObjectOutput out)
    throws IOException
  {
    out.writeUTF(getPrimaryType());
    out.writeUTF(getSubType());
  }

  public void readExternal(ObjectInput in)
    throws IOException
  {
    _primary = in.readUTF();
    _sub = in.readUTF();
  }
}
