package com.caucho.license;

import java.io.File;
import java.io.IOException;

import com.caucho.config.ConfigException;

public class LicenseStore
{
  public final void init(File ...licenseDirectory)
    throws ConfigException, IOException
  {
    clearLicenses();
    
    if (licenseDirectory != null) {
      for (File dir : licenseDirectory) {
        addLicenseDirectory(dir);
      }
    }

    String resinLicenseDir = System.getProperty("resin.license.dir");
    
    File dir = null;

    if (resinLicenseDir != null) {
      dir = new File(resinLicenseDir);

      if (dir.isDirectory()) {
        addLicenseDirectory(dir);
      }
    }

    dir = new File(System.getProperty("user.dir") + "/licenses");

    if (dir.exists() && dir.isDirectory() && dir.canRead()) {
      addLicenseDirectory(dir);
    }

    String resinHome = System.getProperty("resin.home");
    String resinRoot = System.getProperty("resin.root");

    if (resinRoot != null) {
      dir = new File(resinRoot + "/licenses");

      if (dir.exists() && dir.isDirectory() && dir.canRead()) {
        addLicenseDirectory(dir);
      }
    }

    if (resinHome != null) {
      dir = new File(resinHome + "/licenses");
        
      addLicenseDirectory(dir);
    }
  }

  public void addLicenseDirectory(File licenseDirectory)
    throws ConfigException, IOException
  {
  }
  
  public File getLicenseDirectory()
  {
    return null;
  }

  public void clearLicenses()
  {
  }
}
