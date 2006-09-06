package com.caucho.quercus.lib.dom;

import com.caucho.xml.QDocumentType;

public class DOMDocumentType
  extends QDocumentType
{
  DOMDocumentType(String name)
  {
    super(name);
  }

  DOMDocumentType(String name, String publicId, String systemId)
  {
    super(name);
    setPublicId(publicId);
    setSystemId(systemId);
  }
}
