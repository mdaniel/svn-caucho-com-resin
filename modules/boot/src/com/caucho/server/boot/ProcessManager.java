/*
 * Copyright (c) 1998-2004 Caucho Technology -- all rights reserved
 *
 * This file is part of Resin(R) Open Source
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
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.server.boot;

import java.io.InputStream;
import java.io.IOException;

import java.util.logging.Logger;
import java.util.logging.Level;

import com.caucho.vfs.ReadStream;
import com.caucho.vfs.Vfs;

/**
 * Thread for managing the child process.
 */
public class ProcessManager {
  private static final Logger log =
    Logger.getLogger(ProcessManager.class.getName());
  
  private volatile Process _process;
  
  void start()
    throws IOException
  {
    Runtime runtime = Runtime.getRuntime();
    
    _process = runtime.exec(new String[] {"java", "com.caucho.server.resin.Resin" },
			    new String[] {"CLASSPATH=lib/resin.jar"});

    try {
      new ReadThread(_process.getInputStream()).start();
      new ReadThread(_process.getErrorStream()).start();
      
      int status = waitForStatus();

      System.out.println("STATUS:" + status);
    } finally {
      _process = null;
    }
  }

  private int waitForStatus()
  {
    while (true) {
      try {
	return _process.waitFor();
      } catch (InterruptedException e) {
	log.log(Level.WARNING, e.toString(), e);
      }
    }
  }

  class ReadThread extends Thread {
    private ReadStream _is;
    private char []cBuf = new char[1024];

    ReadThread(InputStream is)
    {
      _is = Vfs.openRead(is);
    }
    
    public void run()
    {
      while (_process != null) {
	try {
	  int len = _is.read(cBuf, 0, cBuf.length);

	  if (len < 0)
	    return;

	  System.out.print(new String(cBuf, 0, len));
	} catch (IOException e) {
	}
      }
    }
  }
}
