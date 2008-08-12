/*
 * Copyright (c) 1998-2008 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.server.admin;

import java.util.*;

public class DeploySendQuery implements java.io.Serializable
{
  private int _sequenceId;
  private String _sha1;
  private byte []_buffer;
  private int _length;
  private boolean _isEnd;

  private DeploySendQuery()
  {
  }

  public DeploySendQuery(int seq,
			 String sha1,
			 byte []buffer,
			 int length,
			 boolean isEnd)
  {
    _sequenceId = seq;
    _sha1 = sha1;
    _buffer = buffer;
    _length = length;
    _isEnd = isEnd;
  }

  public int getSequenceId()
  {
    return _sequenceId;
  }

  public String getSha1()
  {
    return _sha1;
  }

  public byte []getBuffer()
  {
    return _buffer;
  }

  public int getLength()
  {
    return _length;
  }

  public boolean isEnd()
  {
    return _isEnd;
  }

  @Override
  public String toString()
  {
    return (getClass().getSimpleName()
	    + "[" + _sequenceId + "," + _sha1 + "," + _isEnd + "]");
  }
}
