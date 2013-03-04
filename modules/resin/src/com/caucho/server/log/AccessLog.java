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
 * @author Scott Ferguson
 */

package com.caucho.server.log;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import javax.annotation.PostConstruct;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.caucho.config.ConfigException;
import com.caucho.config.Configurable;
import com.caucho.config.types.Bytes;
import com.caucho.config.types.CronType;
import com.caucho.config.types.Period;
import com.caucho.server.http.AbstractHttpRequest;
import com.caucho.server.http.AbstractHttpResponse;
import com.caucho.server.http.CauchoRequest;
import com.caucho.server.http.HttpServletRequestImpl;
import com.caucho.server.http.HttpServletResponseImpl;
import com.caucho.server.util.CauchoSystem;
import com.caucho.util.Alarm;
import com.caucho.util.AlarmListener;
import com.caucho.util.CharBuffer;
import com.caucho.util.CharSegment;
import com.caucho.util.CurrentTime;
import com.caucho.util.L10N;
import com.caucho.util.WeakAlarm;
import com.caucho.vfs.Path;

/**
 * Represents an log of every top-level request to the server.
 */
public class AccessLog extends AbstractAccessLog implements AlarmListener
{
  protected static final L10N L = new L10N(AccessLog.class);
  protected static final Logger log
    = Logger.getLogger(AccessLog.class.getName());

  // Default maximum log size = 1G
  private static final long ROLLOVER_SIZE = 1024L * 1024L * 1024L;
  public static final int BUFFER_SIZE = 64 * 1024;
  
  private String _timeFormat;

  private final AccessLogWriter _logWriter = new AccessLogWriter(this);

  private String _format;
  private Segment []_segments;

  private ArrayList<Pattern> _excludeList = new ArrayList<Pattern>();
  private Pattern []_excludes = new Pattern[0];

  private boolean _isAutoFlush;

  private long _autoFlushTime = 60000;

  private final CharBuffer _cb = new CharBuffer();

  private Alarm _alarm = new WeakAlarm(this);
  private boolean _isActive;

  public AccessLog()
  {
    setRolloverSize(new Bytes(ROLLOVER_SIZE));
  }

  /**
   * Sets the log buffer size.
   *
   * @param bytes buffer
   */
  @Configurable
  public void setBufferSize(Bytes bytes)
  {
    _logWriter.setBufferSize(bytes);
  }
  
  @Override
  public int getBufferSize()
  {
    return _logWriter.getBufferSize();
  }

  /**
   * Sets the access log format.
   */
  public void setFormat(String format)
  {
    _format = format;
  }

  /**
   * Sets the log path
   */
  @Override
  public void setPath(Path path)
  {
    super.setPath(path);

    _logWriter.setPath(path);
  }

  /**
   * Sets the formatted path.
   */
  @Configurable
  @Override
  public void setPathFormat(String pathFormat)
    throws ConfigException
  {
    super.setPathFormat(pathFormat);

    _logWriter.setPathFormat(pathFormat);
  }

  /**
   * Sets the archive name format
   */
  @Configurable
  public void setArchiveFormat(String format)
  {
    _logWriter.setArchiveFormat(format);
  }

  /**
   * Sets the maximum number of rolled logs.
   *
   * @param count maximum count of the log file
   */
  @Configurable
  public void setRolloverCount(int count)
  {
    _logWriter.setRolloverCount(count);
  }

  /**
   * Sets the log rollover cron
   *
   * @param cron the cron string for rollover times
   */
  @Configurable
  public void setRolloverCron(CronType cron)
  {
    _logWriter.setRolloverCron(cron);
  }

  /**
   * Sets the log rollover period, rounded up to the nearest hour.
   *
   * @param period the new rollover period in milliseconds.
   */
  @Configurable
  public void setRolloverPeriod(Period period)
  {
    _logWriter.setRolloverPeriod(period);
  }

  /**
   * Sets the log rollover size, rounded up to the megabyte.
   *
   * @param bytes buffer
   */
  @Configurable
  public void setRolloverSize(Bytes bytes)
  {
    _logWriter.setRolloverSize(bytes);
  }

