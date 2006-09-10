/*
 * Copyright (c) 1998-2006 Caucho Technology -- all rights reserved
 *
 * Caucho Technology permits modification and use of this file in
 * source and binary form ("the Software") subject to the Caucho
 * Developer Source License 1.1 ("the License") which accompanies
 * this file.  The License is also available at
 *   http://www.caucho.com/download/cdsl1-1.xtp
 *
 * In addition to the terms of the License, the following conditions
 * must be met:
 *
 * 1. Each copy or derived work of the Software must preserve the copyright
 *    notice and this notice unmodified.
 *
 * 2. Each copy of the Software in source or binary form must include 
 *    an unmodified copy of the License in a plain ASCII text file named
 *    LICENSE.
 *
 * 3. Caucho reserves all rights to its names, trademarks and logos.
 *    In particular, the names "Resin" and "Caucho" are trademarks of
 *    Caucho and may not be used to endorse products derived from
 *    this software.  "Resin" and "Caucho" may not appear in the names
 *    of products derived from this software.
 *
 * This Software is provided "AS IS," without a warranty of any kind. 
 * ALL EXPRESS OR IMPLIED REPRESENTATIONS AND WARRANTIES, INCLUDING ANY
 * IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE
 * OR NON-INFRINGEMENT, ARE HEREBY EXCLUDED.
 *
 * CAUCHO TECHNOLOGY AND ITS LICENSORS SHALL NOT BE LIABLE FOR ANY DAMAGES
 * SUFFERED BY LICENSEE OR ANY THIRD PARTY AS A RESULT OF USING OR
 * DISTRIBUTING SOFTWARE. IN NO EVENT WILL CAUCHO OR ITS LICENSORS BE LIABLE
 * FOR ANY LOST REVENUE, PROFIT OR DATA, OR FOR DIRECT, INDIRECT, SPECIAL,
 * CONSEQUENTIAL, INCIDENTAL OR PUNITIVE DAMAGES, HOWEVER CAUSED AND
 * REGARDLESS OF THE THEORY OF LIABILITY, ARISING OUT OF THE USE OF OR
 * INABILITY TO USE SOFTWARE, EVEN IF HE HAS BEEN ADVISED OF THE POSSIBILITY
 * OF SUCH DAMAGES.      
 *
 * @author Scott Ferguson
 */

package com.caucho.server.admin;

import java.io.IOException;
import java.io.InputStream;

import com.caucho.vfs.TempBuffer;

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
