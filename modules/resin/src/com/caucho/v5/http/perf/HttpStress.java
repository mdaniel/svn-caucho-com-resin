/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * This file is part of Resin(R)
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Resin Open Source is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * Resin Open Source is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, or any warranty
 * of NON-INFRINGEMENT.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Resin Open Source; if not, write to the
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.http.perf;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;

import com.caucho.v5.config.ConfigException;
import com.caucho.v5.network.port.PollContext;
import com.caucho.v5.network.port.PollTcpSystem;
import com.caucho.v5.util.CharBuffer;
import com.caucho.v5.util.L10N;
import com.caucho.v5.vfs.PathImpl;
import com.caucho.v5.vfs.ReadStream;
import com.caucho.v5.vfs.Vfs;
import com.caucho.v5.vfs.WriteStream;

/**
 * http stress testing.
 */
public class HttpStress
{
  private static final L10N L = new L10N(HttpStress.class);

  private final String []_args;

  private final ArrayList<URI> _urls = new ArrayList<>();
  private PollTcpSystem _keepaliveSystem;

  private CharBuffer _key = new CharBuffer();
  private CharBuffer _value = new CharBuffer();
  private PathImpl _socketPath;
  private int _count;

  private byte []_data = new byte[8 * 1024];

  private HttpStress(String []args)
  {
    _args = args;

    try {
      parseArgs(args);
    } catch (Exception e) {
      throw ConfigException.wrap(e);
    }

    _keepaliveSystem = new PollTcpSystem();
  }

  private void parseArgs(String []args)
    throws URISyntaxException
  {
    for (int i = 0; i < args.length; i++) {
      String arg = args[i];

      if (arg.startsWith("-") && ! arg.startsWith("--")) {
        arg = "-" + arg;
      }

      switch (arg) {
      case "--unix-socket":
        _socketPath = Vfs.lookup().lookup(args[i + 1]);
        i++;
        break;

      case "--count":
        _count = Integer.parseInt(args[i + 1]);
        i++;
        break;

      default:
        if (arg.startsWith("--")) {
          throw new ConfigException(L.l("{0} is an unknown argument", arg));
        }

        else {
          _urls.add(new URI(arg));
        }
      }
    }
  }

  private void run()
    throws IOException
  {
    if (_urls.size() == 0) {
      System.out.println(L.l("{0} requires at least one URL.",
                             getClass().getSimpleName()));
      return;
    }

    PollContext cxt = _keepaliveSystem.createContext(new KeepaliveTask());

    URI uri = _urls.get(0);
    InetSocketAddress addr = new InetSocketAddress(uri.getHost(), uri.getPort());

    runOne(cxt, uri, addr);

    long start = System.nanoTime();

    int count = _count;

    if (count <= 0) {
      count = 1;
    }

    for (int i = 0; i < count; i++) {
      runOne(cxt, uri, addr);
    }

    long end = System.nanoTime();

    long delta = Math.max(end - start, 1);

    System.out.format("Time %.2fms\n", delta / 1e6);
    System.out.format("OPS %.2f\n", 1e9 * count / delta);
  }

  private void runOne(PollContext cxt,
                      URI uri,
                      InetSocketAddress addr)
      throws IOException
  {
    String host = uri.getHost();
    int port = uri.getPort();

    String path = uri.getPath();

    try {
      if (_socketPath != null) {
        _keepaliveSystem.connectUnix(cxt, _socketPath);
      }
      else {
        _keepaliveSystem.connect(cxt, addr);
      }

      writeRequest(cxt.getWriteStream(), host, port, path);

      ReadStream is = cxt.getReadStream();

      int status = parseResult(is);
    } finally {
      cxt.close();
    }
  }

  private void writeRequest(WriteStream os,
                            String host, int port,
                            String path)
    throws IOException
  {
    os.print("GET ");
    os.print(path);
    os.print(" HTTP/1.1\r\n");

    os.print("Host: ");
    os.print(host);
    os.print(":");
    os.print(port);
    os.print("\r\n");

    os.print("Connection: close\r\n");
    os.print("\r\n\r\n");
    os.flush();
    os.close(); // tiny different when commented out
  }

  private int parseResult(ReadStream is)
    throws IOException
  {
    int status = parseStatus(is);

    if (status != 200) {
      System.out.println("Bad-Status: " + status);
    }

    parseHeaders(is);

    parseBody(is);

    return status;
  }

  private int parseStatus(ReadStream is)
    throws IOException
  {
    int ch;

    for (ch = is.read(); ch > 0 && ch != '\n' && ch != ' '; ch = is.read()) {
    }

    if (ch == '\n' || ch < 0) {
      return -1;
    }

    int status = 0;

    for (ch = is.read(); '0' <= ch && ch <= '9'; ch = is.read()) {
      status = 10 * status + ch - '0';
    }

    for (; ch >= 0 && ch != '\n'; ch = is.read()) {
    }

    return status;
  }

  private void parseHeaders(ReadStream is)
    throws IOException
  {
    CharBuffer key = _key;
    CharBuffer value = _value;

    while (true) {
      int ch;

      key.clear();

      for (ch = is.read(); ch > 0 && ch != ':' && ch != '\n'; ch = is.read()) {
        key.append((char) ch);
      }

      if (ch < 0 || ch == '\n') {
        return;
      }

      for (; ch == ':' || ch == ' '; ch = is.read()) {
      }

      value.clear();

      for (; ch >= 0 && ch != '\r' && ch != '\n'; ch = is.read()) {
        value.append((char) ch);
      }

      if (ch == '\r') {
        ch = is.read();
      }
    }
  }

  private void parseBody(ReadStream is)
    throws IOException
  {
    int len;

    while ((len = is.read(_data, 0, _data.length)) >= 0) {
    }
  }

  public static void main(String []args)
  {
    try {
      HttpStress httpStress = new HttpStress(args);

      httpStress.run();
    } catch (Exception e) {
      System.out.println("Exception: " + e);
    }
  }

  class KeepaliveTask implements Runnable {
    public void run()
    {
      System.out.println("Run:");
    }
  }
}
