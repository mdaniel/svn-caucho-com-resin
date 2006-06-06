/*
 * Copyright (c) 1998-2005 Caucho Technology -- all rights reserved
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
 * @author Charles Reich
 */

package com.caucho.quercus.lib.zlib;

import java.io.ByteArrayInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.GZIPInputStream;
import java.util.zip.CRC32;

import com.caucho.quercus.QuercusModuleException;

import com.caucho.quercus.env.ArrayValue;
import com.caucho.quercus.env.ArrayValueImpl;
import com.caucho.quercus.env.BinaryBuilderValue;
import com.caucho.quercus.env.BooleanValue;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.StringBuilderValue;
import com.caucho.quercus.env.StringValueImpl;
import com.caucho.quercus.env.Value;
import com.caucho.quercus.module.NotNull;
import com.caucho.quercus.module.Optional;
import com.caucho.quercus.lib.file.FileValue;
import com.caucho.quercus.lib.file.FileModule;
import com.caucho.quercus.lib.string.StringModule;
import com.caucho.util.ByteBuffer;
import com.caucho.util.L10N;
import com.caucho.vfs.Path;
import com.caucho.vfs.TempBuffer;

/**
 * Zlib object oriented API facade
 */

public class Zlib {
  private static final Logger log = Logger.getLogger(Zlib.class.getName());
  private static final L10N L = new L10N(Zlib.class);

  private BufferedReader _bufferedReader;
  private GZIPFileValueWriter _gout;

  private Path _path;
  private Value _fileValue; // Created by fopen... can be BooleanValue.FALSE

  /**
   * XXX: todo - implement additional read/write modes (a,+,etc)
   *
   * Creates and sets GZIP stream if mode is 'w'
   * Also creates _fileValue.  All write functions are wrappers around
   * the _fileValue functions using _deflater to compress the
   * byte stream.
   *
   * All read functions use _bufferedReader;
   *
   * @param fileName
   * @param mode (ie: "w9" or "r7f")
   * @param useIncludePath is always on
   */
  public Zlib(Env env,
              String filename,
              String mode,
              boolean useIncludePath)
  {

    String filemode = getFileMode( mode );
    int compressionLevel = getCompressionLevel( mode );
    int compressionStrategy = getCompressionStrategy( mode );

    try {
      if( filemode.equals("r") )
      {
        _fileValue = FileModule.fopen(env, filename, mode, useIncludePath, null);
        _path = env.getPwd().lookup(filename);
        getBufferedReader();
      }
      else if( filemode.equals("w") )
      {
        _fileValue = FileModule.fopen(env, filename, mode, useIncludePath, null);
        _path = env.getPwd().lookup(filename);
        _gout = new GZIPFileValueWriter( compressionLevel, compressionStrategy, (FileValue)_fileValue );
      }
    }
    catch (IOException e) {
      log.log(Level.FINE, e.getMessage(), e);
      env.warning(L.l(e.getMessage()));
    }
  }


  /**
   * Reads from the input and writes to the gzip stream
   * @param s
   * @param length # of bytes to compress
   * @return # of uncompressed bytes
   */
  public int gzwrite(Env env,
		     InputStream is, int length )
  {
    if ((_fileValue == null) || (_fileValue == BooleanValue.FALSE)) {
      env.warning(L.l("file could not be open for writing"));
      return -1;
    }

    // default size that would have been allocated by ByteBuffer
    byte[] buffer = new byte[32];

    int inputSize = 0;
    int bytesin = 0;

    try {
      if( length == 0 ) {
        for (bytesin=is.read(buffer, 0, buffer.length); bytesin > 0; bytesin=is.read(buffer, 0, buffer.length)) {
          _gout.write(buffer, 0, bytesin);
          inputSize += bytesin;
        }
      }
      else {
        for (int left = length; left > 0; left -= bytesin ) {
          if (buffer.length < left)
            bytesin = buffer.length;
          else
            bytesin = left;

          bytesin = is.read(buffer, 0, bytesin);

          if (bytesin <= 0)
            break;

          _gout.write(buffer, 0, bytesin);
          inputSize += bytesin;
        }
      }

    }
    catch (IOException e) {
      log.log(Level.FINE, e.getMessage(), e);
      env.warning(L.l(e.getMessage()));
    }

    return inputSize;
  }


