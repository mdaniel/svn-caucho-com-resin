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

package com.caucho.quercus.lib.file;

import java.io.InputStream;
import java.io.IOException;

import java.util.HashMap;
import java.util.Map;
import java.util.Arrays;

import java.util.logging.Logger;
import java.util.logging.Level;

import com.caucho.util.L10N;

import com.caucho.quercus.QuercusModuleException;

import com.caucho.quercus.module.AbstractQuercusModule;
import com.caucho.quercus.module.Optional;

import com.caucho.quercus.env.*;

import com.caucho.quercus.resources.StreamResource;
import com.caucho.quercus.lib.file.DirectoryValue;
import com.caucho.quercus.lib.file.FileReadValue;
import com.caucho.quercus.lib.file.FileValue;
import com.caucho.quercus.lib.file.FileWriteValue;
import com.caucho.quercus.lib.string.StringModule;
import com.caucho.quercus.lib.StreamModule;

import com.caucho.quercus.module.NotNull;
import com.caucho.quercus.module.ReturnNullAsFalse;

import com.caucho.util.Alarm;

import com.caucho.vfs.WriteStream;
import com.caucho.vfs.ReadStream;
import com.caucho.vfs.TempBuffer;
import com.caucho.vfs.Path;

/**
 * Information and actions for about files
 */
public class FileModule extends AbstractQuercusModule {
  private static final L10N L = new L10N(FileModule.class);
  private static final Logger log
    = Logger.getLogger(FileModule.class.getName());

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

  public static final int FILE_USE_INCLUDE_PATH = 1;
  public static final int FILE_APPEND = 2;

  private static final HashMap<String,StringValue> _iniMap
    = new HashMap<String,StringValue>();

