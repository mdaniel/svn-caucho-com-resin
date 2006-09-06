package com.caucho.quercus.lib.dom;

import com.caucho.quercus.module.Optional;
import com.caucho.xml.QProcessingInstruction;

public class DOMProcessingInstruction
  extends QProcessingInstruction
{
  public DOMProcessingInstruction(String name, @Optional String content)
  {
    super(name, content);
  }
}
