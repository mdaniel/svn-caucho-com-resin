/*
* Copyright (c) 1998-2007 Caucho Technology -- all rights reserved
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

package com.caucho.xml.saaj;

import com.caucho.util.*;
import com.caucho.xml.*;
import javax.xml.soap.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.*;
import org.w3c.dom.*;
import org.xml.sax.*;
import java.util.*;

public class SOAPPartImpl extends SOAPPart 
{
  private static final L10N L = new L10N(SOAPPartImpl.class);

  private SOAPFactory _factory;
  private SOAPEnvelopeImpl _envelope;
  private String _protocol;
  private MimeHeaders _headers = new MimeHeaders();
  private Transformer _transformer;

  SOAPPartImpl(SOAPFactory factory, String protocol)
    throws SOAPException
  {
    _factory = factory;
    _protocol = protocol;
    
    // prepare the envelope with an empty body and header
    
    Name envelopeName = null;

    if (SOAPConstants.SOAP_1_1_PROTOCOL.equals(_protocol))
      envelopeName = SOAPEnvelopeImpl.SOAP_1_1_ENVELOPE_NAME;
    else if (SOAPConstants.SOAP_1_2_PROTOCOL.equals(_protocol))
      envelopeName = SOAPEnvelopeImpl.SOAP_1_2_ENVELOPE_NAME;
    else if (SOAPConstants.DYNAMIC_SOAP_PROTOCOL.equals(_protocol))
      // this constructor does not get its structure from an
      // external input -> cannot do dynamic protocol detection
      throw new UnsupportedOperationException();
    else
      throw new SOAPException("Unknown SOAP protocol: " + _protocol);

    _envelope = (SOAPEnvelopeImpl) _factory.createElement(envelopeName);
    _envelope.setOwner((Document) this);
    _envelope.addHeader();
    _envelope.addBody();
  }

  SOAPPartImpl(SOAPFactory factory, String protocol, Document document)
    throws SOAPException
  {
    _factory = factory;
    _protocol = protocol;
    
    Name envelopeName = null;

    if (SOAPConstants.SOAP_1_1_PROTOCOL.equals(_protocol))
      envelopeName = SOAPEnvelopeImpl.SOAP_1_1_ENVELOPE_NAME;
    else if (SOAPConstants.SOAP_1_2_PROTOCOL.equals(_protocol))
      envelopeName = SOAPEnvelopeImpl.SOAP_1_2_ENVELOPE_NAME;
    else if (SOAPConstants.DYNAMIC_SOAP_PROTOCOL.equals(_protocol))
      // dynamic protocol is a special SAAJ "protocol" that means
      // get the version from the input.
      envelopeName = NameImpl.fromElement(document.getDocumentElement());
    else
      throw new SOAPException("Unknown SOAP protocol: " + _protocol);

    _envelope = (SOAPEnvelopeImpl) _factory.createElement(envelopeName);
    _envelope.setOwner((Document) this);
    _envelope.deepCopy(document.getDocumentElement());
  }

  private Transformer getTransformer()
    throws TransformerException
  {
    if (_transformer == null) {
      TransformerFactory transformerFactory = TransformerFactory.newInstance();
      _transformer = transformerFactory.newTransformer();
    }

    return _transformer;
  }

  public void addMimeHeader(String name, String value)
  {
    _headers.addHeader(name, value);
  }

  public Iterator getAllMimeHeaders()
  {
    return _headers.getAllHeaders();
  }

  public Source getContent() throws SOAPException
  {
    return new DOMSource(this);
  }

  public SOAPEnvelope getEnvelope() throws SOAPException
  {
    return _envelope;
  }

  public Iterator getMatchingMimeHeaders(String[] names)
  {
    return _headers.getMatchingHeaders(names);
  }

  public String[] getMimeHeader(String name)
  {
    return _headers.getHeader(name);
  }

  public Iterator getNonMatchingMimeHeaders(String[] names)
  {
    return _headers.getNonMatchingHeaders(names);
  }

  public void removeAllMimeHeaders()
  {
    _headers.removeAllHeaders();
  }

  public void removeMimeHeader(String header)
  {
    _headers.removeHeader(header);
  }

  public void setContent(Source source) 
    throws SOAPException
  {
    org.w3c.dom.Node node = null;
    Element element = null;

    if (source instanceof DOMSource) {
      node = ((DOMSource) source).getNode();
    }
    else {
      try {
        DOMResult result = new DOMResult();
        getTransformer().transform(source, result);
        node = result.getNode();
      }
      catch (TransformerException e) {
        throw new SOAPException(e);
      }
    }

    if (node.getNodeType() == DOCUMENT_NODE)
      element = ((Document) node).getDocumentElement();
    else if (node.getNodeType() == ELEMENT_NODE) 
      element = (Element) node;
    else
      throw new SOAPException(L.l("Source (or transformed DOM) does not have a Document or Element node: {0}", source));

    Name envelopeName = null;

    if (SOAPConstants.SOAP_1_1_PROTOCOL.equals(_protocol))
      envelopeName = SOAPEnvelopeImpl.SOAP_1_1_ENVELOPE_NAME;
    else if (SOAPConstants.SOAP_1_2_PROTOCOL.equals(_protocol))
      envelopeName = SOAPEnvelopeImpl.SOAP_1_2_ENVELOPE_NAME;
    else if (SOAPConstants.DYNAMIC_SOAP_PROTOCOL.equals(_protocol))
      // dynamic protocol is a special SAAJ "protocol" that means
      // get the version from the input.
      envelopeName = NameImpl.fromElement(element);
    else
      throw new SOAPException("Unknown SOAP protocol: " + _protocol);

    _envelope = (SOAPEnvelopeImpl) _factory.createElement(envelopeName);
    _envelope.setOwner((Document) this);
    _envelope.deepCopy(element);
  }

  public void setMimeHeader(String name, String value)
  {
    _headers.setHeader(name, value);
  }

  // javax.xml.soap.Node
 
  public void detachNode()
  {
    throw new UnsupportedOperationException();
  }

  public SOAPElement getParentElement()
  {
    throw new UnsupportedOperationException();
  }

  public String getValue()
  {
    throw new UnsupportedOperationException();
  }

  public void recycleNode()
  {
    throw new UnsupportedOperationException();
  }

  public void setParentElement(SOAPElement parent) 
    throws SOAPException
  {
    throw new UnsupportedOperationException();
  }

  public void setValue(String value)
  {
    throw new UnsupportedOperationException();
  }

  // org.w3c.dom.Document

  public org.w3c.dom.Node adoptNode(org.w3c.dom.Node source)
  {
    throw new UnsupportedOperationException();
  }

  public Attr createAttribute(String name)
  {
    throw new UnsupportedOperationException();
  }

  public Attr createAttributeNS(String namespaceURI, String qualifiedName)
  {
    throw new UnsupportedOperationException();
  }

  public CDATASection	createCDATASection(String data)
  {
    throw new UnsupportedOperationException();
  }

  public Comment createComment(String data)
  {
    throw new UnsupportedOperationException();
  }

  public DocumentFragment createDocumentFragment()
  {
    throw new UnsupportedOperationException();
  }

  public Element createElement(String tagName)
  {
    throw new UnsupportedOperationException();
  }

  public Element createElementNS(String namespaceURI, String qualifiedName)
  {
    throw new UnsupportedOperationException();
  }

  public EntityReference createEntityReference(String name)
  {
    throw new UnsupportedOperationException();
  }

  public ProcessingInstruction createProcessingInstruction(String target, 
                                                           String data)
  {
    throw new UnsupportedOperationException();
  }

  public org.w3c.dom.Text createTextNode(String data)
  {
    throw new UnsupportedOperationException();
  }

  public DocumentType getDoctype()
  {
    throw new UnsupportedOperationException();
  }

  public Element getDocumentElement()
  {
    return _envelope;
  }

  public String getDocumentURI()
  {
    throw new UnsupportedOperationException();
  }

  public DOMConfiguration getDomConfig()
  {
    throw new UnsupportedOperationException();
  }

  public Element getElementById(String elementId)
  {
    throw new UnsupportedOperationException();
  }

  public NodeList getElementsByTagName(String tagname)
  {
    throw new UnsupportedOperationException();
  }

  public NodeList getElementsByTagNameNS(String namespaceURI, String localName)
  {
    throw new UnsupportedOperationException();
  }

  public DOMImplementation getImplementation()
  {
    throw new UnsupportedOperationException();
  }

  public String getInputEncoding()
  {
    throw new UnsupportedOperationException();
  }

  public boolean getStrictErrorChecking()
  {
    throw new UnsupportedOperationException();
  }

  public String getXmlEncoding()
  {
    throw new UnsupportedOperationException();
  }

  public boolean getXmlStandalone()
  {
    throw new UnsupportedOperationException();
  }

  public String getXmlVersion()
  {
    throw new UnsupportedOperationException();
  }

  public org.w3c.dom.Node importNode(org.w3c.dom.Node importedNode, 
                                     boolean deep)
  {
    throw new UnsupportedOperationException();
  }

  public void normalizeDocument()
  {
    throw new UnsupportedOperationException();
  }

  public org.w3c.dom.Node renameNode(org.w3c.dom.Node n, 
                                     String namespaceURI, 
                                     String qualifiedName)
    throws DOMException
  {
    throw new UnsupportedOperationException();
  }

  public void setDocumentURI(String documentURI)
  {
    throw new UnsupportedOperationException();
  }

  public void setStrictErrorChecking(boolean strictErrorChecking)
  {
    throw new UnsupportedOperationException();
  }

  public void setXmlStandalone(boolean xmlStandalone)
  {
    throw new UnsupportedOperationException();
  }

  public void setXmlVersion(String xmlVersion)
  {
    throw new UnsupportedOperationException();
  }

  // org.w3c.dom.Node

  public org.w3c.dom.Node appendChild(org.w3c.dom.Node newChild)
  {
    throw new UnsupportedOperationException();
  }

  public org.w3c.dom.Node cloneNode(boolean deep)
  {
    throw new UnsupportedOperationException();
  }

  public short compareDocumentPosition(org.w3c.dom.Node other)
  {
    throw new UnsupportedOperationException();
  }

  public NamedNodeMap getAttributes()
  {
    throw new UnsupportedOperationException();
  }

  public String getBaseURI()
  {
    throw new UnsupportedOperationException();
  }

  public NodeList getChildNodes()
  {
    return new NodeListImpl();
  }

  public Object getFeature(String feature, String version)
  {
    throw new UnsupportedOperationException();
  }

  public org.w3c.dom.Node getFirstChild()
  {
    return _envelope;
  }

  public org.w3c.dom.Node getLastChild()
  {
    return _envelope;
  }

  public String getLocalName()
  {
    throw new UnsupportedOperationException();
  }

  public String getNamespaceURI()
  {
    throw new UnsupportedOperationException();
  }

  public org.w3c.dom.Node getNextSibling()
  {
    throw new UnsupportedOperationException();
  }

  public String getNodeName()
  {
    return "#document";
  }

  public short getNodeType()
  {
    return DOCUMENT_NODE;
  }

  public String getNodeValue()
  {
    throw new UnsupportedOperationException();
  }

  public Document getOwnerDocument()
  {
    throw new UnsupportedOperationException();
  }

  public org.w3c.dom.Node getParentNode()
  {
    throw new UnsupportedOperationException();
  }

  public String getPrefix()
  {
    throw new UnsupportedOperationException();
  }

  public org.w3c.dom.Node getPreviousSibling()
  {
    throw new UnsupportedOperationException();
  }

  public String getTextContent()
  {
    throw new UnsupportedOperationException();
  }

  public Object getUserData(String key)
  {
    throw new UnsupportedOperationException();
  }

  public boolean hasAttributes()
  {
    throw new UnsupportedOperationException();
  }

  public boolean hasChildNodes()
  {
    throw new UnsupportedOperationException();
  }

  public org.w3c.dom.Node insertBefore(org.w3c.dom.Node newChild, 
                                       org.w3c.dom.Node refChild)
  {
    throw new UnsupportedOperationException();
  }

  public boolean isDefaultNamespace(String namespaceURI)
  {
    throw new UnsupportedOperationException();
  }

  public boolean isEqualNode(org.w3c.dom.Node arg)
  {
    throw new UnsupportedOperationException();
  }

  public boolean isSameNode(org.w3c.dom.Node other)
  {
    throw new UnsupportedOperationException();
  }

  public boolean isSupported(String feature, String version)
  {
    throw new UnsupportedOperationException();
  }

  public String lookupNamespaceURI(String prefix)
  {
    throw new UnsupportedOperationException();
  }

  public String lookupPrefix(String namespaceURI)
  {
    throw new UnsupportedOperationException();
  }

  public void normalize()
  {
    throw new UnsupportedOperationException();
  }

  public org.w3c.dom.Node removeChild(org.w3c.dom.Node oldChild)
  {
    throw new UnsupportedOperationException();
  }

  public org.w3c.dom.Node replaceChild(org.w3c.dom.Node newChild, 
                                       org.w3c.dom.Node oldChild)
  {
    throw new UnsupportedOperationException();
  }

  public void setNodeValue(String nodeValue)
  {
    throw new UnsupportedOperationException();
  }

  public void setPrefix(String prefix)
  {
    throw new UnsupportedOperationException();
  }

  public void setTextContent(String textContent)
  {
    throw new UnsupportedOperationException();
  }

  public Object setUserData(String key, Object data, UserDataHandler handler)
  {
    throw new UnsupportedOperationException();
  }

  protected class NodeListImpl 
    implements NodeList 
  {
    public int getLength()
    {
      return 1;
    }

    public org.w3c.dom.Node item(int i)
    {
      if (i != 0)
        return null;

      return _envelope;
    }
  }
}
