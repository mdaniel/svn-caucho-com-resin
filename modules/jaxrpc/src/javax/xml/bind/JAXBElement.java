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
import java.io.*;
import javax.xml.namespace.*;

/** XXX */
public class JAXBElement<T> implements Serializable {

  /** XXX */
  protected final Class<T> declaredType=null;


  /** XXX */
  protected final QName name=null;


  /** XXX */
  protected boolean nil;


  /** XXX */
  protected final Class scope=null;


  /** XXX */
  protected JAXBElement value;


  /** XXX */
  public JAXBElement(QName name, Class<T> declaredType,
                     Class scope, JAXBElement value)
  {
    throw new UnsupportedOperationException();
  }


  /** XXX */
  public JAXBElement(QName name,
                     Class<T> declaredType, JAXBElement value)
  {
    throw new UnsupportedOperationException();
  }


  /** XXX */
  public Class<T> getDeclaredType()
  {
    throw new UnsupportedOperationException();
  }


  /** XXX */
  public QName getName()
  {
    throw new UnsupportedOperationException();
  }


  /** XXX */
  public Class getScope()
  {
    throw new UnsupportedOperationException();
  }


  /** XXX */
  public JAXBElement getValue()
  {
    throw new UnsupportedOperationException();
  }


  /** XXX */
  public boolean isGlobalScope()
  {
    throw new UnsupportedOperationException();
  }


  /** XXX */
  public boolean isNil()
  {
    throw new UnsupportedOperationException();
  }


  /** XXX */
  public boolean isTypeSubstituted()
  {
    throw new UnsupportedOperationException();
  }


  /** XXX */
  public void setNil(boolean value)
  {
    throw new UnsupportedOperationException();
  }


  /** XXX */
  public void setValue(JAXBElement t)
  {
    throw new UnsupportedOperationException();
  }


  /** XXX */
  public static final class GlobalScope {
    public GlobalScope()
    {
      throw new UnsupportedOperationException();
    }

  }
}

