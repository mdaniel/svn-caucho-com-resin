package com.caucho.quercus.lib.dom;

import com.caucho.xml.QEntityReference;

public class DOMEntityReference
  extends QEntityReference
{
  public DOMEntityReference(String name)
  {
    super(name);
  }

  DOMEntityReference(DOMDocument owner, String name)
  {
    super(owner, name);
  }
}
