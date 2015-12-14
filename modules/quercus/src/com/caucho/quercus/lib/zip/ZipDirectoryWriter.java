package com.caucho.quercus.lib.zip;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import com.caucho.quercus.env.StringValue;

public class ZipDirectoryWriter extends ZipDirectory
{
  private File _zipFile;
  private ZipOutputStream _zos;

  public ZipDirectoryWriter(String fileToWriteZipFileTo)
      throws FileNotFoundException {
    _zipFile = new File(fileToWriteZipFileTo);
    _zos = new ZipOutputStream(new FileOutputStream(_zipFile));
  }

  public boolean addFromString(String localName, StringValue contents) {
    ZipEntry ze= new ZipEntry(localName);
    try {
      _zos.putNextEntry(ze);
      contents.writeTo(_zos);
      _zos.closeEntry();
    } catch (IOException e) {
      return false;
    }
    return true;
  }

  public boolean zip_close() {
    if (_zos != null) {
      try {
        _zos.close();
      } catch (IOException e) {
      } finally {
        _zos = null;
      }
    }
    return true;
  }
}
