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
 * @author Charles Reich
 */

package com.caucho.quercus.lib;

import com.caucho.quercus.env.BooleanValue;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.NullValue;
import com.caucho.quercus.env.Value;

import org.w3c.dom.DOMImplementation;
import org.w3c.dom.DocumentType;

public class DOMImplementationValue extends Value {

  private DOMImplementation _DOMImplementation;

  public DOMImplementationValue(DOMImplementation DOMImplementation)
  {
    _DOMImplementation = DOMImplementation;
  }

  public Value createDocument(Value namespaceURI,
                              Value qualifiedName,
                              Value doctype)
  {   
    if (_DOMImplementation == null)
      return NullValue.NULL;
    
    String nsURI = null;
    String qn = null;
    DocumentType dt = null;
    
    if (namespaceURI != null) {
      nsURI = namespaceURI.toString();
      
      if (qualifiedName != null) {
        qn = qualifiedName.toString();
        
        if ((doctype != null) && (doctype instanceof DOMDocumentType))
          dt = ((DOMDocumentType) doctype).getDocType();
      }
    }

    return new DOMDocumentValue(_DOMImplementation.createDocument(nsURI, qn, dt));
  }

  public Value createDocumentType(Value qualifiedName,
                                  Value publicId,
                                  Value systemId)
  {
    if (_DOMImplementation == null)
      return NullValue.NULL;
    
    String qn = null;
    String pId = null;
    String sId = null;
    
    if (qualifiedName != null) {
      qn = qualifiedName.toString();
      
      if (publicId != null) {
        pId = publicId.toString();
        
        if (systemId != null) {
          sId = systemId.toString();
        }
      }
    }
    
    return new DOMDocumentType(_DOMImplementation.createDocumentType(qn, pId, sId));
  }
  
  @Override
  public Value evalMethod(Env env, String methodName)
    throws Throwable
  {
    if ("createDocument".equals(methodName))
      return createDocument(null, null, null);
    else if ("createDocumentType".equals(methodName))
      return createDocumentType(null, null, null);
    
    return super.evalMethod(env, methodName);
  }
  
  @Override
  public Value evalMethod(Env env, String methodName, Value a0)
    throws Throwable
  {
    if ("createDocument".equals(methodName))
      return createDocument(a0, null, null);
    else if ("createDocumentType".equals(methodName))
      return createDocumentType(a0, null, null);
    
    return super.evalMethod(env, methodName, a0);
  }
  
  @Override
  public Value evalMethod(Env env, String methodName, Value a0, Value a1)
    throws Throwable
  {
    if ("createDocument".equals(methodName))
      return createDocument(a0, a1, null);
    else if ("createDocumentType".equals(methodName))
      return createDocumentType(a0, a1, null);
    else if ("hasFeature".equals(methodName))
      return hasFeature(a0, a1);
    
    return super.evalMethod(env, methodName, a0, a1);
  }
  
  @Override
  public Value evalMethod(Env env, String methodName, Value a0, Value a1, Value a2)
    throws Throwable
  {
    if ("createDocument".equals(methodName))
      return createDocument(a0, a1, a2);
    else if ("createDocumentType".equals(methodName))
      return createDocumentType(a0, a1, a2);
    
    return super.evalMethod(env, methodName, a0, a1, a2);
  }
  
  public Value hasFeature(Value feature,
                          Value version)
  {
    if (_DOMImplementation == null)
      return BooleanValue.FALSE;
    
    if (_DOMImplementation.hasFeature(feature.toString(), version.toString()))
      return BooleanValue.TRUE;
    else
      return BooleanValue.FALSE;
  }

}
