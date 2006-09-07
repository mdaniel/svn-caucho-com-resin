package com.caucho.quercus.lib.dom;

import com.caucho.quercus.module.Optional;
import com.caucho.xml.QAttr;
import com.caucho.xml.QName;

public class DOMAttr
  extends QAttr
{
  public DOMAttr(String name, @Optional String textContent)
  {
    super(new QName(name), textContent);
  }

  DOMAttr(DOMDocument owner, QName qname)
  {
    super(owner, qname);
  }
}
