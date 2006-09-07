package com.caucho.quercus.lib.dom;

import com.caucho.xml.QCdata;

public class DOMCDATASection
  extends QCdata
{
  DOMCDATASection(DOMDocument owner, String data)
  {
    super(owner, data);
  }
}
