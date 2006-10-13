package example;

import java.net.*;
import java.io.*;

import com.caucho.vfs.*;

public class AppConfig {
  ConfigFilesLocation _cfl = null;

  /**
   * Set the base for subsequent call's to openConfigFileRead()
   * and openConfigFileWrite()
   *
   * @param location a file path or url
   */
  public void setConfigFilesLocation(String location)
    throws Exception
  {
    _cfl = new ConfigFilesLocation();
    _cfl.setLocation(location);
  }

  public void init()
    throws Exception
  {
    if (_cfl == null)
      throw new Exception("'config-files-location' must be set");
  }

  /**
   * Create and return a ReadStream for a configuration file, with
   * the file being relative to the base previously set with
   * setConfigFilesLocation()
   *
   * @return a WriteStream, which can be treated as a
   * java.io.InputStream if desired
   *
   * @see java.io.InputStream
   */
  public ReadStream openConfigFileRead(String file)
    throws IOException
  {
    return _cfl.openRead(file);
  }

  /**
   * Create and return an WriteStream for a configuration file, with
   * the file being relative to the base previously set with
   * setConfigFilesLocation().
   *
   * @return a WriteStream, which can be treated as a
   * java.io.OutputStream if desired
   *
   * @see java.io.OutputStream
   */
  public WriteStream openConfigFileWrite(String file)
    throws IOException
  {
    return _cfl.openWrite(file);
  }

  public static class ConfigFilesLocation {
    Path _path;  // com.caucho.vfs.Path

    public void setLocation(String location) 
    {
      _path = Vfs.lookup(location);
    }

    public ReadStream openRead(String file)
      throws IOException
    {
      Path p = _path.lookup(file);
      return p.openRead();
    }

    public WriteStream openWrite(String file)
      throws IOException
    {
      Path p = _path.lookup(file);
      return p.openWrite();
    }
  }
}
