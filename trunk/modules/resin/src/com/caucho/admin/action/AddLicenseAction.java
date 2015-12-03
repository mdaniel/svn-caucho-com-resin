/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
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
import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.config.ConfigException;
import com.caucho.env.shutdown.ExitCode;
import com.caucho.env.shutdown.ShutdownSystem;
import com.caucho.jmx.Jmx;
import com.caucho.license.LicenseCheck;
import com.caucho.management.server.LicenseStoreMXBean;
import com.caucho.server.resin.Resin;
import com.caucho.server.resin.ResinDelegate;
import com.caucho.util.*;
import com.caucho.vfs.Path;
import com.caucho.vfs.Vfs;
import com.caucho.vfs.WriteStream;
import com.caucho.vfs.WriterStreamImpl;

public class AddLicenseAction implements AdminAction
{
  private static final Logger log
    = Logger.getLogger(AddLicenseAction.class.getName());

  private static final L10N L = new L10N(AddLicenseAction.class);

  public String execute(String licenseContent, 
                        String fileName, 
                        boolean overwrite,
                        boolean restart)
  {
    try {
      Class<?> cl = null;
      
      cl = Class.forName("com.caucho.license.LicenseCheckImpl");
    }
    catch (ClassNotFoundException e) {
      throw new ConfigException(L.l("add-license requires the Resin Professional download"), e);
    }
    
    WriteStream out = null;

    try {
      Resin resin = Resin.getCurrent();
      Path resinRoot = resin.getRootDirectory();
      
      // Path licensePath = resinRoot.lookup("licenses");
      
      Path licensePath = resin.getLicenseDirectory();
      
      if (licensePath == null 
          || ! licensePath.isDirectory()
          || ! licensePath.canWrite()) {
        licensePath = resinRoot.lookup("licenses");
      }

      Path licenseFile = licensePath.lookup(fileName);
      
      if (licenseFile.exists() && ! overwrite) {
        log.log(Level.FINE,
                L.l("add-license will not overwrite {0} (use -overwrite)",
                    licenseFile));
        return L.l("add-license will not overwrite {0} (use -overwrite)",
                   licenseFile);
      }

      licensePath.mkdirs();
      
      log.info(this + " adding license " + licenseFile.getNativePath());

      out = licenseFile.openWrite();
      out.print(licenseContent);
    } catch (IOException e) {
      throw new ConfigException(L.l("add-license failed to write {0}: {1}", 
                                    fileName, e.toString()), e);
    } finally {
      IoUtil.close(out);
    }
    
    if (restart) {
      new Alarm(new AlarmListener()
      {
        @Override
        public void handleAlarm(Alarm alarm)
        {
          ShutdownSystem.shutdownActive(ExitCode.OK, 
                                        L.l("Resin restarting for add-license"));
        }
      }, 500);
      
      log.log(Level.FINE, 
              L.l("add-license wrote {0} successfully and restarting Resin", 
                  fileName));
      return L.l("add-license wrote {0} successfully and restarting Resin", 
                 fileName);
    } else {
      log.log(Level.FINE, L.l("add-license wrote {0} successfully", fileName));
      return L.l("add-license wrote {0} successfully", fileName);
    }
  }
  
  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[]";
  }
}
