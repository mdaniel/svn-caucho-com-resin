/*
 * Copyright (c) 1998-2011 Caucho Technology -- all rights reserved
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
 */

package com.caucho.admin.action;

import java.io.*;
import java.text.DecimalFormat;
import java.util.*;
import java.util.logging.*;

import javax.management.*;

import com.caucho.config.ConfigException;
import com.caucho.jmx.Jmx;
import com.caucho.profile.HeapDump;
import com.caucho.util.*;

public class HeapDumpAction implements AdminAction
{
  private static final L10N L = new L10N(HeapDumpAction.class);
  
  public String execute(boolean raw, String serverId, File hprofDir)
    throws ConfigException, JMException, IOException
  {
    if (raw)
      return doRawHeapDump(serverId, hprofDir);
    else
      return doProHeapDump();
  }
  
  private String doRawHeapDump(String serverId, File hprofDir)
    throws ConfigException, JMException, IOException
  {
    ObjectName name = new ObjectName(
      "com.sun.management:type=HotSpotDiagnostic");

    final String base = "hprof-" + serverId;
    final Calendar date = new GregorianCalendar();
    date.setTimeInMillis(Alarm.getCurrentTime());
    DecimalFormat f = new DecimalFormat("00");
    String suffix = f.format(date.get(Calendar.YEAR)) + "-" +
                    f.format(date.get(Calendar.MONTH)) + "-" +
                    f.format(date.get(Calendar.DAY_OF_MONTH)) + "-" +
                    f.format(date.get(Calendar.HOUR_OF_DAY)) + "-" +
                    f.format(date.get(Calendar.MINUTE)) + "-" +
                    f.format(date.get(Calendar.SECOND));

    if (hprofDir == null)
      hprofDir = new File(System.getProperty("java.io.tmpdir"));

    final String fileName = base + "-" + suffix + ".hprof";

    MemoryPoolAdapter memoryAdapter = new MemoryPoolAdapter();
    if (memoryAdapter.getEdenUsed() > hprofDir.getFreeSpace())
      throw new ConfigException(L.l("Not enough disk space for `{0}'", fileName));

    File file = new File(hprofDir, fileName);
    if (file.exists())
      throw new ConfigException(L.l("File `{0}' exists.", file));

    MBeanServer mBeanServer = Jmx.getGlobalMBeanServer();
    mBeanServer.invoke(name,
                       "dumpHeap",
                       new Object[]{file.getCanonicalPath(), Boolean.TRUE},
                       new String[]{String.class.getName(), boolean.class.getName()});

    final String result = L.l("Heap dump is written to `{0}'.\n"
                              + "To view the file on the target machine use\n"
                              + "jvisualvm --openfile {0}", file);

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
}