  /**
   * Closes the gzip stream
   * @return true if successful, false otherwise
   */
  public boolean gzclose()
  {
    if ((_fileValue == null) || (_fileValue == BooleanValue.FALSE)) {
      return false;
    }

    FileValue fv = (FileValue) _fileValue;

    try {
      if( _gout != null ) {
        _gout.close();
        _gout = null;
      }
    }
    catch (Exception e) {
      throw QuercusModuleException.create(e);
    }

    fv.close();

    return true;
  }

  /**
   * alias of gzwrite
   * @param env
   * @param s
   * @param length
   * @return # of uncompressed bytes
   */
  public int gzputs(Env env,
                    @NotNull InputStream is,
                    @Optional("0") int length)
  {
    return gzwrite(env, is, length);
  }

  /**
   *
   * @return the next character or BooleanValue.FALSE
   */
  public Value gzgetc()
  {
    try {
      if (_bufferedReader == null) {
	getBufferedReader();
      }
      int ch = _bufferedReader.read();

      if (ch >= 0)
	return new StringValueImpl(Character.toString((char) ch));
      else
	return BooleanValue.FALSE;
    } catch (IOException e) {
      throw QuercusModuleException.create(e);
    }
  }

  /**
   * Gets a (uncompressed) string of up to 'length' bytes read
   * from the given file pointer. Reading ends when 'length' bytes
   * have been read, on a newline, or on EOF (whichever comes first).
   *
   * @param length
   * @return StringBuilderValue
   */
  public Value gzgets(int length)
  {
    try {
      if (_bufferedReader == null) {
        getBufferedReader();
      }

      StringBuilderValue sbv = new StringBuilderValue();
      int readChar;
      for (int i=0; i < length - 1; i++) {
	readChar = _bufferedReader.read();
	if (readChar >= 0) {
                    sbv.append( (char)readChar );
	  if ((((char) readChar) == '\n') || (((char) readChar) == '\r'))
	    break;
	} else
	  break;
      }
      if( sbv.length() > 0)
        return sbv;
      else
        return BooleanValue.FALSE;
    } catch (Exception e) {
      throw QuercusModuleException.create(e);
    }
  }

  /**
   * helper function for ZlibModule.gzfile
   * need to have created a Zlib before calling this
   *
   * @return array of uncompressed lines
   * @throws IOException
   * @throws DataFormatException
   */
  public Value gzfile()
  {
    Value line;
    int oldLength = 0; 

    ArrayValue array = new ArrayValueImpl();
    StringBuilderValue sbv = new StringBuilderValue();

    try {
      //read in String BuilderValue's initial capacity
      while( (line=gzgets(128)) != BooleanValue.FALSE )
      {
        oldLength = sbv.length();
        line.appendTo( sbv );

        //if read less than the maximum, then know that a newline or EOF has been encountered
        if( sbv.length() < oldLength+128 )
        {
          array.put( sbv.toString() );
          sbv = new StringBuilderValue();
        }
      }

      return array;
    } catch (Exception e) {
      throw QuercusModuleException.create(e);
    }
  }

  /**
   * helper function for readgzfile.
   * Not meant to be called directly from PHP
   *
   * @return uncompressed file
   * @throws IOException
   * @throws DataFormatException
   */
  InputStream readgzfile()
  {
    try {
      try
      {
        return new GZIPInputStream(_path.openRead());
      }
      catch(IOException e)
      {
        // read uncompressed file
        return _path.openRead();
      }
    }
    catch (Exception e) {
      throw QuercusModuleException.create(e);
    }
  }

