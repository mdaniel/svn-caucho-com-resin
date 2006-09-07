package com.caucho.quercus.lib.dom;

import com.caucho.quercus.module.Optional;
import com.caucho.xml.QText;

public class DOMText
  extends QText
{
  public DOMText(@Optional String content)
  {
    super(content);
  }

  DOMText(DOMDocument owner, String content)
  {
    super(owner, content);
  }
}
