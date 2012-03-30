package com.caucho.license;

import com.caucho.config.ConfigException;
import com.caucho.util.L10N;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

public class LicenseStore
{
  private static final Logger log
    = Logger.getLogger(LicenseStore.class.getName());
  private static final L10N L = new L10N(LicenseStore.class);

  protected File _licenseDirectory;

  protected int _personalCount;
  protected int _professionalCount;

  public final File getLicenseDirectory()
  {
    return _licenseDirectory;
  }

  public void init(File licenseDirectory)
    throws ConfigException, IOException
  {
    clearLicenses();
    _personalCount = 0;
    _professionalCount = 0;

    if (licenseDirectory == null) {
      String resinLicenseDir = System.getProperty("resin.license.dir");

      if (resinLicenseDir != null) {
        licenseDirectory = new File(resinLicenseDir);

        if (! licenseDirectory.isDirectory()) {
          licenseDirectory = null;
        }
      }
    }

    if (licenseDirectory == null) {
      File dir = new File(System.getProperty("user.dir") + "/licenses");

      if (dir.exists() && dir.isDirectory() && dir.canRead()) {
        licenseDirectory = dir;
      }
    }

    if (licenseDirectory == null) {
      String resinHome = System.getProperty("resin.home");
      String resinRoot = System.getProperty("resin.root");

      if (resinRoot != null) {
        File dir = new File(resinRoot + "/licenses");

        if (dir.exists() && dir.isDirectory() && dir.canRead()) {
          licenseDirectory = dir;
        }
      }

      if (licenseDirectory != null) {
      }
      else if (resinHome != null) {
        licenseDirectory = new File(resinHome + "/licenses");
      } else {
        throw new ConfigException(L.l("  Resin Professional has not found any valid licenses.\n"
                                      + "  resin.home must be defined for license validation."));
      }
    }

    _licenseDirectory = licenseDirectory;

    if (! licenseDirectory.exists()) {
      throw new ConfigException(L.l("  Resin Professional has not found any valid licenses.\n"
                                    + "  License directory '{0}' does not exist.",
                                    licenseDirectory));
    }
    else if (! licenseDirectory.isDirectory()) {
      throw new ConfigException(L.l("  Resin Professional has not found any valid licenses.\n"
                                    + "  License path '{0}' is not a valid directory.",
                                    licenseDirectory));
    }
    else if (! licenseDirectory.canRead()) {
      throw new ConfigException(L.l("  Resin Professional has not found any valid licenses.\n"
                                    + "  License directory '{0}' is not readable.",
                                    licenseDirectory));
    }

    addLicenseDirectory(licenseDirectory);
  }

  public void addLicenseDirectory(File licenseDirectory)
    throws ConfigException, IOException
  {
  }

  public void clearLicenses()
  {
  }
}
