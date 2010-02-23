/*
 * Copyright (c) 1998-2010 Caucho Technology -- all rights reserved
 *
 * This file is part of Resin(R) Open Source
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Resin Open Source is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2
 * as published by the Free Software Foundation.
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
 * @author Alex Rojkov
 */

using System;
using System.Collections;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.IO;
using Microsoft.Win32;


namespace Caucho
{
  class Apache
  {
    private static String REG_APACHE_2_2 = "Software\\Apache Software Foundation\\Apache";
    private static String REG_APACHE_2 = "Software\\Apache Group\\Apache";


    public static void FindApache(ArrayList homes)
    {
      String apacheHome = null;

      apacheHome = FindApacheInRegistry(Registry.LocalMachine, REG_APACHE_2_2);

      if (apacheHome != null)
        homes.Add(Util.GetCanonicalPath(apacheHome));

      apacheHome = FindApacheInRegistry(Registry.CurrentUser, REG_APACHE_2_2);

      if (apacheHome != null)
        homes.Add(Util.GetCanonicalPath(apacheHome));

      apacheHome = FindApacheInRegistry(Registry.LocalMachine, REG_APACHE_2);

      if (apacheHome != null)
        homes.Add(Util.GetCanonicalPath(apacheHome));

      apacheHome = FindApacheInRegistry(Registry.CurrentUser, REG_APACHE_2);
      if (apacheHome != null)
        homes.Add(Util.GetCanonicalPath(apacheHome));

      FindApacheInProgramFiles(homes);
    }

    public static void FindApacheInProgramFiles(ArrayList homes)
    {
      String programFiles
        = Environment.GetFolderPath(Environment.SpecialFolder.ProgramFiles);

      String[] groupDirs = Directory.GetDirectories(programFiles, "Apache*");

      foreach (String groupDir in groupDirs) {
        String[] testDirs = Directory.GetDirectories(groupDir, "*");
        foreach (String testDir in testDirs) {
          if (File.Exists(testDir + "\\bin\\Apache.exe") || File.Exists(testDir + "\\bin\\httpd.exe")) {
            homes.Add(Util.GetCanonicalPath(testDir));
          }
        }
      }
    }

    public static String FindApacheInRegistry(RegistryKey registryKey, String location)
    {

      RegistryKey apacheKey = registryKey.OpenSubKey(location);

      String result = null;

      if (apacheKey != null) {
        foreach (String name in apacheKey.GetSubKeyNames()) {
          RegistryKey key = apacheKey.OpenSubKey(name);

          String testRoot = (String)key.GetValue("ServerRoot");
          if (testRoot != null && !"".Equals(testRoot))
            result = testRoot;
        }
      }

      if (result != null && result.IndexOf('~') != -1) {
        StringBuilder builder = new StringBuilder(256);
        Interop.GetLongPathName(result, builder, builder.Capacity);

        result = builder.ToString();
      }

      return result;
    }

  }
}
