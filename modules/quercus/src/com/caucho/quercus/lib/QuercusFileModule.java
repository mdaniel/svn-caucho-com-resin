/*
 * Copyright (c) 1998-2006 Caucho Technology -- all rights reserved
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

package com.caucho.quercus.lib;

import java.io.IOException;
import java.util.Arrays;

import java.util.logging.Logger;
import java.util.logging.Level;

import com.caucho.util.L10N;

import com.caucho.quercus.module.AbstractQuercusModule;
import com.caucho.quercus.module.Optional;

import com.caucho.quercus.env.*;

import com.caucho.quercus.resources.StreamResource;

import com.caucho.util.Alarm;

import com.caucho.vfs.WriteStream;
import com.caucho.vfs.TempBuffer;
import com.caucho.vfs.Path;
import com.caucho.vfs.TempStream;
import com.caucho.vfs.StringWriter;

/**
 * Information and actions for about files
 */
public class QuercusFileModule extends AbstractQuercusModule {
  private static final L10N L = new L10N(QuercusFileModule.class);
  private static final Logger log
    = Logger.getLogger(QuercusFileModule.class.getName());

  public static final int SEEK_SET = 0;
  public static final int SEEK_CUR = 1;
  public static final int SEEK_END = 2;

  public static final String DIRECTORY_SEPARATOR = "/";
  public static final String PATH_SEPARATOR = ":";

  public static final int UPLOAD_ERROR_OK = 0;
  public static final int UPLOAD_ERR_INI_SIZE = 1;
  public static final int UPLOAD_ERR_FORM_SIZE = 2;
  public static final int UPLOAD_ERR_PARTIAL = 3;
  public static final int UPLOAD_ERR_NO_FILE = 4;

  /**
   * Returns the base name of a string.
   */
  public static String basename(String path, @Optional String suffix)
  {
    int len = path.length();

    if (len == 0)
      return "";
    else if (path.endsWith("/"))
      len -= 1;
    else if (path.endsWith("\\"))
      len -= 1;

    int p = path.lastIndexOf('/', len - 1);

    if (p < 0)
      p = path.lastIndexOf('\\', len - 1);

    String file;

    if (p < 0)
      file = path.substring(0, len);
    else
      file = path.substring(p + 1, len);

    if (suffix != null && file.endsWith(suffix))
      file = file.substring(0, file.length() - suffix.length());

    return file;
  }

  /**
   * Changes the working directory
   *
   * @param path the path to change to
   */
  public static boolean chdir(Env env, Path path)
  {
    if (path.isDirectory()) {
      env.setPwd(path);
      return true;
    }
    else {
      env.warning(L.l("{0} is not a directory", path.getFullPath()));

      return false;
    }
  }

  /**
   * Changes the working directory, forming a virtual root
   *
   * @param path the path to change to
   */
  public static boolean chroot(Env env, Path path)
  {
    if (path.isDirectory()) {
      env.setPwd(path.createRoot());

      return true;
    }
    else {
      env.warning(L.l("{0} is not a directory", path.getFullPath()));

      return false;
    }
  }

  /**
   * Changes the group of the file.
   *
   * @param env the PHP executing environment
   * @param file the file to change the group of
   * @param group the group id to change to
   */
  public static boolean chgrp(Env env, Path file, Value group)
  {
    if (!file.canRead()) {
      env.warning(L.l("{0} cannot be read", file.getFullPath()));

      return false;
    }

    // quercus/160i

    try {
      // XXX: safe_mode

      if (group instanceof LongValue)
        file.changeGroup(group.toInt());
      else
        file.changeGroup(group.toString());

      return true;
    } catch (IOException e) {
      log.log(Level.FINE, e.toString(), e);

      return false;
    }
  }

  /**
   * Changes the permissions of the file.
   *
   * @param env the PHP executing environment
   * @param file the file to change the group of
   * @param mode the mode id to change to
   */
  public static boolean chmod(Env env, Path file, int mode)
  {
    if (!file.canRead()) {
      env.warning(L.l("{0} cannot be read", file.getFullPath()));

      return false;
    }

    // quercus/160j

    // XXX: safe_mode

    file.chmod(mode);

    return true;
  }