  /**
   * Sets how often the log rollover will be checked.
   *
   * @param period how often the log rollover will be checked.
   */
  @Configurable
  public void setRolloverCheckTime(Period period)
  {
    _logWriter.setRolloverCheckPeriod(period.getPeriod());
  }

  /**
   * Sets the auto-flush attribute.
   */
  @Configurable
  public void setAutoFlush(boolean isAutoFlush)
  {
    _isAutoFlush =  isAutoFlush;
  }

  boolean isAutoFlush()
  {
    return _isAutoFlush;
  }

  /**
   * Sets the autoFlushTime
   */
  public void setAutoFlushTime(Period period)
  {
    _autoFlushTime = period.getPeriod();
  }

  /**
   * Sets the shared buffer attribute.
   */
  public void setSharedBuffer(boolean isSharedBuffer)
  {
  }

  /**
   * Adds an exclusion pattern.
   */
  @Configurable
  public void addExclude(Pattern pattern)
  {
    _excludeList.add(pattern);
    _excludes = new Pattern[_excludeList.size()];
    _excludeList.toArray(_excludes);
  }

  /**
   * Initialize the log.
   */
  @PostConstruct
  @Override
  public void init()
    throws ServletException, IOException
  {
    _isActive = true;

    if (_alarm != null) {
      if (_autoFlushTime > 0)
        _alarm.queue(_autoFlushTime);
      else
        _alarm.queue(60000);
    }
    

    if (_format == null)
      _format = "%h %l %u %t \"%r\" %>s %b \"%{Referer}i\" \"%{User-Agent}i\"";

    ArrayList<Segment> segments = parseFormat(_format);

    _segments = new Segment[segments.size()];
    segments.toArray(_segments);

    if (_timeFormat == null || _timeFormat.equals("")) {
      _timeFormat = "[%d/%b/%Y:%H:%M:%S %z]";
    }
    
    _logWriter.init();
    // _sharedBufferLock = _logWriter.getBufferLock();

    super.init();
    
    _logWriter.rollover();
  }

  /**
   * Parses the access log string.
   */
  private ArrayList<Segment> parseFormat(String format)
  {
    ArrayList<Segment> segments = new ArrayList<Segment>();
    CharBuffer cb = new CharBuffer();

    int i = 0;
    while (i < _format.length()) {
      char ch = _format.charAt(i++);

      if (ch != '%' || i >= _format.length()) {
        cb.append((char) ch);
        continue;
      }

      String arg = null;
      ch = _format.charAt(i++);
      if (ch == '>')
        ch = _format.charAt(i++);
      else if (ch == '{') {
        if (cb.length() > 0)
          segments.add(new Segment(this, Segment.TEXT, cb.toString()));
        cb.clear();
        while (i < _format.length() && _format.charAt(i++) != '}')
          cb.append(_format.charAt(i - 1));
        arg = cb.toString();
        cb.clear();

        ch = _format.charAt(i++);
      }

      switch (ch) {
      case 'b': case 'c':
      case 'h': case 'i': case 'l': case 'n':
      case 'r': case 's':
      case 'T': case 'D': case 'o':
      case 'u': case 'U':
      case 'v': case 'S':
        if (cb.length() > 0)
          segments.add(new Segment(this, Segment.TEXT, cb.toString()));
        cb.clear();
        segments.add(new Segment(this, ch, arg));
        break;

      case 't':
        if (cb.length() > 0)
          segments.add(new Segment(this, Segment.TEXT, cb.toString()));
        cb.clear();
        if (arg != null)
          _timeFormat = arg;
        segments.add(new Segment(this, ch, arg));
        break;

      default:
        cb.append('%');
        i--;
        break;
      }
    }

    cb.append(CauchoSystem.getNewlineString());
    segments.add(new Segment(this, Segment.TEXT, cb.toString()));

    return segments;
  }

