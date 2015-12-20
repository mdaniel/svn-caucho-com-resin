/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.health.stat;

import java.io.Serializable;

/**
 * Factory for creating statistics attributes.
 */
@SuppressWarnings("serial")
public class AddSampleMetadata implements Serializable
{
  private final long _id;
  private final String _name;
  
  @SuppressWarnings("unused")
  private AddSampleMetadata()
  {
    _id = 0;
    _name = null;
  }
  
  public AddSampleMetadata(long id, String name)
  {
    _id = id;
    _name = name;
  }

  public long getId()
  {
    return _id;
  }

  public String getName()
  {
    return _name;
  }

  @Override
  public int hashCode()
  {
    return (int) getId();
  }

  @Override
  public boolean equals(Object obj)
  {
    if (! (obj instanceof AddSampleMetadata))
      return false;

    AddSampleMetadata msg = (AddSampleMetadata) obj;

    return getId() == msg.getId();
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _id + "," + _name + "]";
  }
}
