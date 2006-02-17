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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.log;

import com.caucho.config.ConfigException;
import com.caucho.config.types.RawString;
import com.caucho.el.EL;
import com.caucho.el.ELParser;
import com.caucho.el.Expr;
import com.caucho.util.CharBuffer;
import com.caucho.util.L10N;

import java.util.ResourceBundle;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import javax.servlet.jsp.el.ELException;
import javax.servlet.jsp.el.VariableResolver;

/**
 * A Formatter that accepts an EL format string, and.
 */
public class ELFormatter extends MessageFormatter {
  static final L10N L = new L10N(ELFormatter.class);

  private String _format;
  private Expr _expr;

  public void setFormat(RawString format)
  {
    _format = format.getValue();
  }

  public String getFormat()
  {
    return _format;
  }

  public void init()
    throws ConfigException
  {
    if (_format != null) {
      try {
        _expr = (new ELParser(_format)).parse();
      } catch (Exception ex) {
        throw new ConfigException(ex);
      }
    }
  }

  public String format(LogRecord logRecord)
  {
    if (_expr == null) {
      return super.format(logRecord);
    }

    String ret;
    if (_expr == null) {
      ret = super.format(logRecord);
    }
    else {
      try {
        ELFormatterVariableResolver vr = new ELFormatterVariableResolver();
        vr.setLogRecord(logRecord);

        ret =  _expr.evalString(vr);
      } 
      catch (Exception ex) {
        throw new RuntimeException(ex);
      }
    }

    return ret;
  }

  class ELFormatterVariableResolver implements VariableResolver {
    ELFormatterLogRecord _logRecord;

    public void setLogRecord(LogRecord logRecord)
    {
      _logRecord = new ELFormatterLogRecord(logRecord);
    }

    public Object resolveVariable(String name) 
      throws ELException 
    {
      if (name.equals("log")) {
        return _logRecord;
      }
      else {
        return EL.getEnvironment().resolveVariable(name);
      }
    }
  }

  /**
   * An api similar to java.util.logging.LogRecord that provides more complete
   * information for logging purposes.
   */
  public class ELFormatterLogRecord {

    LogRecord _logRecord;

    ELFormatterLogRecord(LogRecord logRecord)
    {
      _logRecord = logRecord;
    }

    /**
     * The "formatted" log message, after localization, substitution of
     * parameters, and the inclusion of an exception stack trace if applicable.
     * <p>
     * During formatting, if the source logger has a localization
     * ResourceBundle and if that ResourceBundle has an entry for
     * this message string, then the message string is replaced
     * with the localized value.
     * <p>
     * If the message has parameters, java.text.MessageFormat is used to format
     * the message with the parameters.
     * <p>
     * If the log record has an associated exception, the stack trace is
     * appended to the log message.
     *
     * @see java.text.MessageFormat 
     * @see java.lang.Throwable.printStackTrace() 
     */ 
    public String getMessage()
    { 
      /** use the formatMessage() method of the outer class */
      return formatMessage(_logRecord); 
    }

    /** 
     * The source Logger's name.
     *
     * @return source logger name, which may be null
     */ 
    public String getName()
    { return _logRecord.getLoggerName(); }
   
    /** 
     * The source Logger's name.
     *
     * @return source logger name, which may be null
     */ 
    public String getLoggerName()
    { return _logRecord.getLoggerName(); }
   
    /** 
     * The last component of the source Logger's name.  The last component
     * is everything that occurs after the last `.' character, usually 
     * it is the class name. 
     *
     *
     * @return short version of the source logger name, or null
     */ 
    public String getShortName()
    { 
      String name = _logRecord.getLoggerName();

      if (name != null) {
        int index = name.lastIndexOf('.') + 1;
        if (index > 0 && index < name.length()) {
          name = name.substring(index);
        }
      }

      return name;
    }
   
    /**
     * The logging message level, for example Level.INFO.
     *
     * @see java.util.logging.Level
     */
    public Level getLevel()
    { return _logRecord.getLevel(); }

    /** 
     * The time of the logging event, in milliseconds since 1970.
     */ 
    public long getMillis()
    { return _logRecord.getMillis(); }

    /** 
     * An identifier for the thread where the message originated.
     */ 
    public int getThreadID()
    { return _logRecord.getThreadID(); }

    /** 
     * The throwable associated with the log record, if one was associated.
     */ 
    public Throwable getThrown()
    { return _logRecord.getThrown(); }

    /** 
     * The sequence number, normally assigned in the constructor of LogRecord.
     */ 
    public long getSequenceNumber()
    { return _logRecord.getSequenceNumber(); }

    /** 
     * The name of the class that issued the logging request.  
     * This name may be unavailable, or not actually the name of the class that
     * issued the logging message.
     */
    public String getSourceClassName()
    { return _logRecord.getSourceClassName(); }

    /** 
     * The last component of the name (everthing after the last `.') of the
     * class that issued the logging request.
     * This name may be unavailable, or not actually the name of the class that
     * issued the logging message.
     *
     * @return short version of the sourceClassName
     */ 
    public String getShortSourceClassName()
    { 
      String name = _logRecord.getSourceClassName();

      if (name != null) {
        int index = name.lastIndexOf('.') + 1;
        if (index > 0 && index < name.length()) {
          name = name.substring(index);
        }
      }

      return name;
    }
   
    /** 
     * The name of the method that issued the logging request.  This name
     * may be unavailable, or not actually the name of the class that issued
     * the logging message.
     */
    public String getSourceMethodName()
    { return _logRecord.getSourceMethodName(); }

    /**
     * The "raw" log message, before localization or substitution 
     * of parameters. 
     * <p>
     * This returned message will be either the final text, text containing
     * parameter substitution "format elements" (like `{0}') for use by
     * java.text.MessageFormat, or a localization key.
     *
     * @see java.text.MessageFormat 
     */ 
    public String getRawMessage()
    { return _logRecord.getMessage(); }

    /** 
     * The resource bundle for localization.
     */ 
    public ResourceBundle getResourceBundle()
    { return _logRecord.getResourceBundle(); }
   
    /** 
     * The name of resource bundle for localization.
     */ 
    public String getResourceBundleName()
    { return _logRecord.getResourceBundleName(); }
   
    public Object[] getParameters()
    { return _logRecord.getParameters(); }

  }
}

