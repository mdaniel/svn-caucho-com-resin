package com.caucho.quercus.lib.dom;

import com.caucho.quercus.module.Optional;
import com.caucho.quercus.module.NotNull;
import com.caucho.quercus.module.ReturnNullAsFalse;
import com.caucho.xml.QDOMImplementation;

import org.w3c.dom.DocumentType;

public class DOMImplementation
{
  private final QDOMImplementation _impl = new QDOMImplementation();

  public DOMImplementation()
  {
  }

  static public boolean hasFeature(String feature, String version)
  {
    return (new QDOMImplementation()).hasFeature(feature, version);
  }

  @ReturnNullAsFalse
  static public DOMDocumentType createDocumentType(@NotNull String qualifiedName,
                                                   @Optional String publicId,
                                                   @Optional String systemId)
  {
    if (qualifiedName == null)
      return null;

    if ((publicId != null && publicId.length() > 0)
        && (publicId != null && publicId.length() > 0))
      return new DOMDocumentType(qualifiedName, publicId, systemId);
    else
      return new DOMDocumentType(qualifiedName);
  }

  static public DOMDocument createDocument(@Optional String namespaceURI,
                                           @Optional String name,
                                           @Optional DocumentType docType)
  {
    DOMDocument doc = new DOMDocument(new DOMImplementation());

    if (docType != null)
      doc.setDoctype(docType);

    if (name != null && name.length() > 0) {
      DOMElement elt = new DOMElement(name, null, namespaceURI);
      doc.appendChild(elt);
    }

    return doc;
  }

  QDOMImplementation getQDOMImplementation()
  {
    return _impl;
  }
}