  /**
   * same as gzgets but does not stop at '\n' or '\r'
   * @param length
   * @return StringValue
   * @throws IOException
   * @throws DataFormatException
   */
  public Value gzread(int length)
   {
     try {
       if (_bufferedReader == null) {
	 getBufferedReader();
       }

       BinaryBuilderValue bbv = new BinaryBuilderValue();
       int readChar;
       for (int i=0; i < length; i++) {
	 readChar = _bufferedReader.read();
	 if (readChar >= 0) {
                     bbv.append( readChar );
	 } else
	   break;
       }
       if (bbv.length() > 0)
	 return new StringValueImpl(bbv.toString());
       else
	 return BooleanValue.FALSE;
     } catch (Exception e) {
       throw QuercusModuleException.create(e);
     }
   }

  /**
   *
   * @return true if eof
   */
  public boolean gzeof()
  {
    try {
      if (_bufferedReader == null) {
	getBufferedReader();
      }

      _bufferedReader.mark(1);
      int result = _bufferedReader.read();
      _bufferedReader.reset();

      return (result == -1);
    } catch (IOException e) {
      throw new QuercusModuleException(e);
    }
  }

  /**
   *
   * @param length
   * @param allowedTags
   * @return next line stripping tags
   * @throws IOException
   * @throws DataFormatException
   */
  public Value gzgetss(int length,
                       @Optional String allowedTags)
  {
    try {
      if (_bufferedReader == null) {
	getBufferedReader();
      }

      StringBuffer sb = new StringBuffer();
      int readChar;
      for (int i=0; i < length; i++) {
	readChar = _bufferedReader.read();
	if (readChar >= 0) {
	  sb.append(Character.toString((char) readChar));
	  if ((((char) readChar) == '\n') || (((char) readChar) == '\r'))
	    break;
	} else
	  break;
      }
      if (sb.length() > 0)
	return new StringValueImpl(StringModule.strip_tags(sb.toString(), allowedTags));
      else
	return BooleanValue.FALSE;
    } catch (Exception e) {
      throw QuercusModuleException.create(e);
    }
  }

  /**
   * resets _bufferedReader to beginning of file stream.
   *
   * @return always true
   * @throws IOException
   */
  public boolean gzrewind()
  {
    try
    {
      getBufferedReader();
    }
    catch (IOException e)
    {
      throw QuercusModuleException.create(e);
    }
    return true;
  }

  /**
   * helper function to open file for reading when necessary
   *
   * @throws IOException
   */
  private void getBufferedReader()
    throws IOException
  {
    BufferedReader bufferedReader = _bufferedReader;

    _bufferedReader = null;
    if (bufferedReader != null)
      bufferedReader.close();

    try
    {
      _bufferedReader = new BufferedReader( new InputStreamReader(new GZIPInputStream(_path.openRead())) );
    }
    catch(IOException e)
    {
      // read uncompressed file
      _bufferedReader = new BufferedReader( new InputStreamReader(_path.openRead()) );
    }
  }

  /**
   * Helper function to retrieve the filemode like how PHP5 does it (returns the file mode nearest to the end)
   *
   * XXX: todo:
   *		1. warning if mode is 'x', set mode to 'w'
   *			- "cannot open a zlib stream for reading and writing at the same time!"
   *
   *
   */
  private String getFileMode( String input )
  {
    String filemode = input.substring( 0, 1 );
    if( "x".equals(filemode) )
      filemode = "w";

    int i = input.lastIndexOf( 'r' );
    int j = input.lastIndexOf( 'w' );
    if( j > i )
      i = j;
    j = input.lastIndexOf( 'a' );
    if( j > i )
      i = j;

    if( i > 0 )
      filemode = input.substring( i, i+1 );

    return filemode;
  }

