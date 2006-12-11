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
import com.caucho.util.L10N;

import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.ws.WebServiceException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Invokes a SOAP request on a Java POJO method
 */
public class RpcAction extends AbstractAction {
  private final static Logger log = Logger.getLogger(RpcAction.class.getName());
  public static final L10N L = new L10N(RpcAction.class);

  private static final String TARGET_NAMESPACE_PREFIX = "tns";

  public RpcAction(Method method, 
                   JAXBContextImpl jaxbContext, 
                   String targetNamespace,
                   Marshaller marshaller,
                   Unmarshaller unmarshaller)
    throws JAXBException, WebServiceException
  {
    super(method, jaxbContext, targetNamespace);

    /*

    // WSDL message name
    String messageName = DirectSkeleton.getWebServiceName(cl) + "_" + 
                         _methodName;

    _inputMessage.setName(messageName);
    _outputMessage.setName(messageName + "Response");

    _wsdlOperation.setName(_methodName);
    _wsdlBindingOperation.setName(_methodName);

    SOAPBody soapBody = new SOAPBody();
    soapBody.addEncodingStyle("http://schemas.xmlsoap.org/soap/encoding/");
    soapBody.setUse(SOAPUseChoice.LITERAL);
    soapBody.setNamespace(targetNamespace);

    WSDLBindingOperationMessage bindingInput = 
      new WSDLBindingOperationMessage();
    bindingInput.addAny(soapBody);
    _wsdlBindingOperation.setInput(bindingInput);

    WSDLBindingOperationMessage bindingOutput = 
      new WSDLBindingOperationMessage();
    bindingOutput.addAny(soapBody);
    _wsdlBindingOperation.setOutput(bindingOutput);

    SOAPOperation soapOperation = new SOAPOperation();
    soapOperation.setSoapAction("");
    _wsdlBindingOperation.addAny(soapOperation);

    _jaxbContext = jaxbContext;

    //
    // Create wrappers for input and output parameters
    //
    
    switch (_jaxbStyle) {
      case DOCUMENT_BARE:
        prepareDocumentBareParameters();
        break;

      case DOCUMENT_WRAPPED:
        prepareDocumentWrappedParameters();
        break;

      case RPC:
        prepareDocumentWrappedParameters();
        break;
    }

    for (int i = 0; i < params.length; i++) {
      boolean isInput = true;
      WSDLPart part = new WSDLPart();

      String localName = "arg" + i; // As per JAX-WS spec

      for(Annotation a : annotations[i]) {
        if (a instanceof WebParam) {
          WebParam webParam = (WebParam) a;

          if (! "".equals(webParam.name()))
            localName = webParam.name();

          if ("".equals(webParam.targetNamespace()))
            _argMarshall[i]._name = new QName(localName);
          else 
            _argMarshall[i]._name = 
              new QName(localName, webParam.targetNamespace());

          if (params[i].equals(Holder.class)) {
            _argMarshall[i]._mode = webParam.mode();

            if (_argMarshall[i]._mode == WebParam.Mode.OUT)
              isInput = false;
          }
        }
      }

      part.setName(localName);
      part.setType(marshall.getXmlSchemaDatatype());

      if (isInput) {
        _inputMessage.addPart(part);
        _wsdlOperation.addParameterOrder(localName);
      }
      else {
        _outputMessage.addPart(part);
        _inputArgumentCount--;
      }

      argNames.put(localName, _argMarshall[i]);
    }

    WSDLOperationInput opInput = new WSDLOperationInput();
    opInput.setMessage(new QName(targetNamespace, 
                                 _inputMessage.getName(), 
                                 TARGET_NAMESPACE_PREFIX));
    _wsdlOperation.addOperationPart(opInput);

    WSDLOperationOutput opOutput = new WSDLOperationOutput();
    opOutput.setMessage(new QName(targetNamespace, 
                                  _outputMessage.getName(), 
                                  TARGET_NAMESPACE_PREFIX));
    _wsdlOperation.addOperationPart(opOutput);

    // XXX Can input/output messages have no parts?

    _retMarshall = 
      factory.createSerializer(method.getReturnType(), _jaxbContext);

    if (method.isAnnotationPresent(WebResult.class))
      _resultName =
        new QName(method.getAnnotation(WebResult.class).targetNamespace(),
                  method.getAnnotation(WebResult.class).name());
    else
      _resultName = new QName("return");

    if (! method.getReturnType().equals(Void.class) &&
        ! method.getReturnType().equals(void.class)) {
      WSDLPart part = new WSDLPart();
      part.setName(_resultName.getLocalPart());
      part.setType(_retMarshall.getXmlSchemaDatatype());
      _outputMessage.addPart(part);
    }
    */

    //
    // Exceptions -> Faults
    //
    
    // XXX
    /*Class[] exceptions = getExceptionTypes();

    for (Class exception : exceptions)
      exceptionToFault(exception);
      */
  }
}