  /**
   * Changes the ownership of the file.
   *
   * @param env the PHP executing environment
   * @param file the file to change the group of
   * @param user the user id to change to
   */
  public static boolean chown(Env env, Path file, Value user)
  {
    if (!file.canRead()) {
      env.warning(L.l("{0} cannot be read", file.getFullPath()));

      return false;
    }

    try {
      // XXX: safe_mode

      if (user instanceof LongValue)
        file.changeOwner(user.toInt());
      else
        file.changeOwner(user.toString());

      return true;
    } catch (IOException e) {
      log.log(Level.FINE, e.toString(), e);

      return false;
    }
  }

  /**
   * Clears the stat cache for the file
   *
   * @param env the PHP executing environment
   */
  public static Value clearstatcache(Env env)
  {
    // quercus/160l

    // XXX: stubbed

    return NullValue.NULL;
  }

  /**
   * Copies a file to the destination.
   *
   * @param src the source path
   * @param dst the destination path
   */
  public static boolean copy(Env env, Path src, Path dst)
  {
    // quercus/1603

    try {
      if (! src.canRead() || ! src.isFile()) {
        env.warning(L.l("{0} cannot be read", src.getFullPath()));

        return false;
      }

      WriteStream os = dst.openWrite();

      try {
        src.writeToStream(os);
      } finally {
        os.close();
      }

      return true;
    } catch (IOException e) {
      log.log(Level.FINE, e.toString(), e);

      return false;
    }
  }

  /**
   * Opens a directory
   *
   * @param path the path to change to
   */
  public static Value dir(Env env, Path path)
    throws IOException
  {
    if (!path.isDirectory()) {
      env.warning(L.l("{0} is not a directory", path.getFullPath()));

      return BooleanValue.FALSE;
    }

    DirectoryValue dir = new DirectoryValue(path);

    env.addResource(dir);

    return dir;
  }

  /**
   * Returns the directory name of a string.
   */
  public String dirname(String path)
  {
    // quercus/1601

    int len = path.length();

    if (len == 0)
      return ".";
    else if (path.equals("/"))
      return path;

    int p = path.lastIndexOf('/', len - 2);

    if (p == 0)
      return "/";
    else if (p > 0)
      return path.substring(0, p);

    p = path.lastIndexOf('\\', len - 2);

    if (p == 0)
      return "\\";
    else if (p > 0)
      return path.substring(0, p);

    return ".";
  }

  /**
   * Returns the free space for disk partition containing the directory
   *
   * @param directory the disk directory
   */
  public static Value disk_free_space(Env env, Path directory)
  {
    // quercus/160m

    if (!directory.canRead()) {
      env.warning(L.l("{0} cannot be read", directory.getFullPath()));

      return BooleanValue.FALSE;
    }

    return new DoubleValue(directory.getDiskSpaceFree());
  }

  /**
   * Returns the total space for disk partition containing the directory
   *
   * @param directory the disk directory
   */
  public static Value disk_total_space(Env env, Path directory)
  {
    // quercus/160n

    if (!directory.canRead()) {
      env.warning(L.l("{0} cannot be read", directory.getFullPath()));

      return BooleanValue.FALSE;
    }

    return new DoubleValue(directory.getDiskSpaceTotal());
  }

  /**
   * Returns the total space for disk partition containing the directory
   *
   * @param directory the disk directory
   */
  public static Value diskfreespace(Env env, Path directory)
  {
    return disk_free_space(env, directory);
  }

  /**
   * Closes a file.
   */
  public boolean fclose(Env env, StreamResource file)
    throws IOException
  {
    // quercus/1611

    if (file == null) {
      env.warning(L.l("{0} is null", "handle"));
      return false;
    }

    file.close();

    return true;
  }

  /**
   * Checks for the end of file.
   */
  public boolean feof(Env env, StreamResource file)
  {
    // quercus/1618

    if (file == null) {
      env.warning(L.l("{0} is null", "handle"));
      return false;
    }

    return file.isEOF();
  }

  /**
   * Flushes a file.
   */
  public boolean fflush(Env env, StreamResource file)
    throws IOException
  {
    // quercus/1619

    if (file == null) {
      env.warning(L.l("{0} is null", "handle"));

      return false;
    }

    file.flush();

    return true;
  }