  /**
   * Helper function to retrieve the compression level like how PHP5 does it.
   * 	1. finds the compression level nearest to the end and returns that
   */
  private int getCompressionLevel( String input )
  {
    int len = input.length();
    char[] c = new char[len];
    input.getChars( 0, len, c, 0 );

    for( int i = len-1; i >= 0; i-- )
    {
      if( c[i] >= '0' && c[i] <= '9' )
        return c[i] - '0';
    }
    return Deflater.DEFAULT_COMPRESSION;
  }

  /**
   * Helper function to retrieve the compression strategy like how PHP5 does it.
   * 	1. finds the compression strategy nearest to the end and returns that
   */
  private int getCompressionStrategy( String input )
  {
    int h = input.lastIndexOf( 'h' );
    int f = input.lastIndexOf( 'f' );

    if( h > f && h > -1 )
      return Deflater.HUFFMAN_ONLY;
    else if( f > -1 )
      return Deflater.FILTERED;
    else
      return Deflater.DEFAULT_STRATEGY;
  }

  public String toString()
  {
    return "Zlib[]";
  }
}

class GZIPFileValueWriter
{
  private Deflater _deflater;
  private FileValue _fileValue;
  private TempBuffer tb = TempBuffer.allocate();
  private byte[] buffer = tb.getBuffer();
  private CRC32 _crc32 = new CRC32();

  private byte[] header = { (byte)0x1f, (byte)0x8b,  //gzip file identifier (ID1, ID2)
                                            8, //Deflate compression method (CM)
                                            0, //optional flags (FLG)
                                            0, 0, 0, 0,  //modification time (MTIME)
                                            0, //extra optional flags (XFL)
                                            0 //operating system (OS)
                                         };

  private byte[] trailer = new byte[8];

  /**
   * Helper class to seamlessly write gzip compressed data directly to the FileValue
   *
   * XXX: todo	1. set operating system (file architecure) header
   *
   */
  private GZIPFileValueWriter( Deflater def, int strategy, FileValue fv ) throws IOException
  {
    def.setStrategy( strategy );
    _deflater = def;
    _fileValue = fv;
    _fileValue.write( header, 0, header.length );
  }

  public GZIPFileValueWriter( int compressionLevel, int strategy, FileValue fv ) throws IOException
  {
    this( new Deflater(compressionLevel, true), strategy, fv );
  }

  public GZIPFileValueWriter( FileValue fv ) throws IOException
  {
    this( Deflater.DEFAULT_COMPRESSION, Deflater.DEFAULT_STRATEGY, fv );
  }

  /**
   *
   * Blocking write to the FileValue unless not enough input data passed in to compress.
   * If not enough data to compress, then write will be postponed until call to close()
   *
   */
  public void write( byte[] input, int offset, int length ) throws IOException
  {
      int clength;

      _deflater.setInput( input, offset, length );
      while(  !_deflater.needsInput() ) {
        clength = _deflater.deflate( buffer, 0, buffer.length );
        if( clength > 0 )
          _fileValue.write( buffer, 0, clength );
      }

      _crc32.update( input, offset, length );
  }

  public void close() throws IOException
  {
    _deflater.finish();

    int clength = _deflater.deflate( buffer, 0, buffer.length );
    if(  clength > 0 )
      _fileValue.write( buffer, 0, clength );

    long value = _crc32.getValue();
    long inputSize = _deflater.getBytesRead();

    trailer[0] = (byte) value;
    trailer[1] = (byte) (value >> 8);
    trailer[2] = (byte) (value >> 16);
    trailer[3] = (byte) (value >> 24);

    trailer[4] = (byte) inputSize;
    trailer[5] = (byte) (inputSize >> 8);
    trailer[6] = (byte) (inputSize >> 16);
    trailer[7] = (byte) (inputSize >> 24);

    _fileValue.write( trailer, 0, trailer.length );
    _fileValue.flush();

    TempBuffer.free( tb );
  }
}