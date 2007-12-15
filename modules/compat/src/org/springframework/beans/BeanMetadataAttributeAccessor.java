package org.springframework.beans;

import org.springframework.core.*;

public class BeanMetadataAttributeAccessor
  extends AttributeAccessorSupport
	  implements BeanMetadataElement
{
  private Object _source;
  
  public Object getSource()
  {
    return _source;
  }
  
  public void setSource(Object source)
  {
    _source = source;
  }
}