  /**
   * Returns the next character
   */
  public Value fgetc(Env env, StreamResource file)
    throws IOException
  {
    if (file == null) {
      env.warning(L.l("{0} is null", "handle"));
      return BooleanValue.FALSE;
    }

    // quercus/1612
    int ch = file.read();

    if (ch >= 0)
      return new StringValueImpl(String.valueOf((char) ch));
    else
      return BooleanValue.FALSE;
  }

  /**
   * Parses a comma-separated-value line from a file.
   *
   * @param file the file to read
   * @param length the maximum line length
   * @param delimiter optional comma replacement
   * @param enclosure optional quote replacement
   */
  public Value fgetcsv(Env env,
                       StreamResource file,
                       @Optional int length,
                       @Optional String delimiter,
                       @Optional String enclosure)
    throws IOException
  {
    // quercus/1619

    if (file == null) {
      env.warning(L.l("{0} is null", "handle"));
      return BooleanValue.FALSE;
    }

    // XXX: length is never used
    if (length <= 0)
      length = Integer.MAX_VALUE;

    int comma = ',';

    if (delimiter != null && delimiter.length() > 0)
      comma = delimiter.charAt(0);

    int quote = '"';

    if (enclosure != null && enclosure.length() > 0)
      quote = enclosure.charAt(0);

    ArrayValue array = new ArrayValueImpl();

    int ch;

    while (true) {
      // scan whitespace
      while (true) {
        ch = file.read();

        if (ch < 0 || ch == '\n')
          return array;
        else if (ch == '\r') {
          file.readOptionalLinefeed();
          return array;
        }
        else if (ch == ' ' || ch == '\t')
          continue;
        else
          break;
      }

      StringBuilder sb = new StringBuilder();

      if (ch == quote) {
        for (ch = file.read(); ch >= 0; ch = file.read()) {
          if (ch == quote) {
            ch = file.read();

            if (ch == quote)
              sb.append((char) ch);
            else
              break;
          }
          else
            sb.append((char) ch);
        }

        array.append(new StringValueImpl(sb.toString()));

        for (; ch >= 0 && ch == ' ' || ch == '\t'; ch = file.read()) {
        }
      }
      else {
        for (;
             ch >= 0 && ch != comma && ch != '\r' && ch != '\n';
             ch = file.read()) {
          sb.append((char) ch);
        }

        array.append(new StringValueImpl(sb.toString()));
      }

      if (ch < 0)
        return array;
      else if (ch == '\n')
        return array;
      else if (ch == '\r') {
        file.readOptionalLinefeed();
        return array;
      }
      else if (ch == comma) {
      }
      else {
        env.warning("expected comma");
      }
    }
  }

  /**
   * Returns the next line
   */
  public Value fgets(Env env, StreamResource file, @Optional long length)
    throws IOException
  {
    // quercus/1615

    if (file == null) {
      env.warning(L.l("{0} is null", "handle"));
      return BooleanValue.FALSE;
    }

    String value = file.readLine();

    if (value != null)
      return new StringValueImpl(value);
    else
      return BooleanValue.FALSE;
  }

  /**
   * Returns the next line stripping tags
   */
  public Value fgetss(Env env,
                      StreamResource file,
                      @Optional long length,
                      @Optional String allowedTags)
    throws IOException
  {
    // quercus/161a

    if (file == null) {
      env.warning(L.l("{0} is null", "handle"));
      return BooleanValue.FALSE;
    }

    String value = file.readLine();

    if (value != null)
      return new StringValueImpl(QuercusStringModule.strip_tags(value, allowedTags));
    else
      return BooleanValue.FALSE;
  }

  /**
   * Parses the file, returning it in an array.
   *
   * @param filename the file's name
   * @param useIncludePath if 1, use the include path
   * @param context the resource context
   */
  public static Value file(Env env,
                           String filename,
                           @Optional int useIncludePath,
                           @Optional Value context)
    throws IOException
  {
    Value fileValue = fopen(env, filename, "r", useIncludePath == 1, context);

    if (fileValue instanceof FileValue) {
      FileValue file = (FileValue) fileValue;

      try {
        ArrayValue result = new ArrayValueImpl();
        String line;

        while ((line = file.readLine()) != null)
          result.append(new StringValueImpl(line));

        return result;
      } finally {
        file.close();
      }
    }

    return BooleanValue.FALSE;
  }

