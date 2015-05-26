/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.server.admin;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Admin output stream
 */
public class HmuxOutput extends OutputStream {
  private final String _servicePath;

  private OutputStream _os;

  public HmuxOutput(String servicePath)
  {
    _servicePath = servicePath;
  }

  public void open(OutputStream os)
    throws IOException
  {
    _os = os;

    _os.write('C');
    _os.write(0);
    _os.write(1);

    print('m', "POST");
    print('v', "admin.caucho");
    print('i', "127.0.0.1");
    print('H', "Host");
    print('S', "admin.caucho");
    print('U', _servicePath);
  }

  public void write(int ch)
    throws IOException
  {
    _os.write('D');
    _os.write(0);
    _os.write(1);
    _os.write(ch);
  }

  public void write(byte []data, int offset, int length)
    throws IOException
  {
    while (length > 0) {
      int sublen = 0x8000;
      if (length < sublen)
	sublen = length;
      
      _os.write('D');
      _os.write(sublen >> 8);
      _os.write(sublen);
      _os.write(data, offset, sublen);

      length -= sublen;
      offset += sublen;
    }
  }

  public void close()
    throws IOException
  {
    _os.write('Q');
  }

  private void print(int code, String data)
    throws IOException
  {
    _os.write(code);
    _os.write(data.length() >> 8);
    _os.write(data.length());
    for (int i = 0; i < data.length(); i++)
      _os.write(data.charAt(i));
  }
}
