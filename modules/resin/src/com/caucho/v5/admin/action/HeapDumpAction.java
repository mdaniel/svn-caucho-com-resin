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
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 */

package com.caucho.v5.admin.action;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import javax.management.JMException;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import com.caucho.v5.config.ConfigException;
import com.caucho.v5.jmx.JmxUtil;
import com.caucho.v5.profile.HeapDump;
import com.caucho.v5.server.container.ServerBase;
import com.caucho.v5.util.L10N;
import com.caucho.v5.vfs.Path;
import com.caucho.v5.vfs.Vfs;

public class HeapDumpAction implements AdminAction
{
  private static final L10N L = new L10N(HeapDumpAction.class);
  
  public String execute(boolean isJvmHprof, String serverId, Path hprofPath)
    throws ConfigException, JMException, IOException
  {
    if (isJvmHprof)
      return doJvmHprofHeapDump(serverId, hprofPath);
    else
      return doProHeapDump();
  }
  
  private String doJvmHprofHeapDump(String serverId, Path hprofPath)
    throws ConfigException, JMException, IOException
  {
    ObjectName name = new ObjectName(
      "com.sun.management:type=HotSpotDiagnostic");
    
    if (hprofPath == null) {
      ServerBase resin = ServerBase.getCurrent();
      
      if (resin == null)
        hprofPath = Vfs.lookup(System.getProperty("java.io.tmpdir"));
      else
        hprofPath = resin.getLogDirectory();
      
      hprofPath = hprofPath.lookup("heap.hprof");
    } else if (hprofPath.isDirectory()) {
      hprofPath = hprofPath.lookup("heap.hprof");
    }

    hprofPath.getParent().mkdirs();

    //MemoryPoolAdapter memoryAdapter = new MemoryPoolAdapter();
    //if (memoryAdapter.getEdenUsed() > hprofPath.getDiskSpaceFree())
    //  throw new ConfigException(L.l("Not enough disk space for `{0}'", fileName));

    // dumpHeap fails if file exists, it will not overwrite, so we have to delete
    if (hprofPath.exists() && hprofPath.isFile())
      hprofPath.remove();

    MBeanServer mBeanServer = JmxUtil.getMBeanServer();
    mBeanServer.invoke(name,
                       "dumpHeap",
                       new Object[]{hprofPath.getPath(), Boolean.TRUE},
                       new String[]{String.class.getName(), boolean.class.getName()});

    final String result = L.l("Heap dump is written to `{0}'.\n"
                              + "To view the file on the target machine use\n"
                              + "jvisualvm --openfile {0}", hprofPath.getPath());

    return result;
  }
  
  private String doProHeapDump()
    throws IOException
  {
    HeapDump dump = HeapDump.create();
    
    StringWriter buffer = new StringWriter();
    PrintWriter writer = new PrintWriter(buffer);
    dump.writeExtendedHeapDump(writer);
    writer.flush();
    
    return buffer.toString();
  }
  
  public String executeJson()
    throws IOException
  {
    HeapDump dump = HeapDump.create();
   
    return dump.jsonHeapDump();
  }
}
