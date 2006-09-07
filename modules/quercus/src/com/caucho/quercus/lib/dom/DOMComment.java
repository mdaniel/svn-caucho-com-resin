package com.caucho.quercus.lib.dom;

import com.caucho.quercus.module.Optional;
import com.caucho.xml.QComment;

public class DOMComment
  extends QComment
{
  public DOMComment(@Optional String content)
  {
    super(content);
  }

  DOMComment(DOMDocument owner, String content)
  {
    super(owner, content);
  }
}