  /**
   * Returns the file access time
   *
   * @param path the path to check
   */
  public static Value fileatime(Env env, Path path)
  {
    if (!path.canRead()) {
      env.warning(L.l("{0} cannot be read", path.getFullPath()));
      return BooleanValue.FALSE;
    }

    long time = path.getLastAccessTime();

    if (time <= 24 * 3600 * 1000L)
      return BooleanValue.FALSE;
    else
      return new LongValue(time / 1000L);
  }

  /**
   * Returns the file create time
   *
   * @param path the path to check
   */
  public static Value filectime(Env env, Path path)
  {
    if (!path.canRead()) {
      env.warning(L.l("{0} cannot be read", path.getFullPath()));
      return BooleanValue.FALSE;
    }

    long time = path.getCreateTime();

    if (time <= 24 * 3600 * 1000L)
      return BooleanValue.FALSE;
    else
      return new LongValue(time / 1000L);
  }

  /**
   * Returns the file's group
   *
   * @param path the path to check
   */
  public static Value filegroup(Env env, Path path)
  {
    if (!path.canRead()) {
      env.warning(L.l("{0} cannot be read", path.getFullPath()));
      return BooleanValue.FALSE;
    }

    return new LongValue(path.getGroup());
  }

  /**
   * Returns the file's inocde
   *
   * @param path the path to check
   */
  public static Value fileinode(Env env, Path path)
  {
    if (!path.canRead()) {
      env.warning(L.l("{0} cannot be read", path.getFullPath()));
      return BooleanValue.FALSE;
    }

    return new LongValue(path.getInode());
  }

  /**
   * Returns the file modified time
   *
   * @param path the path to check
   */
  public static Value filemtime(Env env, Path path)
  {
    if (!path.canRead()) {
      env.warning(L.l("{0} cannot be read", path.getFullPath()));
      return BooleanValue.FALSE;
    }

    long time = path.getLastModified();

    if (time <= 24 * 3600 * 1000L)
      return BooleanValue.FALSE;
    else
      return new LongValue(time / 1000L);
  }

  /**
   * Returns the file's owner
   *
   * @param path the path to check
   */
  public static Value fileowner(Env env, Path path)
  {
    if (!path.canRead()) {
      env.warning(L.l("{0} cannot be read", path.getFullPath()));
      return BooleanValue.FALSE;
    }

    return new LongValue(path.getOwner());
  }

  /**
   * Returns the file's permissions
   *
   * @param path the path to check
   */
  public static Value fileperms(Env env, Path path)
  {
    if (!path.canRead()) {
      env.warning(L.l("{0} cannot be read", path.getFullPath()));
      return BooleanValue.FALSE;
    }

    // XXX: stubbed (faked)

    int perms = 0;

    if (path.isDirectory()) {
      perms += 01000;
      perms += 0111;
    }

    if (path.canRead())
      perms += 0444;

    if (path.canWrite())
      perms += 0222;

    return new LongValue(perms);
  }

  /**
   * Returns the file's size
   *
   * @param path the path to check
   */
  public static Value filesize(Env env, Path path)
  {
    if (! path.exists() || ! path.isFile()) {
      env.warning(L.l("{0} cannot be read", path.getFullPath()));
      return BooleanValue.FALSE;
    }

    long length = path.getLength();

    if (length < 0)
      return BooleanValue.FALSE;
    else
      return new LongValue(length);
  }

  /**
   * Returns the file's type
   *
   * @param path the path to check
   */
  public static Value filetype(Env env, Path path)
  {
    // XXX: incomplete

    if (! path.exists()) {
      env.warning(L.l("{0} cannot be read", path.getFullPath()));
      return BooleanValue.FALSE;
    }
    else if (path.isDirectory())
      return new StringValueImpl("dir");
    else
      return new StringValueImpl("file");
  }

  /**
   * Returns true if file exists
   *
   * @param path the path to check
   */
  public static boolean file_exists(Path path)
  {
    return path.exists();
  }