  /**
   * Logs a request using the current format.
   */
  @Override
  public void log(HttpServletRequest req,
                  HttpServletResponse res,
                  ServletContext application)
    throws IOException
  {
    // server/1kk7
    CauchoRequest cRequest = (CauchoRequest) req;
    HttpServletResponseImpl responseImpl = (HttpServletResponseImpl) res;

    AbstractHttpRequest absRequest = cRequest.getAbstractHttpRequest();
    HttpServletRequestImpl request = absRequest.getRequestFacade();
    AbstractHttpResponse response = responseImpl.getAbstractHttpResponse();

    // skip excluded urls
    if (_excludes.length > 0) {
      byte []data = absRequest.getUriBuffer();
      int sublen = absRequest.getUriLength();

      String uri = new String(data, 0, sublen);

      for (Pattern pattern : _excludes) {
        if (pattern.matcher(uri).find()) {
          return;
        }
      }
    }

    LogBuffer logBuffer = response.getLogBuffer();
    
    if (! logBuffer.allocate(_logWriter)) {
      // if currently queued, create a new one
      logBuffer = _logWriter.allocateBuffer();
    }

    try {
      byte []buffer = logBuffer.getBuffer();

      int length = log(request, responseImpl, response, buffer, 0, buffer.length);

      logBuffer.setLength(length);

      _logWriter.writeBuffer(logBuffer);
      logBuffer = null;
    } finally {
      if (logBuffer != null) {
        _logWriter.freeBuffer(logBuffer);
      }
    }
  }

