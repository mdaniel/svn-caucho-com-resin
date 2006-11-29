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
 * @author Emil Ong
 */

package com.caucho.soap.skeleton;

import com.caucho.jaxb.JAXBContextImpl;
import com.caucho.soap.marshall.Marshall;
import com.caucho.soap.marshall.MarshallFactory;
import com.caucho.soap.wsdl.SOAPBody;
import com.caucho.soap.wsdl.SOAPHeader;
import com.caucho.soap.wsdl.SOAPUseChoice;
import com.caucho.soap.wsdl.WSDLOperationInput;
import com.caucho.soap.wsdl.WSDLOperationOutput;
import com.caucho.soap.wsdl.WSDLPart;
import com.caucho.util.L10N;

import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;
import javax.xml.ws.Holder;
import javax.xml.ws.WebServiceException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Invokes a SOAP request on a Java POJO method.
 *
 * This class handles the document-literal bare (i.e. non-wrapped) style
 * which JAX-WS maps to methods of at most one input and one output 
 * argument.  Non-void return values count as an output argument and
 * INOUT arguments count as both one input and one output.
 */
public class DocumentBarePojoMethodSkeleton extends PojoMethodSkeleton {
  private final static Logger log = 
    Logger.getLogger(DocumentBarePojoMethodSkeleton.class.getName());
  private static final L10N L = new L10N(DocumentBarePojoMethodSkeleton.class);

  private static final String TARGET_NAMESPACE_PREFIX = "tns";