  /**
   * Parses the file, returning it in an array.
   *
   * @param filename the file's name
   * @param useIncludePath if true, use the include path
   * @param context the resource context
   */
  public static Value file_get_contents(Env env,
                                        String filename,
                                        @Optional boolean useIncludePath,
                                        @Optional Value context,
                                        @Optional long offset,
                                        @Optional("4294967296") long maxLen)
    throws IOException
  {
    Value fileValue = fopen(env, filename, "r", useIncludePath, context);

    if (! (fileValue instanceof FileValue))
      return BooleanValue.FALSE;

    FileValue file = (FileValue) fileValue;

    try {
      StringBuilder sb = new StringBuilder();

      int ch;

      while ((ch = file.read()) >= 0) {
        sb.append((char) ch);
      }

      // XXX: handle offset and maxlen

      return new StringValueImpl(sb.toString());
    } finally {
      file.close();
    }
  }

  /**
   * Writes data to a file.
   */
  public Value file_put_contents(Env env,
                                 String filename,
                                 Value data,
                                 @Optional int flags,
                                 @Optional Value context)
    throws IOException
  {
    // quercus/1634

    // XXX: bug862, flags check for FILE_USE_INCLUDE_PATH, FILE_APPEND, LOCK_EX
    Value fileV = fopen(env, filename, "w", false, context);

    if (! (fileV instanceof StreamResource))
      return fileV;

    StreamResource file = (StreamResource) fileV;

    long dataWritten = 0;

    try {
      if (data instanceof ArrayValue) {
        for (Value item : ((ArrayValue) data).values()) {
          file.print(item.toString());
        }
      }
      else
        file.print(data.toString());
    } finally {
      file.close();
    }

    return new LongValue(dataWritten);
  }

  /**
   * Advisory locking
   *
   * @param fileV the file handle
   * @param operation the locking operation
   * @param wouldBlock the resource context
   */
  public static boolean flock(Env env,
                              Value fileV,
                              int operation,
                              @Optional Value wouldBlock)
    throws IOException
  {
    // XXX: stubbed,  also wouldblock is a ref

    if (fileV == null || fileV.isNull()) {
      env.warning(L.l("{0} is null", "handle"));
      return false;
    }

    return true;
  }

  // XXX: fnmatch

  /**
   * Opens a file.
   */
  public static Value fopen(Env env,
                            String filename,
                            String mode,
                            @Optional boolean useIncludePath,
                            @Optional Value context)
  {
    // XXX: useInputPath
    // XXX: context
    try {
      Path path = env.getPwd().lookup(filename);

      if (mode.startsWith("r")) {
        if (!path.canRead()) {
          env.warning(L.l("{0} cannot be read", path.getFullPath()));

          return BooleanValue.FALSE;
        }

        return new FileReadValue(path);
      }
      else if (mode.startsWith("w")) {
        if (path.exists() && !path.canWrite()) {
          env.warning(L.l("{0} cannot be written", path.getFullPath()));

          return BooleanValue.FALSE;
        }

        return new FileWriteValue(path);
      }
      else if (mode.startsWith("x") && ! path.exists())
        return new FileWriteValue(path);

      env.warning(L.l("bad mode `{0}'", mode));

      return NullValue.NULL;
    } catch (IOException e) {
      log.log(Level.FINE, e.toString(), e);

      return BooleanValue.FALSE;
    }
  }

  /**
   * Output the filepointer data to the output stream.
   */
  public Value fpassthru(Env env,
                         StreamResource is)
    throws IOException
  {
    // quercus/1635
    if (is == null) {
      env.warning(L.l("{0} is null", "handle"));
      return BooleanValue.FALSE;
    }

    TempBuffer temp = TempBuffer.allocate();
    byte []buffer = temp.getBuffer();

    int len;

    WriteStream out = env.getOut();

    long writeLength = 0;

    while ((len = is.read(buffer, 0, buffer.length)) > 0) {
      out.write(buffer, 0, len);

      writeLength += len;
    }

    TempBuffer.free(temp);

    return new LongValue(writeLength);
  }