  /**
   * Logs a request using the current format.
   *
   * @param request the servlet request.
   * @param response the servlet response.
   * @param buffer byte buffer containing the response
   * @param offset buffer starting offset
   * @param length length allowed in the buffer
   *
   * @return the new tail of the buffer
   */
  private int log(HttpServletRequestImpl request,
                  HttpServletResponseImpl responseFacade,
                  final AbstractHttpResponse response,
                  byte []buffer, int offset, int length)
    throws IOException
  {
    final AbstractHttpRequest absRequest = request.getAbstractHttpRequest();
    CharBuffer cb = _cb;

    int len = _segments.length;
    for (int i = 0; i < len; i++) {
      Segment segment = _segments[i];
      String value = null;
      CharSegment csValue = null;

      switch (segment._code) {
      case Segment.TEXT:
        int sublen = segment._data.length;
        byte []data = segment._data;
        for (int j = 0; j < sublen; j++) {
          buffer[offset++] = data[j];
        }
        break;

      case Segment.CHAR:
        buffer[offset++] = segment._ch;
        break;

      case 'b':
        if (responseFacade.getStatus() == 304)
          buffer[offset++] = (byte) '-';
        else
          offset = print(buffer, offset, response.getContentLength());
        break;

        // cookie
      case 'c':
        Cookie cookie = request.getCookie(segment._string);
        if (cookie == null)
          cookie = responseFacade.getCookie(segment._string);
        if (cookie == null)
          buffer[offset++] = (byte) '-';
        else
          offset = print(buffer, offset, cookie.getValue());
        break;

        // set cookie
      case Segment.SET_COOKIE:
        ArrayList<Cookie> cookies = responseFacade.getCookies();
        
        if (cookies == null) {
          break;
        }
        
        int cookiesSize = cookies.size();
        
        value = response.getHeader(segment._string);
        
        if (cookies != null && cookiesSize > 0) {
          cb.clear();
          
          if (value != null) {
            cb.append(value);
          }

          for (int j = 0; j < cookiesSize; j++) {
            cookie = cookies.get(j);
            
            if (cb.getLength() > 0) {
              cb.append(",");
            }

            cb.append(cookie.getName());
            cb.append('=');
            cb.append(cookie.getValue());
          }

          offset = print(buffer, offset, cb.getBuffer(), 0, cb.getLength());
        }
        else if (value != null) {
          int p = value.indexOf(';');
          
          if (p > 0) {
            offset = print(buffer, offset, value, 0, p);
          }
          else {
            offset = print(buffer, offset, value);
          }
        }
        else {
          buffer[offset++] = (byte) '-';
        }
        break;

      case 'h':
        if (isHostnameDnsLookup()) {
          String addrName = request.getRemoteAddr();
          InetAddress addr = InetAddress.getByName(addrName);

          offset = print(buffer, offset, addr.getHostName());
        }
        else
          offset = absRequest.printRemoteAddr(buffer, offset);
        break;

        // input header
      case 'i':
        csValue = absRequest.getHeaderBuffer(segment._string);
        if (csValue == null)
          buffer[offset++] = (byte) '-';
        else
          offset = print(buffer, offset, csValue);
        break;

      case 'l':
        buffer[offset++] = (byte) '-';
        break;

        // request attribute
      case 'n':
        Object oValue = request.getAttribute(segment._string);
        if (oValue == null)
          buffer[offset++] = (byte) '-';
        else
          offset = print(buffer, offset, String.valueOf(oValue));
        break;

        // output header
      case 'o':
        value = response.getHeader(segment._string);
        if (value == null)
          buffer[offset++] = (byte) '-';
        else
          offset = print(buffer, offset, value);
        break;

      case 'r':
        offset = print(buffer, offset, request.getMethod());

        buffer[offset++] = (byte) ' ';

        data = absRequest.getUriBuffer();
        sublen = absRequest.getUriLength();

        // server/02e9
        if (buffer.length - offset - 128 < sublen) {
          sublen = buffer.length - offset - 128;
          System.arraycopy(data, 0, buffer, offset, sublen);
          offset += sublen;
          buffer[offset++] = (byte) '.';
          buffer[offset++] = (byte) '.';
          buffer[offset++] = (byte) '.';
        }
        else {
          System.arraycopy(data, 0, buffer, offset, sublen);
          offset += sublen;
        }

        buffer[offset++] = (byte) ' ';

        offset = print(buffer, offset, request.getProtocol());
        break;

      case 's':
        int status = responseFacade.getStatus();
        buffer[offset++] = (byte) ('0' + (status / 100) % 10);
        buffer[offset++] = (byte) ('0' + (status / 10) % 10);
        buffer[offset++] = (byte) ('0' + status % 10);
        break;
        
      case 'S':
        {
          String sessionId = request.getRequestedSessionId();
          if (! request.isRequestedSessionIdValid() || sessionId == null) {
            buffer[offset++] = (byte) '-';
          }
          else {
            offset = print(buffer, offset, sessionId);
          }
          break;
        }

      case 't':
      {
        long now = CurrentTime.getCurrentTime();

        data = response.fillLogDateBuffer(now, _timeFormat);
        sublen = response.getLogDateBufferLength();

        System.arraycopy(data, 0, buffer, offset, sublen);

        offset += sublen;
        break;
      }

      case 'T':
        {
          long startTime = request.getStartTime();
          long endTime = CurrentTime.getCurrentTime();

          offset = print(buffer, offset, (int) ((endTime - startTime + 500) / 1000));
          break;
        }

      case 'D':
        {
          long startTime = request.getStartTime();
          long endTime = CurrentTime.getExactTime();

          offset = print(buffer, offset, (int) ((endTime - startTime) * 1000));
          break;
        }

      case 'u':
        value = request.getRemoteUser(false);
        if (value == null)
          buffer[offset++] = (byte) '-';
        else {
          buffer[offset++] = (byte) '"';
          offset = print(buffer, offset, value);
          buffer[offset++] = (byte) '"';
        }
        break;

      case 'v':
        value = request.getServerName();
        if (value == null)
          buffer[offset++] = (byte) '-';
        else {
          offset = print(buffer, offset, value);
        }
        break;

      case 'U':
        offset = print(buffer, offset, request.getRequestURI());
        break;

      default:
        throw new IOException();
      }
    }

    return offset;
  }

  /**
   * Prints a CharSegment to the log.
   *
   * @param buffer receiving byte buffer.
   * @param offset offset into the receiving buffer.
   * @param cb the new char segment to be logged.
   * @return the new offset into the byte buffer.
   */
  private int print(byte []buffer, int offset, CharSegment cb)
  {
    char []charBuffer = cb.getBuffer();
    int cbOffset = cb.getOffset();
    int length = cb.getLength();

    // truncate for hacker attacks
    if (buffer.length - offset - 256 < length)
      length =  buffer.length - offset - 256;

    for (int i = length - 1; i >= 0; i--)
      buffer[offset + i] = (byte) charBuffer[cbOffset + i];

    return offset + length;
  }