  public DocumentBarePojoMethodSkeleton(Method method, 
                                        MarshallFactory factory,
                                        JAXBContextImpl jaxbContext, 
                                        String targetNamespace,
                                        Map<String,String> elements)
    throws JAXBException, WebServiceException
  {
    super(method, factory, jaxbContext, targetNamespace);

    //
    // Create marshallers and message/parts for the arguments/return values
    //

    Type[] genericParams = _method.getGenericParameterTypes();
    Class[] params = _method.getParameterTypes();
    Annotation[][] annotations = _method.getParameterAnnotations();

    WSDLPart inputPart = null;
    WSDLPart outputPart = null;

    for (int i = 0; i < params.length; i++) {
      QName name = null;
      String localName = "arg" + i;
      Marshall marshall = null;

      WebParam webParam = (WebParam) _method.getAnnotation(WebParam.class);

      if (webParam != null) {
        String partName = null;

        if (! "".equals(webParam.partName()))
          partName = webParam.partName();

        if (! "".equals(webParam.name()))
          localName = webParam.name();

        if (! "".equals(webParam.targetNamespace()))
          name = new QName(webParam.targetNamespace(), localName);
        else
          name = new QName(localName);

        if (webParam.mode() == WebParam.Mode.OUT) {
          if (outputPart != null)
            throw new WebServiceException(L.l("Document bare services cannot have more than 1 input and 1 output parameter"));

          if ("".equals(webParam.name()))
            throw new WebServiceException(L.l("Document bare services must set the name of OUT parameters"));

          if (! params[i].equals(Holder.class))
            throw new WebServiceException(L.l("Output parameters must be Holder<T>s"));

          outputPart = new WSDLPart();
          outputPart.setName(partName);

          Class parameterType = getHolderValueType(genericParams[i]);
          marshall = factory.createSerializer(parameterType, _jaxbContext);
        }
        else if (webParam.mode() == WebParam.Mode.INOUT) {
          if (inputPart != null || outputPart != null)
            throw new WebServiceException(L.l("Document bare services cannot have more than 1 input and 1 output parameter"));

          if ("".equals(webParam.name()))
            throw new WebServiceException(L.l("Document bare services must set the name of INOUT parameters"));

          if (! params[i].equals(Holder.class))
            throw new WebServiceException(L.l("Output parameters must be Holder<T>s"));

          inputPart = new WSDLPart();
          inputPart.setName(partName);

          outputPart = new WSDLPart();
          outputPart.setName(partName);

          Class parameterType = getHolderValueType(genericParams[i]);
          marshall = factory.createSerializer(parameterType, _jaxbContext);
        }
        else {
          if (inputPart != null)
            throw new WebServiceException(L.l("Document bare services cannot have more than 1 input and 1 output parameter"));

          inputPart = new WSDLPart();
          inputPart.setName(partName);
          
          marshall = factory.createSerializer(params[i], _jaxbContext);
        }

        ParameterMarshall paramMarshall = 
          new ParameterMarshall(i, marshall, name, webParam.mode());
        
        if (webParam.header()) {
          _headerArguments.put(localName, paramMarshall);

          if (webParam.mode() == WebParam.Mode.OUT || 
              webParam.mode() == WebParam.Mode.INOUT)
            _headerOutputs = 1;
        }
        else {
          _bodyArguments.put(localName, paramMarshall);

          if (webParam.mode() == WebParam.Mode.OUT || 
              webParam.mode() == WebParam.Mode.INOUT)
            _bodyOutputs = 1;
        }
      }
      else {
        // 
        // No @WebParam annotation found... assume this is an input
        //

        if (inputPart != null)
          throw new WebServiceException(L.l("Document bare services cannot have more than 1 input and 1 output parameter"));

        inputPart = new WSDLPart();
        inputPart.setName(localName);
        name = new QName(localName);

        marshall = factory.createSerializer(params[i], _jaxbContext);

        ParameterMarshall paramMarshall = 
          new ParameterMarshall(i, marshall, name, WebParam.Mode.IN);

        // no annotation => this goes in the body
        _bodyArguments.put(localName, paramMarshall);
      }
    }

    // Check the return value

    if (! _method.getReturnType().equals(Void.class) &&
        ! _method.getReturnType().equals(void.class)) {
      if (outputPart != null)
        throw new WebServiceException(L.l("Document bare services cannot have more than 1 input and 1 output parameter"));

      outputPart = new WSDLPart();
      Marshall marshall = 
        factory.createSerializer(_method.getReturnType(), _jaxbContext);

      WebResult webResult = (WebResult) _method.getAnnotation(WebResult.class);

      QName name = null;
      String partName = null;
      String localName = _responseName; // XXX???

      if (webResult != null) {
        if (! "".equals(webResult.partName()))
          partName = webResult.partName();

        if (! "".equals(webResult.name()))
          localName = webResult.name();

        if (! "".equals(webResult.targetNamespace()))
          name = new QName(webResult.targetNamespace(), localName);
        else 
          name = new QName(localName);

        outputPart.setName(partName);
        outputPart.setType(marshall.getXmlSchemaDatatype());

        ParameterMarshall paramMarshall = 
          new ParameterMarshall(-1, marshall, name, WebParam.Mode.OUT);

        if (webResult.header()) {
          _headerArguments.put(localName, paramMarshall);
          _headerOutputs = 1;
        }
        else {
          _bodyArguments.put(localName, paramMarshall); 
          _bodyOutputs = 1;
        }
      }
      else {
        name = new QName(localName);

        outputPart.setName(localName);

        ParameterMarshall paramMarshall
	  = new ParameterMarshall(-1, marshall, name, WebParam.Mode.OUT);

        _bodyArguments.put(localName, paramMarshall);
      }
    }

    // SOAP Binding -> binding/operation
    
    if (_headerArguments.size() == 0 && _bodyArguments.size() > 0) {
      SOAPBody soapBody = new SOAPBody();
      soapBody.addEncodingStyle(SOAP_ENCODING_STYLE);
      soapBody.setUse(SOAPUseChoice.LITERAL);
      soapBody.setNamespace(targetNamespace);

      if (_bodyArguments.size() - _bodyOutputs > 0)
        _wsdlBindingOperation.getInput().addAny(soapBody);

      if (_bodyOutputs > 0)
        _wsdlBindingOperation.getOutput().addAny(soapBody);
    }
    else if (_bodyArguments.size() == 0 && _headerArguments.size() > 0) {
      SOAPHeader soapHeader = new SOAPHeader();
      soapHeader.addEncodingStyle(SOAP_ENCODING_STYLE);
      soapHeader.setUse(SOAPUseChoice.LITERAL);
      soapHeader.setNamespace(targetNamespace);

      if (_headerArguments.size() - _headerOutputs > 0)
        _wsdlBindingOperation.getInput().addAny(soapHeader);

      if (_headerOutputs > 0)
        _wsdlBindingOperation.getOutput().addAny(soapHeader);
    }
    else if (_bodyArguments.size() == 1 && _headerArguments.size() == 1) {
      SOAPBody soapBody = new SOAPBody();
      soapBody.addEncodingStyle(SOAP_ENCODING_STYLE);
      soapBody.setUse(SOAPUseChoice.LITERAL);
      soapBody.setNamespace(targetNamespace);

      SOAPHeader soapHeader = new SOAPHeader();
      soapHeader.addEncodingStyle(SOAP_ENCODING_STYLE);
      soapHeader.setUse(SOAPUseChoice.LITERAL);
      soapHeader.setNamespace(targetNamespace);

      if (_headerOutputs > 0) {
        _wsdlBindingOperation.getInput().addAny(soapBody);
        _wsdlBindingOperation.getOutput().addAny(soapHeader);
      }
      else {
        _wsdlBindingOperation.getInput().addAny(soapHeader);
        _wsdlBindingOperation.getOutput().addAny(soapBody);
      }
    }

    // portType/operation/input
    
    WSDLOperationInput opInput = new WSDLOperationInput();
    opInput.setMessage(new QName(targetNamespace, 
                                 _inputMessage.getName(), 
                                 TARGET_NAMESPACE_PREFIX));
    _wsdlOperation.addOperationPart(opInput);

    // portType/operation/output
    
    WSDLOperationOutput opOutput = new WSDLOperationOutput();
    opOutput.setMessage(new QName(targetNamespace, 
                                  _outputMessage.getName(), 
                                  TARGET_NAMESPACE_PREFIX));
    _wsdlOperation.addOperationPart(opOutput);

    // message/part 
    if (inputPart != null) {
      _inputMessage.addPart(inputPart);

      ParameterMarshall marshall = _bodyArguments.get(inputPart.getName());

      if (marshall == null)
        marshall = _headerArguments.get(inputPart.getName());

      elements.put(inputPart.getName(),
                   marshall._marshall.getXmlSchemaDatatype().getLocalPart());
    }

    if (outputPart != null) {
      _outputMessage.addPart(outputPart);

      ParameterMarshall marshall = _bodyArguments.get(outputPart.getName());

      if (marshall == null)
        marshall = _headerArguments.get(outputPart.getName());

      elements.put(outputPart.getName(),
                   marshall._marshall.getXmlSchemaDatatype().getLocalPart());
    }


    //
    // Exceptions -> faults
    //

    Class[] exceptions = _method.getExceptionTypes();

    // XXX multiple exceptions -> multiple faults?
    //
    for (Class exception : exceptions) {
    }
  }
}