  /**
   * Parses a comma-separated-value line from a file.
   *
   * @param file the file to read
   * @param delimiter optional comma replacement
   * @param enclosure optional quote replacement
   */
  public Value fputcsv(Env env,
                       StreamResource file,
                       ArrayValue value,
                       @Optional String delimiter,
                       @Optional String enclosure)
    throws IOException
  {
    // quercus/1636

    if (file == null) {
      env.warning(L.l("{0} is null", "handle"));
      return BooleanValue.FALSE;
    }

    if (value == null) {
      env.warning(L.l("{0} is null", "array"));
      return BooleanValue.FALSE;
    }

    char comma = ',';
    char quote = '\"';

    if (delimiter != null && delimiter.length() > 0)
      comma = delimiter.charAt(0);

    if (enclosure != null && enclosure.length() > 0)
      quote = enclosure.charAt(0);

    int writeLength = 0;
    boolean isFirst = true;

    for (Value data : value.values()) {
      if (! isFirst) {
        file.print(comma);
        writeLength++;
      }
      isFirst = false;

      String s = data.toString();
      int strlen = s.length();

      writeLength++;
      file.print(quote);

      for (int i = 0; i < strlen; i++) {
        char ch = s.charAt(i);

        if (ch != quote) {
          file.print(ch);
          writeLength++;
        }
        else {
          file.print(quote);
          file.print(quote);
          writeLength += 2;
        }
      }

      file.print(quote);
      writeLength++;
    }

    file.print("\n");
    writeLength++;

    return LongValue.create(writeLength);
  }

  /**
   * Writes a string to the file.
   */
  public static Value fputs(Env env, Value fileV, String value, @Optional("-1") int length)
    throws IOException
  {
    return fwrite(env, fileV, value, length);
  }

  /**
   * Reads content from a file.
   *
   * @param fileValue the file
   */
  public static Value fread(Env env, Value fileValue, int length)
    throws IOException
  {
    if (! (fileValue instanceof FileValue)) {
      env.warning(L.l("bad {0}", "handle"));
      return BooleanValue.FALSE;
    }

    FileValue file = (FileValue) fileValue;

    TempBuffer tempBuf = TempBuffer.allocate();
    byte []buffer = tempBuf.getBuffer();

    if (buffer.length < length)
      length = buffer.length;

    length = file.read(buffer, 0, length);

    Value s;

    if (length > 0)
      s = env.createString(buffer, 0, length);
    else
      s = BooleanValue.FALSE;

    TempBuffer.free(tempBuf);

    return s;
  }

  /**
   * Reads and parses a line.
   */
  public static Value fscanf(Env env,
                             StreamResource file,
                             String format,
                             @Optional Value []args)
    throws IOException
  {

    if (file == null) {
      env.warning(L.l("{0} is null", "handle"));
      return BooleanValue.FALSE;
    }

    String value = file.readLine();

    if (value == null)
      return BooleanValue.FALSE;

    return QuercusStringModule.sscanf(value, format, args);
  }

  // XXX: fseek
  // XXX: fstat

  /**
   * Returns the current position.
   *
   * @param file the stream to test
   */
  public static Value ftell(Env env, StreamResource file)
  {
    if (file == null) {
      env.warning(L.l("{0} is null", "handle"));
      return BooleanValue.FALSE;
    }
    else
      return new LongValue(file.getPosition());
  }

  // XXX: ftruncate

  /**
   * Writes a string to the file.
   */
  public static Value fwrite(Env env, Value fileV, String value,  @Optional("-1") int length)
    throws IOException
  {
    if (! (fileV instanceof FileValue)) {
      env.warning(L.l("bad {0}", "handle"));
      return BooleanValue.FALSE;
    }

    FileValue file = (FileValue) fileV;

    file.print(value);

    return BooleanValue.TRUE;
  }

  // XXX: glob

  /**
   * Returns the current working directory.
   *
   * @return the current directory
   */
  public static String getcwd(Env env)
  {
    return env.getPwd().getNativePath();
  }

  /**
   * Returns true if the path is a directory.
   *
   * @param path the path to check
   */
  public static boolean is_dir(Path path)
  {
    return path.isDirectory();
  }

  /**
   * Returns true if the path is an executable file
   *
   * @param path the path to check
   */
  public static boolean is_executable(Path path)
  {
    return path.isExecutable();
  }

  /**
   * Returns true if the path is a file.
   *
   * @param path the path to check
   */
  public static boolean is_file(Path path)
  {
    return path.isFile();
  }

  /**
   * Returns true if the path is a symbolic link
   *
   * @param path the path to check
   */
  public static boolean is_link(Env env, Path path)
  {
    // XXX: check win behaviour
    env.warning(L.l("is_link is not supported"));

    return false;
  }

  /**
   * Returns true if the path is readable
   *
   * @param path the path to check
   */
  public static boolean is_readable(Path path)
  {
    return path.canRead();
  }

