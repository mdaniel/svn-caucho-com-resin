/*
 * Copyright (c) 1998-2010 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.server.admin;

import java.io.IOException;
import java.io.InputStream;

/**
 * Admin input stream
 */
public class HmuxInput extends InputStream {
  private InputStream _is;
  private String _status;
  private int _sublen;
  private boolean _isKeepalive;

  public boolean open(InputStream is)
    throws IOException
  {
    _is = is;
    _sublen = 0;
    _status = "500 incomplete";
    _isKeepalive = false;

    if (scanToData())
      return true;
    else {
      _sublen = -1;
      return false;
    }
  }

  public String getStatus()
  {
    return _status;
  }

  public int read()
    throws IOException
  {
    if (_sublen > 0) {
      _sublen--;
      return _is.read();
    }
    
    if (_sublen == 0) {
      if (scanToData()) {
	_sublen--;
	return _is.read();
      }
      else {
	_sublen = -1;
	return -1;
      }
    }

    return -1;
  }

  private boolean scanToData()
    throws IOException
  {
    int ch;

    while ((ch = _is.read()) >= 0) {
      switch (ch) {
      case 'C':
	_is.skip(2);
	break;
      case 'D':
	_sublen = readLen();
	return true;
      case 's':
	_status = readString(readLen());
	break;
      case 'Q':
	_isKeepalive = true;
	return false;
      case 'X':
      case 'Y':
	return false;
      default:
	{
	  int len = readLen();
	  _is.skip(len);
	  break;
	}
      }
    }

    return false;
  }

  private int readLen()
    throws IOException
  {
    int ch1 = _is.read();
    int ch2 = _is.read();

    if (ch2 < 0)
      return -1;
    else
      return (ch1 << 8) + ch2;
  }
  
  private String readString(int len)
    throws IOException
  {
    StringBuilder sb = new StringBuilder();

    for (; len > 0; len--) {
      int ch = _is.read();

      sb.append((char) ch);
    }

    return sb.toString();
  }
}