  /**
   * Prints a String to the log.
   *
   * @param buffer receiving byte buffer.
   * @param offset offset into the receiving buffer.
   * @param s the new string to be logged.
   * @return the new offset into the byte buffer.
   */
  private int print(byte []buffer, int offset, String s)
  {
    int length = s.length();

    _cb.ensureCapacity(length);
    char []cBuf = _cb.getBuffer();

    s.getChars(0, length, cBuf, 0);

    for (int i = length - 1; i >= 0; i--)
      buffer[offset + i] = (byte) cBuf[i];

    return offset + length;
  }
  
  private int print(byte []buffer, int offset, String s, int sOff, int sLen)
  {
    _cb.ensureCapacity(sLen);
    char []cBuf = _cb.getBuffer();

    s.getChars(sOff, sLen, cBuf, 0);

    for (int i = sLen - 1; i >= 0; i--) {
      buffer[offset + i] = (byte) cBuf[i];
    }

    return offset + sLen;
  }

  /**
   * Prints a String to the log.
   *
   * @param buffer receiving byte buffer.
   * @param offset offset into the receiving buffer.
   * @param cb the new string to be logged.
   * @return the new offset into the byte buffer.
   */
  private int print(byte []buffer, int offset,
                    char []cb, int cbOff, int length)
  {
    for (int i = length - 1; i >= 0; i--)
      buffer[offset + i] = (byte) cb[cbOff + i];

    return offset + length;
  }

  /**
   * Prints an integer to the log.
   *
   * @param buffer receiving byte buffer.
   * @param offset offset into the receiving buffer.
   * @param v the new integer to be logged.
   * @return the new offset into the byte buffer.
   */
  private int print(byte []buffer, int offset, long v)
  {
    if (v == 0) {
      buffer[offset] = (byte) '0';
      return offset + 1;
    }

    if (v < 0) {
      buffer[offset++] = (byte) '-';
      v = -v;
    }

    int length = 0;
    int exp = 10;

    for (; exp <= v && exp > 0; length++)
      exp = 10 * exp;

    offset += length;
    for (int i = 0; i <= length; i++) {
      buffer[offset - i] = (byte) (v % 10 + '0');
      v = v / 10;
    }

    return offset + 1;
  }

  /**
   * Flushes the log.
   */
  @Override
  public void flush()
  {
    // server/0213, 021q
    _logWriter.flush();
    _logWriter.waitForFlush(5000L);
    _logWriter.rollover();
  }

  /**
   * The alarm listener.
   */
  @Override
  public void handleAlarm(Alarm alarm)
  {
    try {
      flush();
    } finally {
      alarm = _alarm;
      if (alarm != null && _isActive && _autoFlushTime > 0)
        alarm.queue(_autoFlushTime);
    }
  }

  /**
   * Closes the log, flushing the results.
   */
  @Override
  public void destroy()
    throws IOException
  {
    super.destroy();

    _isActive = false;

    Alarm alarm = _alarm;
    _alarm = null;

    if (alarm != null)
      alarm.dequeue();

    flush();

    _logWriter.close();
  }

  /**
   * Represents one portion of the access log.
   */
  static class Segment {
    final static int TEXT = 0;
    final static int CHAR = 1;
    final static int SET_COOKIE = 2;

    int _code;
    byte []_data;
    byte _ch;
    String _string;
    AccessLog _log;

    /**
     * Creates a new log segment.
     *
     * @param log the owning log
     * @param code the segment code, telling what kind of segment it is
     * @param string the parameter for the segment code.
     */
    Segment(AccessLog log, int code, String string)
    {
      _log = log;
      _code = code;

      _string = string;
      if (string != null) {
        if (code == 'o' && string.equalsIgnoreCase("Set-Cookie")) {
          _code = SET_COOKIE;
        }

        _data = _string.getBytes();
        if (code == TEXT && _string.length() == 1) {
          _ch = (byte) _string.charAt(0);
          _code = CHAR;
        }
      }
    }
  }
}