  /**
   * Returns true for an uploaded file.
   *
   * @param tail the temp name of the uploaded file
   */
  public static boolean is_uploaded_file(Env env, String tail)
  {
    return env.getUploadDirectory().lookup(tail).canRead();
  }

  /**
   * Returns true if the path is writable
   *
   * @param path the path to check
   */
  public static boolean is_writable(Path path)
  {
    return path.canWrite();
  }

  /**
   * Returns true if the path is writable
   *
   * @param path the path to check
   */
  public static boolean is_writeable(Path path)
  {
    return is_writable(path);
  }

  /**
   * Creates a hard link
   */
  public boolean link(Env env, Path source, Path destination)
  {
    // XXX: check win behaviour
    env.warning(L.l("link is not supported"));

    return false;
  }

  // XXX: linkinfo
  // XXX: lstat

  /**
   * Makes the directory
   *
   * @param path the directory to make
   */
  public static boolean mkdir(Env env, Path path)
  {
    try {
      return path.mkdir();
    } catch (IOException e) {
      log.log(Level.FINE, e.toString(), e);

      return false;
    }
  }

  /**
   * Moves the uploaded file.
   *
   * @param tail the temp name of the uploaded file
   * @param dst the destination path
   */
  public static boolean move_uploaded_file(Env env, String tail, Path dst)
  {
    Path src = env.getUploadDirectory().lookup(tail);

    try {
      if (src.canRead()) {
        src.renameTo(dst);
        return true;
      }
      else
        return false;
    } catch (IOException e) {
      env.warning(e);

      return false;
    }
  }

  /**
   * Opens a directory
   *
   * @param pathName the directory to open
   */
  public static Value opendir(Env env, String pathName,
                              @Optional Value context)
    throws IOException
  {
    Path path = env.getPwd().lookup(pathName);

    if (path.isDirectory()) {
      return new DirectoryValue(path);
    }
    else {
      env.warning(L.l("{0} is not a directory", path.getFullPath()));

      return BooleanValue.FALSE;
    }
  }

  // XXX: parse_ini_file
  // XXX: pathinfo
  // XXX: pclose
  // XXX: popen

  /**
   * Reads the next entry
   *
   * @param dirV the directory resource
   */
  public static Value readdir(Env env, Value dirV)
    throws Exception
  {
    if (! (dirV instanceof DirectoryValue)) {
      env.warning(L.l("{0} is not a directory", dirV));
      return BooleanValue.FALSE;
    }

    DirectoryValue dir = (DirectoryValue) dirV;

    return dir.readdir();
  }

  /**
   * Read the contents of a file and write them out.
   */
  public Value readfile(Env env,
                        String filename,
                        @Optional boolean useIncludePath,
                        @Optional Value context)
  {
    Value fileValue = fopen(env, filename, "r", useIncludePath, context);

    Value result = BooleanValue.FALSE;

    try {
      if (fileValue instanceof StreamResource) {
        StreamResource streamValue = (StreamResource) fileValue;

        result = fpassthru(env, streamValue);
        fclose(env, streamValue);
      }
    }
    catch (IOException e) {
      log.log(Level.FINE, e.toString(), e);
    }

    return result;
  }

  // XXX: readlink
  // XXX: realpath

  /**
   * Renames a file
   *
   * @param fromPath the path to change to
   * @param toPath the path to change to
   */
  public static boolean rename(Env env, Path fromPath, Path toPath)
  {
    if (!fromPath.canRead()) {
      env.warning(L.l("{0} cannot be read", fromPath.getFullPath()));
      return false;
    }

    try {
      return fromPath.renameTo(toPath);
    } catch (IOException e) {
      log.log(Level.FINE, e.toString(), e);

      return false;
    }
  }

  // XXX: rewind

  /**
   * Rewinds the directory listing
   *
   * @param dirV the directory resource
   */
  public static Value rewinddir(Env env, Value dirV)
    throws Exception
  {
    if (! (dirV instanceof DirectoryValue)) {
      env.warning(L.l("{0} is not a directory", dirV));
      return BooleanValue.FALSE;
    }

    DirectoryValue dir = (DirectoryValue) dirV;

    return dir.rewinddir();
  }