  /**
   * Returns the default quercus.ini values.
   */
  public Map<String,StringValue> getDefaultIni()
  {
    return _iniMap;
  }
  
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
  {
    try {
      if (! path.isDirectory()) {
	env.warning(L.l("{0} is not a directory", path.getFullPath()));

	return BooleanValue.FALSE;
      }

      DirectoryValue dir = new DirectoryValue(path);

      env.addClose(dir);

      return dir;
    } catch (IOException e) {
      throw new QuercusModuleException(e);
    }
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
  public static boolean fclose(Env env, @NotNull BinaryStream s)
  {
    if (s == null)
      return false;

    s.close();

    return true;
  }

  /**
   * Checks for the end of file.
   */
  public static boolean feof(Env env, @NotNull BinaryInput is)
  {
    if (is == null)
      return false;

    return is.isEOF();
  }

  /**
   * Flushes a file.
   */
  public static boolean fflush(Env env, @NotNull BinaryOutput os)
  {
    if (os == null)
      return false;

    os.flush();

    return true;
  }

  /**
   * Returns the next character as a boolean
   */
  public static Value fgetc(Env env, @NotNull BinaryInput is)
  {
    try {
      if (is == null)
	return BooleanValue.FALSE;

      // php/1612
      int ch = is.read();

      if (ch >= 0)
	return new BinaryBuilderValue(new byte[] { (byte) ch });
      else
	return BooleanValue.FALSE;
    } catch (IOException e) {
      throw new QuercusModuleException(e);
    }
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
                       @NotNull StreamResource file,
                       @Optional int length,
                       @Optional String delimiter,
                       @Optional String enclosure)
  {
    // php/1619

    try {
      if (file == null)
	return BooleanValue.FALSE;

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
    } catch (IOException e) {
      throw new QuercusModuleException(e);
    }
  }

  /**
   * Returns the next line
   */
  public static Value fgets(Env env,
			    @NotNull BinaryInput is,
			    @Optional("0x7fffffff") int length)
  {
    // php/1615

    try {
      if (is == null)
	return BooleanValue.FALSE;

      StringValue value = is.readLine(length);

      if (value != null)
	return value;
      else
	return BooleanValue.FALSE;
    } catch (IOException e) {
      throw new QuercusModuleException(e);
    }
  }

  /**
   * Returns the next line stripping tags
   */
  public static Value fgetss(Env env,
			     BinaryInput is,
			     @Optional("0x7fffffff") int length,
			     @Optional String allowedTags)
  {
    // php/161a

    try {
      if (is == null) {
	env.warning(L.l("{0} is null", "handle"));
	return BooleanValue.FALSE;
      }

      StringValue value = is.readLine(length);

      if (value != null)
	return StringModule.strip_tags(value, allowedTags);
      else
	return BooleanValue.FALSE;
    } catch (IOException e) {
      throw new QuercusModuleException(e);
    }
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
                           @Optional boolean useIncludePath,
                           @Optional Value context)
  {
    try {
      BinaryStream stream = fopen(env, filename, "r", useIncludePath, context);

      if (stream == null)
	return BooleanValue.FALSE;

      BinaryInput is = (BinaryInput) stream;

      try {
	ArrayValue result = new ArrayValueImpl();
	StringValue line;

	while ((line = is.readLine(Integer.MAX_VALUE)) != null)
	  result.append(line);

	return result;
      } finally {
	is.close();
      }
    } catch (IOException e) {
      throw new QuercusModuleException(e);
    }
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
  @ReturnNullAsFalse
  public static BinaryValue
    file_get_contents(Env env,
		      String filename,
		      @Optional boolean useIncludePath,
		      @Optional Value context,
		      @Optional long offset,
		      @Optional("4294967296") long maxLen)
  {
    try {
      BinaryStream s = fopen(env, filename, "r", useIncludePath, context);

      if (! (s instanceof BinaryInput))
	return null;

      BinaryInput is = (BinaryInput) s;

      try {
	BinaryBuilderValue bb = new BinaryBuilderValue();

	int len;

	do {
	  bb.prepareReadBuffer();

	  len = is.read(bb.getBuffer(), bb.getOffset(),
			bb.getLength() - bb.getOffset());

	  if (len > 0)
	    bb.setOffset(bb.getOffset() + len);
	} while (len > 0);

	return bb;
      } finally {
	is.close();
      }
    } catch (IOException e) {
      throw new QuercusModuleException(e);
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
  {
    // php/1634

    try {
      // XXX: bug862, flags check for FILE_USE_INCLUDE_PATH, FILE_APPEND, LOCK_EX

      boolean useIncludePath = (flags & FILE_USE_INCLUDE_PATH) != 0;
      String mode = (flags & FILE_APPEND) != 0 ? "a" : "w";
      
      BinaryStream s = fopen(env, filename, mode, useIncludePath, context);

      if (! (s instanceof BinaryOutput))
	return BooleanValue.FALSE;

      BinaryOutput os = (BinaryOutput) s;

      try {
	long dataWritten = 0;

	if (data instanceof ArrayValue) {
	  for (Value item : ((ArrayValue) data).values()) {
	    os.print(item.toString());
	  }
	}
	else
	  os.print(data.toString());

	return new LongValue(dataWritten);
      } finally {
	os.close();
      }
    } catch (IOException e) {
      throw new QuercusModuleException(e);
    }
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
   *
   * @param filename the path to the file to open
   * @param mode the mode the file should be opened as.
   * @param useIncludePath if true, search the current include path
   */
  @ReturnNullAsFalse
  public static BinaryStream fopen(Env env,
				   String filename,
				   String mode,
				   @Optional boolean useIncludePath,
				   @Optional Value context)
  {
    // XXX: context
    try {
      Path path;

      if (useIncludePath)
	path = env.lookupInclude(filename);
      else
	path = env.getPwd().lookup(filename);

      if (mode.startsWith("r+")) {
	throw new UnsupportedOperationException("read/write not yet supported");
      }
      else if (mode.startsWith("r")) {
	try {
	  return new FileInput(path);
	} catch (IOException e) {
	  log.log(Level.FINE, e.toString(), e);

          env.warning(L.l("{0} cannot be read", path.getFullPath()));

          return null;
        }
      }
      else if (mode.startsWith("w+")) {
	throw new UnsupportedOperationException("read/write not yet supported");
      }
      else if (mode.startsWith("w")) {
	try {
	  return new FileOutput(path);
	} catch (IOException e) {
	  log.log(Level.FINE, e.toString(), e);

          env.warning(L.l("{0} cannot be written", path.getFullPath()));

          return null;
        }
      }
      else if (mode.startsWith("a+")) {
	throw new UnsupportedOperationException("read/write not yet supported");
      }
      else if (mode.startsWith("a")) {
	try {
	  return new FileOutput(path, true);
	} catch (IOException e) {
	  log.log(Level.FINE, e.toString(), e);

          env.warning(L.l("{0} cannot be written", path.getFullPath()));

          return null;
        }
      }
      else if (mode.startsWith("x+")) {
	throw new UnsupportedOperationException("read/write not yet supported");
      }
      else if (mode.startsWith("x") && ! path.exists())
        return new FileOutput(path);

      env.warning(L.l("bad mode `{0}'", mode));

      return null;
    } catch (IOException e) {
      log.log(Level.FINE, e.toString(), e);
      
      env.warning(L.l("{0} can't be opened.\n{1}",
		      filename, e.toString()));

      return null;
    }
  }

  /**
   * Output the filepointer data to the output stream.
   */
  public Value fpassthru(Env env, @NotNull BinaryInput is)
  {
    // php/1635

    try {
      if (is == null)
	return BooleanValue.FALSE;

      WriteStream out = env.getOut();

      long writeLength = out.writeStream(is.getInputStream());

      return LongValue.create(writeLength);
    } catch (IOException e) {
      throw new QuercusModuleException(e);
    }
  }

  /**
   * Parses a comma-separated-value line from a file.
   *
   * @param file the file to read
   * @param delimiter optional comma replacement
   * @param enclosure optional quote replacement
   */
  public Value fputcsv(Env env,
                       @NotNull StreamResource file,
                       @NotNull ArrayValue value,
                       @Optional String delimiter,
                       @Optional String enclosure)
  {
    // php/1636

    try {
      if (file == null)
	return BooleanValue.FALSE;

      if (value == null)
	return BooleanValue.FALSE;

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
    } catch (IOException e) {
      throw new QuercusModuleException(e);
    }
  }

  /**
   * Writes a string to the file.
   */
  public static Value fputs(Env env,
			    BinaryOutput os,
			    InputStream value,
			    @Optional("-1") int length)
  {
    return fwrite(env, os, value, length);
  }

  /**
   * Reads content from a file.
   *
   * @param fileValue the file
   */
  public static Value fread(Env env,
			    @NotNull StreamResource file,
			    int length)
  {
    try {
      if (file == null)
	return BooleanValue.FALSE;

      TempBuffer tempBuf = TempBuffer.allocate();
      byte []buffer = tempBuf.getBuffer();

      if (buffer.length < length)
	length = buffer.length;

      length = file.read(buffer, 0, length);

      Value s;

      if (length > 0) {
	BinaryBuilderValue bb = new BinaryBuilderValue(buffer, 0, length);
	TempBuffer.free(tempBuf);
	return bb;
      }
      else {
	TempBuffer.free(tempBuf);
	return BooleanValue.FALSE;
      }
    } catch (IOException e) {
      throw new QuercusModuleException(e);
    }
  }

  /**
   * Reads and parses a line.
   */
  public static Value fscanf(Env env,
                             @NotNull StreamResource file,
                             StringValue format,
                             @Optional Value []args)
  {
    try {
      if (file == null)
	return BooleanValue.FALSE;

      StringValue value = file.readLine();

      if (value == null)
	return BooleanValue.FALSE;

      return StringModule.sscanf(value, format, args);
    } catch (IOException e) {
      throw new QuercusModuleException(e);
    }
  }

  // XXX: fseek
  // XXX: fstat

  /**
   * Returns the current position.
   *
   * @param file the stream to test
   */
  public static Value ftell(Env env,
			    @NotNull StreamResource file)
  {
    if (file == null)
      return BooleanValue.FALSE;

    return new LongValue(file.getPosition());
  }

  // XXX: ftruncate

  /**
   * Writes a string to the file.
   */
  public static Value fwrite(Env env,
			     @NotNull BinaryOutput os,
			     InputStream value,
			     @Optional("0x7fffffff") int length)
  {
    try {
      if (os == null)
	return BooleanValue.FALSE;

      return LongValue.create(os.write(value, length));
    } catch (IOException e) {
      throw new QuercusModuleException(e);
    }
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

  /**
   * Returns file statistics
   */
  public static Value lstat(Env env, Path path)
  {
    return stat(env, path);
  }

  /**
   * Makes the directory
   *
   * @param path the directory to make
   */
  public static boolean mkdir(Env env, Path path,
			      @Optional int mode,
			      @Optional boolean recursive,
			      @Optional Value context)
  {
    try {
      if (recursive)
	return path.mkdirs();
      else
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
  {
    try {
      Path path = env.getPwd().lookup(pathName);

      if (path.isDirectory()) {
	return new DirectoryValue(path);
      }
      else {
	env.warning(L.l("{0} is not a directory", path.getFullPath()));

	return BooleanValue.FALSE;
      }
    } catch (IOException e) {
      throw new QuercusModuleException(e);
    }
  }

  /**
   * Parses the ini file.
   */
  public static Value parse_ini_file(Env env,
				     Path path,
				     @Optional boolean processSections)
  {
    ReadStream is = null;
    
    try {
      is = path.openRead();
      is.setEncoding(env.getScriptEncoding());

      return parseIni(env, is, processSections);
    } catch (IOException e) {
      env.warning(e);

      return BooleanValue.FALSE;
    } finally {
      try {
	if (is != null)
	  is.close();
      } catch (IOException e) {
      }
    }
  }

  private static ArrayValue parseIni(Env env,
				     ReadStream is,
				     boolean processSections)
    throws IOException
  {
    ArrayValue top = new ArrayValueImpl();
    ArrayValue section = top;
    
    int ch;

    while ((ch = is.read()) >= 0) {
      if (Character.isWhitespace(ch)) {
      }
      else if (ch == ';') {
	for (; ch >= 0 && ch != '\r' && ch != '\n'; ch = is.read()) {
	}
      }
      else if (ch == '[') {
	StringBuilder sb = new StringBuilder();

	for (ch = is.read(); ch >= 0 && ch != ']'; ch = is.read()) {
	  sb.append((char) ch);
	}

	String name = sb.toString().trim();

	if (processSections) {
	  section = new ArrayValueImpl();
	  top.put(new StringValueImpl(name), section);
	}
      }
      else if (Character.isJavaIdentifierStart((char) ch)) {
	StringBuilder sb = new StringBuilder();

	for (; Character.isJavaIdentifierPart((char) ch); ch = is.read()) {
	  sb.append((char) ch);
	}

	String key = sb.toString().trim();

	for (; ch >= 0 && ch != '='; ch = is.read()) {
	}

	for (ch = is.read(); ch == ' ' || ch == '\t'; ch = is.read()) {
	}

	Value value = parseIniValue(env, ch, is);

	section.put(new StringValueImpl(key), value);
      }
    }

    return top;
  }

  private static Value parseIniValue(Env env, int ch, ReadStream is)
    throws IOException
  {
    if (ch == '\r' || ch == '\n')
      return NullValue.NULL;

    if (ch == '"') {
      StringBuilder sb = new StringBuilder();
      
      for (ch = is.read(); ch >= 0 && ch != '"'; ch = is.read()) {
	sb.append((char) ch);
      }

      skipToEndOfLine(ch, is);

      return new StringValueImpl(sb.toString());
    }
    else if (ch == '\'') {
      StringBuilder sb = new StringBuilder();
      
      for (ch = is.read(); ch >= 0 && ch != '\''; ch = is.read()) {
	sb.append((char) ch);
      }

      skipToEndOfLine(ch, is);

      return new StringValueImpl(sb.toString());
    }
    else {
      StringBuilder sb = new StringBuilder();

      for (; ch >= 0 && ch != '\r' && ch != '\n'; ch = is.read()) {
	sb.append((char) ch);
      }

      String value = sb.toString().trim();

      if (value.equalsIgnoreCase("null"))
	return StringValue.EMPTY;
      else if (value.equalsIgnoreCase("true") ||
	       value.equalsIgnoreCase("yes"))
	return new StringValueImpl("1");
      else if (value.equalsIgnoreCase("false") ||
	       value.equalsIgnoreCase("no"))
	return StringValue.EMPTY;

      if (env.isDefined(value))
	return new StringValueImpl(env.getConstant(value).toString());
      else
	return new StringValueImpl(value);
    }
  }
  
  private static void skipToEndOfLine(int ch, ReadStream is)
    throws IOException
  {
    for (; ch > 0 && ch != '\r' && ch != '\n'; ch = is.read()) {
    }
  }

  /**
   * Parses the path, splitting it into parts.
   */
  public static Value pathinfo(String path)
  {
    int p = path.lastIndexOf('/');

    String dirname;
    if (p >= 0) {
      dirname = path.substring(0, p);
      path = path.substring(p + 1);
    }
    else {
      dirname = "";
    }

    p = path.indexOf('.');
    String ext = "";
    if (p > 0)
      ext = path.substring(p + 1);

    ArrayValueImpl value = new ArrayValueImpl();

    value.put("dirname", dirname);
    value.put("basename", path);
    value.put("extension", ext);

    return value;
  }

  // XXX: pclose
  // XXX: popen

  /**
   * Reads the next entry
   *
   * @param dirV the directory resource
   */
  public static Value readdir(Env env,
			      @NotNull DirectoryValue dir)
  {
    if (dir == null)
      return BooleanValue.FALSE;

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
    BinaryStream s = fopen(env, filename, "r", useIncludePath, context);

    if (! (s instanceof BinaryInput))
      return BooleanValue.FALSE;

    BinaryInput is = (BinaryInput) s;

    try {
      return fpassthru(env, is);
    } finally {
      is.close();
    }
  }

  /**
   * The readlink always fails.
   */
  public static boolean readlink(Env env, String path)
  {
    env.stub("readlink(" + path + ")");

    return false;
  }

  /**
   * Returns the actual path name.
   */
  public static String realpath(Path path)
  {
    return path.getFullPath();
  }

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
  public static Value rewinddir(Env env,
				@NotNull DirectoryValue dir)
  {
    if (dir == null)
      return BooleanValue.FALSE;

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
  {
    try {
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
    } catch (IOException e) {
      throw new QuercusModuleException(e);
    }
  }

  /**
   * Sets the write buffer.
   */
  public static int set_file_buffer(StreamResource stream,
                                    int bufferSize)
  {
    return StreamModule.stream_set_write_buffer(stream, bufferSize);
  }

  /**
   * Returns file statistics
   */
  public static Value stat(Env env, Path path)
  {
    // XXX: what if it doesn't exist?
    ArrayValue result = new ArrayValueImpl();

    int mode = 0;
    if (path.isDirectory())
      mode |= 0040111; // S_IFDIR
    else
      mode |= 0100000; // S_IFREG
    
    if (path.canRead())
      mode |= 0444;
    if (path.canWrite())
      mode |= 0220;
    if (path.canExecute())
      mode |= 0110;

    result.put(0); // dev
    result.put(0); // ino
    result.put(mode);
    result.put(1); // nlink
    result.put(501); // uid
    result.put(501); // gid
    result.put(0); // rdev
    result.put(path.getLength()); // size

    result.put(path.getLastModified() / 1000); // atime
    result.put(path.getLastModified() / 1000); // mtime
    result.put(path.getLastModified() / 1000); // ctime
    result.put(4096); // blksize
    result.put((path.getLength() + 4095) / 4096); // blocks

    result.put("dev", 0);
    result.put("ino", 0);
    
    result.put("mode", mode);
    result.put("nlink", 1);
    result.put("uid", 501);
    result.put("gid", 501);
    result.put("rdev", 0);
    
    result.put(new StringValueImpl("size"), new LongValue(path.getLength()));

    result.put("atime", path.getLastModified() / 1000);
    result.put("mtime", path.getLastModified() / 1000);
    result.put("ctime", path.getLastModified() / 1000);
    result.put("blksize", 4096);
    result.put("blocks", (path.getLength() + 4095) / 4096);

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
  public static int umask(Env env, int mask)
  {
    env.stub("umask(" + mask + ")");

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

  static {
    addIni(_iniMap, "allow_url_fopen", "1", PHP_INI_SYSTEM);
    addIni(_iniMap, "user_agent", null, PHP_INI_ALL);
    addIni(_iniMap, "default_socket_timeout", "60", PHP_INI_ALL);
    addIni(_iniMap, "from", "", PHP_INI_ALL);
    addIni(_iniMap, "auto_detect_line_endings", "0", PHP_INI_ALL);
  }
}