  /**
   * remove a directory
   */
  public static boolean rmdir(Env env,
                              String filename,
                              @Optional Value context)
  {
    // quercus/160s

    // XXX: safe_mode
    try {
      Path path = env.getPwd().lookup(filename);

      if (!path.isDirectory()) {
        env.warning(L.l("{0} is not a directory", path.getFullPath()));
        return false;
      }

      return path.remove();
    } catch (IOException e) {
      log.log(Level.FINE, e.toString(), e);

      return false;
    }
  }

  /**
   * Closes the directory
   *
   * @param dirV the directory resource
   */
  public static Value closedir(Env env, Value dirV)
    throws IOException
  {
    return BooleanValue.TRUE;
  }

  /**
   * Scan the directory
   *
   * @param fileName the directory
   */
  public static Value scandir(Env env, String fileName,
                              @Optional("1") int order,
                              @Optional Value context)
    throws IOException
  {
    Path path = env.getPwd().lookup(fileName);

    if (!path.isDirectory()) {
      env.warning(L.l("{0} is not a directory", path.getFullPath()));
      return BooleanValue.FALSE;
    }

    String []values = path.list();

    Arrays.sort(values);

    ArrayValue result = new ArrayValueImpl();

    if (order == 1) {
      for (int i = 0; i < values.length; i++)
        result.append(new LongValue(i), new StringValueImpl(values[i]));
    }
    else {
      for (int i = values.length - 1; i >= 0; i--) {
        result.append(new LongValue(values.length - i - 1),
                      new StringValueImpl(values[i]));
      }
    }

    return result;
  }

  /**
   * Sets the write buffer.
   */
  public static int set_file_buffer(StreamResource stream,
                                    int bufferSize)
  {
    return QuercusStreamModule.stream_set_write_buffer(stream, bufferSize);
  }

  /**
   * Returns file statistics
   */
  public static Value stat(Env env, Path path)
  {
    // XXX: what if it doesn't exist?
    ArrayValue result = new ArrayValueImpl();

    result.put(new StringValueImpl("size"), new LongValue(path.getLength()));

    return result;
  }

  /**
   * Creates a symlink
   */
  public boolean symlink(Env env, Path source, Path destination)
  {
    // XXX: check win behaviour
    env.warning(L.l("symlink is not supported"));

    return false;
  }

  /**
   * Creates a temporary file.
   */
  public static Value tempnam(Env env, Path dir, String prefix)
  {
    // quercus/160u

    if (!dir.isDirectory()) {
      env.warning(L.l("{0} is not a directory", dir.getFullPath()));
      return BooleanValue.FALSE;
    }

    try {
      // XXX: remove on exit

      Path path = dir.createTempFile(prefix, ".tmp");
      return new StringValueImpl(path.getTail());
    } catch (IOException e) {
      log.log(Level.FINE, e.toString(), e);

      return BooleanValue.FALSE;
    }
  }

  /**
   * Creates a temporary file.
   */
  public static Value tmpfile(Env env)
  {
    try {
      // XXX: remove on exit
      // XXX: location of tmp files s/b configurable

      Path tmp = env.getPwd().lookup("/tmp");

      tmp.mkdirs();

      Path file = tmp.createTempFile("resin", "tmp");

      return new FileWriteValue(file);
    } catch (IOException e) {
      log.log(Level.FINE, e.toString(), e);

      return NullValue.NULL;
    }
  }

  /**
   * sets the time to the current time
   */
  public static boolean touch(Path path,
                              @Optional int time,
                              @Optional int atime)
  {
    // XXX: atime not implemented (it might be > time)

    try {
      if (path.exists()) {
        if (time > 0)
          path.setLastModified(1000L * time);
        else
          path.setLastModified(Alarm.getCurrentTime());
      }
      else {
        WriteStream ws = path.openWrite();
        ws.close();
      }

      return true;
    } catch (IOException e) {
      log.log(Level.FINE, e.toString(), e);

      return false;
    }
  }

  /**
   * umask call
   */
  public static int umask(int mask)
  {
    // XXX: stub

    return mask;
  }

  /**
   * remove call
   */
  public static boolean unlink(Env env,
                               String filename,
                               @Optional Value context)
  {
    // quercus/160p

    // XXX: safe_mode
    try {
      Path path = env.getPwd().lookup(filename);

      return path.remove();
    } catch (IOException e) {
      log.log(Level.FINE, e.toString(), e);

      return false;
    }
  }
}

