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

package com.caucho.quercus.env;

import com.caucho.quercus.*;
import com.caucho.quercus.expr.Expr;
import com.caucho.quercus.function.Marshal;
import com.caucho.quercus.function.MarshalFactory;
import com.caucho.quercus.lib.VariableModule;
import com.caucho.quercus.lib.file.FileModule;
import com.caucho.quercus.lib.string.StringUtility;
import com.caucho.quercus.module.ModuleContext;
import com.caucho.quercus.module.ModuleStartupListener;
import com.caucho.quercus.page.QuercusPage;
import com.caucho.quercus.program.AbstractFunction;
import com.caucho.quercus.program.ClassDef;
import com.caucho.quercus.program.JavaClassDef;
import com.caucho.quercus.program.QuercusProgram;
import com.caucho.quercus.resources.StreamContextResource;
import com.caucho.util.Alarm;
import com.caucho.util.IntMap;
import com.caucho.util.L10N;
import com.caucho.util.LruCache;
import com.caucho.vfs.ByteToChar;
import com.caucho.vfs.Path;
import com.caucho.vfs.ReadStream;
import com.caucho.vfs.WriteStream;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.sql.DataSource;
import java.io.Closeable;
import java.io.IOException;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.lang.reflect.Array;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Represents the Quercus environment.
 */
public class Env {
  private static final L10N L = new L10N(Env.class);
  private static final Logger log
    = Logger.getLogger(Env.class.getName());

  public static final int B_ERROR = 0;
  public static final int B_WARNING = 1;
  public static final int B_PARSE = 2;
  public static final int B_NOTICE = 3;
  public static final int B_CORE_ERROR = 4;
  public static final int B_CORE_WARNING = 5;
  public static final int B_COMPILE_ERROR = 6;
  public static final int B_COMPILE_WARNING = 7;
  public static final int B_USER_ERROR = 8;
  public static final int B_USER_WARNING = 9;
  public static final int B_USER_NOTICE = 10;
  public static final int B_STRICT = 11;

  public static final int E_ERROR = 1 << B_ERROR;
  public static final int E_WARNING = 1 << B_WARNING;
  public static final int E_PARSE = 1 << B_PARSE;
  public static final int E_NOTICE = 1 << B_NOTICE;
  public static final int E_CORE_ERROR = 1 << B_CORE_ERROR;
  public static final int E_CORE_WARNING = 1 << B_CORE_WARNING;
  public static final int E_COMPILE_ERROR = 1 << B_COMPILE_ERROR;
  public static final int E_COMPILE_WARNING = 1 << B_COMPILE_WARNING;
  public static final int E_USER_ERROR = 1 << B_USER_ERROR;
  public static final int E_USER_WARNING = 1 << B_USER_WARNING;
  public static final int E_USER_NOTICE = 1 << B_USER_NOTICE;
  public static final int E_ALL = 2048 - 1;
  public static final int E_STRICT = 1 << B_STRICT;

  public static final int E_DEFAULT = E_ALL & ~E_NOTICE;

  private static final int _SERVER = 1;
  private static final int _GET = 2;
  private static final int _POST = 3;
  private static final int _COOKIE = 4;
  private static final int _GLOBAL = 5;
  private static final int _REQUEST = 6;
  private static final int _SESSION = 7;
  private static final int HTTP_GET_VARS = 8;
  private static final int HTTP_POST_VARS = 9;
  private static final int HTTP_COOKIE_VARS = 10;
  private static final int PHP_SELF = 11;
  private static final int _FILES = 12;
  private static final int HTTP_POST_FILES = 13;
  private static final int _ENV = 14;
  private static final int HTTP_SERVER_VARS = 15;

  private static final IntMap SPECIAL_VARS = new IntMap();

  private static final StringValue PHP_SELF_STRING
    = new StringValueImpl("PHP_SELF");

  private static final StringValue UTF8_STRING
    = new StringValueImpl("utf-8");

  private static final
    LruCache<ClassKey,SoftReference<QuercusClass>> _classCache
    = new LruCache<ClassKey,SoftReference<QuercusClass>>(4096);

  private static final
    LruCache<IncludeKey,SoftReference<IncludeCache>> _includeCache
    = new LruCache<IncludeKey,SoftReference<IncludeCache>>(4096);

  private static ThreadLocal<Env> _env = new ThreadLocal<Env>();

  protected Quercus _quercus;
  private QuercusPage _page;

  private Value _this = NullThisValue.NULL;

  private ArrayList<SoftReference<Closeable>> _closeList
    = new ArrayList<SoftReference<Closeable>>();
  
  private ArrayList<Shutdown> _shutdownList
    = new ArrayList<Shutdown>();

  private HashMap<String, Var> _globalMap
    = new HashMap<String, Var>(1024);
  
  private HashMap<String, Var> _staticMap
    = new HashMap<String, Var>();
  
  private HashMap<String, Var> _map = _globalMap;

  private DefinitionState _defState;

  private HashMap<String, Value> _constMap
    = new HashMap<String, Value>(1024);

  private HashMap<String, Value> _lowerConstMap
    = new HashMap<String, Value>(1024);

  /*
  private HashMap<String, AbstractFunction> _funMap
    = new HashMap<String, AbstractFunction>(8192, 0.5F);

  private HashMap<String, AbstractFunction> _lowerFunMap
    = new HashMap<String, AbstractFunction>(8192, 0.5F);
  */

  /*
  private HashMap<String, ClassDef> _classDefMap
    = new HashMap<String, ClassDef>();

  private HashMap<String, ClassDef> _lowerClassDefMap
    = new HashMap<String, ClassDef>();
  */

  private HashMap<String, QuercusClass> _classMap
    = new HashMap<String, QuercusClass>();

  private HashMap<String, QuercusClass> _lowerClassMap
    = new HashMap<String, QuercusClass>();

  private HashSet<String> _initializedClassSet
    = new HashSet<String>();

  private HashMap<String, StringValue> _iniMap;

  // specialMap is used for implicit resources like the mysql link
  private HashMap<String, Object> _specialMap
    = new HashMap<String, Object>();

  private String _prevIncludePath = ".";
  private String _includePath;
  private ArrayList<String> _includePathList;
  private HashMap<Path,ArrayList<Path>> _includePathMap;

  private HashSet<Path> _includeSet = new HashSet<Path>();
  
  private HashMap<StringValue,Path> _lookupCache
    = new HashMap<StringValue,Path>();

  private HashMap<ConnectionEntry,ConnectionEntry> _connMap
    = new HashMap<ConnectionEntry,ConnectionEntry>();

  private AbstractFunction _autoload;

  private long _startTime;
  private long _timeLimit = 600000L;

  private Expr [] _callStack = new Expr[256];
  private Value [] _callThisStack = new Value[256];
  private int _callStackTop;

  private Value [] _functionArgs;

  private Path _selfPath;
  private Path _pwd;
  private Path _uploadPath;
  private ArrayList<Path> _removePaths;

  private final boolean _isStrict;

  private HttpServletRequest _request;
  private HttpServletResponse _response;

  private ArrayValue _post;
  private ArrayValue _files;
  private SessionArrayValue _session;
  private HttpSession _javaSession;

  private ScriptContext _scriptContext;

  private WriteStream _originalOut;
  private OutputBuffer _outputBuffer;

  private WriteStream _out;

  private LocaleInfo _locale;

  private Callback [] _prevErrorHandlers = new Callback[B_STRICT + 1];
  private Callback [] _errorHandlers = new Callback[B_STRICT + 1];

  private Callback _prevExceptionHandler;
  private Callback _exceptionHandler;

  private SessionCallback _sessionCallback;

  private StreamContextResource _defaultStreamContext;

  // XXX: need to look this up from the module itself
  private int _errorMask = E_DEFAULT;

  private int _objectId = 0;

  private Logger _logger;

  public Env(Quercus quercus,
             QuercusPage page,
             WriteStream out,
             HttpServletRequest request,
             HttpServletResponse response)
  {
    _quercus = quercus;

    _isStrict = quercus.isStrict();

    _page = page;

    // XXX: grab initial from page
    _defState = new DefinitionState(quercus);
    
    _originalOut = out;
    _out = out;

    _request = request;
    _response = response;

    if (page != null) {
      _page.init(this);

      importPage(_page);
    }

    setPwd(_quercus.getPwd());

    if (_page != null) {
      _selfPath = _page.getSelfPath(null);

      // php/0b32
      _includeSet.add(_selfPath);
    }

    if (_request != null && _request.getMethod().equals("POST")) {
      _post = new ArrayValueImpl();
      _files = new ArrayValueImpl();
      Post.fillPost(this,
                    _post,
                    _files,
                    _request,
                    getIniBoolean("magic_quotes_gpc"));
    }

    /*
    Cluster cluster = Cluster.getLocal();

    if (cluster != null) {
      ClusterServer selfServer = cluster.getSelfServer();

      if (selfServer != null)
        setIni("caucho.server_id", selfServer.getId());
    }
    */

    _startTime = Alarm.getCurrentTime();
  }

  public Env(Quercus quercus)
  {
    this(quercus, null, null, null, null);
  }

  public static Env getInstance()
  {
    return _env.get();
  }

  //
  // debugging
  //

  /*
   * Returns the PHP call stack.
   */
  public ArrayValue getCallStack()
  {
    ArrayValue array = new ArrayValueImpl();
    
    for (int i = _callStackTop; i >= 2; i--) {
      String fun = _callStack[i].toString();

      array.put(new StringValueImpl(fun));
    }
    
    return array;
  }

  //
  // i18n
  //

  /**
   * Returns the encoding used for scripts.
   */
  public String getScriptEncoding()
  {
    StringValue encoding = getIni("unicode.script_encoding");

    if (encoding == null)
      encoding = getIni("unicode.fallback");

    if (encoding != null)
      return encoding.toString();
    else
      return getQuercus().getScriptEncoding();
  }

  /**
   * Returns the encoding used for runtime conversions, e.g. files
   */
  public StringValue getRuntimeEncoding()
  {
    StringValue encoding = getIni("unicode.runtime_encoding");

    if (encoding == null)
      encoding = getIni("unicode.fallback_encoding");

    if (encoding != null)
      return encoding;
    else
      return UTF8_STRING;
  }

  /**
   * Sets the encoding used for runtime conversions.
   */
  public Value setRuntimeEncoding(String encoding)
  {
    return setIni("unicode.runtime_encoding", encoding);
  }

  /**
   * Returns the encoding used for input, i.e. post
   */
  public StringValue getHttpInputEncoding()
  {
    StringValue encoding = getIni("unicode.http_input_encoding");

    if (encoding == null)
      encoding = getIni("unicode.fallback_encoding");

    if (encoding != null)
      return encoding;
    else
      return UTF8_STRING;
  }

  /**
   * Returns the encoding used for output
   */
  public StringValue getOutputEncoding()
  {
    StringValue encoding = getIni("unicode.output_encoding");

    if (encoding == null)
      encoding = getIni("unicode.fallback_encoding");

    if (encoding != null)
      return encoding;
    else
      return UTF8_STRING;
  }

  public void setScriptContext(ScriptContext context)
  {
    _scriptContext = context;
  }

  /**
   * Returns true for strict mode.
   */
  public final boolean isStrict()
  {
    return _isStrict;
  }
  
  public void start()
  {
    _env.set(this);

    // quercus/1b06
    String encoding = getOutputEncoding().toString();
    String type = getIniString("default_mimetype");
    
    if ("".equals(type) || _response == null) {
    }
    else if (! "".equals(encoding))
      _response.setContentType(type + "; charset=" + encoding);
    else
      _response.setContentType(type);

    if (_out != null) {
      try {
	_out.setEncoding(encoding);
      } catch (Exception e) {
	log.log(Level.WARNING, e.toString(), e);
      }
    }
	
    HashSet<ModuleStartupListener> listeners = 
      _quercus.getModuleStartupListeners(); 

    for (ModuleStartupListener listener : listeners)
      listener.startup(this);
  }

  /**
   * add resource to _closeList
   */
  public void addClose(Closeable closeable)
  {
    _closeList.add(new SoftReference<Closeable>(closeable));
  }

  /**
   * remove resource from _resourceList
   *
   * @param resource
   */
  public void removeClose(Closeable closeable)
  {
    for (int i = _closeList.size() - 1; i >= 0; i--) {
      SoftReference<Closeable> ref = _closeList.get(i);

      Closeable res = ref.get();

      if (closeable.equals(res)) {
        _closeList.remove(i);
        break;
      }
    }
  }

  /**
   * Returns the owning PHP engine.
   */
  public Quercus getQuercus()
  {
    return _quercus;
  }

  /**
   * Returns the owning PHP engine.
   */
  public ModuleContext getModuleContext()
  {
    return _quercus.getModuleContext();
  }

  /**
   * Returns the configured database.
   */
  public DataSource getDatabase()
  {
    return _quercus.getDatabase();
  }

  /**
   * Returns the configured database.
   */
  public Connection getConnection(String driver, String url,
                                  String userName, String password)
    throws Exception
  {
    DataSource database = _quercus.getDatabase();

    if (database != null) {
      ConnectionEntry entry = new ConnectionEntry();
      entry.init(database, null, null);

      ConnectionEntry oldEntry = _connMap.get(entry);

      if (oldEntry != null)
	return oldEntry.getConnection();

      entry.setConnection(database.getConnection());
      _connMap.put(entry, entry);
      
      return entry.getConnection();
    }

    database = _quercus.findDatabase(driver, url);
    
    ConnectionEntry entry = new ConnectionEntry();
    entry.init(database, userName, password);

    ConnectionEntry oldEntry = _connMap.get(entry);

    if (oldEntry != null)
      return oldEntry.getConnection();

    if (userName == null || userName.equals(""))
      entry.setConnection(database.getConnection());
    else
      entry.setConnection(database.getConnection(userName, password));
      
    _connMap.put(entry, entry);
      
    return entry.getConnection();
  }

  /**
   * Returns the configured database.
   */
  public DataSource getDataSource(String driver, String url)
    throws Exception
  {
    DataSource database = _quercus.getDatabase();

    if (database != null)
      return database;
    else
      return _quercus.findDatabase(driver, url);
  }

  /**
   * Sets the time limit.
   */
  public void setTimeLimit(long ms)
  {
    if (ms <= 0)
      ms = Long.MAX_VALUE / 2;

    _timeLimit = ms;
  }

  /**
   * Checks for the program timeout.
   */
  public final void checkTimeout()
  {
    long now = Alarm.getCurrentTime();

    if (_startTime + _timeLimit < now)
      throw new QuercusRuntimeException(L.l("script timed out"));
  }

  /**
   * Returns the writer.
   */
  public WriteStream getOut()
  {
    return _out;
  }

  /**
   * Returns the writer.
   */
  public WriteStream getOriginalOut()
  {
    return _originalOut;
  }

  /**
   * Flushes the output buffer.
   */
  public final void flush()
  {
    try {
      getOut().flush();
    } catch (IOException e) {
      throw new QuercusModuleException(e);
    }
  }

  /**
   * Prints a string
   */
  public final void print(String v)
  {
    try {
      getOut().print(v);
    } catch (IOException e) {
      throw new QuercusModuleException(e);
    }
  }

  /**
   * Prints a character buffer.
   */
  public final void print(char []buffer, int offset, int length)
  {
    try {
      getOut().print(buffer, offset, length);
    } catch (IOException e) {
      throw new QuercusModuleException(e);
    }
  }

  /**
   * Prints a char
   */
  public final void print(char v)
  {
    try {
      getOut().print(v);
    } catch (IOException e) {
      throw new QuercusModuleException(e);
    }
  }

  /**
   * Prints a long
   */
  public final void print(long v)
  {
    try {
      getOut().print(v);
    } catch (IOException e) {
      throw new QuercusModuleException(e);
    }
  }

  /**
   * Prints a double
   */
  public final void print(double v)
  {
    try {
      long longV = (long) v;
      
      if (v == longV)
	getOut().print(longV);
      else
	getOut().print(v);
    } catch (IOException e) {
      throw new QuercusModuleException(e);
    }
  }

  /**
   * Prints an object
   */
  public final void print(Object v)
  {
    try {
      getOut().print(v);
    } catch (IOException e) {
      throw new QuercusModuleException(e);
    }
  }

  /**
   * Prints a value
   */
  public final void print(Value v)
  {
    v.print(this);
  }

  /**
   * Prints a string
   */
  public final void println()
  {
    try {
      getOut().println();
    } catch (IOException e) {
      throw new QuercusModuleException(e);
    }
  }

  /**
   * Prints a string
   */
  public final void println(String v)
  {
    try {
      getOut().println(v);
    } catch (IOException e) {
      throw new QuercusModuleException(e);
    }
  }

  /**
   * Prints a string
   */
  public final void println(Value v)
  {
    try {
      v.print(this);
      getOut().println();
    } catch (IOException e) {
      throw new QuercusModuleException(e);
    }
  }

  /**
   * Prints and object.
   */
  public final void println(Object v)
  {
    try {
      getOut().println(v);
    } catch (IOException e) {
      throw new QuercusModuleException(e);
    }
  }

  /**
   * Prints a byte buffer.
   */
  public final void write(byte []buffer, int offset, int length)
  {
    try {
      getOut().write(buffer, offset, length);
    } catch (IOException e) {
      throw new QuercusModuleException(e);
    }
  }

  /**
   * Returns the current output buffer.
   */
  public OutputBuffer getOutputBuffer()
  {
    return _outputBuffer;
  }

  /**
   * Returns the writer.
   */
  public void pushOutputBuffer(Callback callback, int chunkSize, boolean erase)
  {
    if (_outputBuffer == null) {
      _outputBuffer = 
        new OutputBuffer(_outputBuffer, this, callback, chunkSize, erase);
    } 
    else
      _outputBuffer = 
        new OutputBuffer(_outputBuffer, this, callback, chunkSize, erase);

    _out = _outputBuffer.getOut();
  }

  /**
   * Pops the output buffer
   */
  public boolean popOutputBuffer()
  {
    OutputBuffer outputBuffer = _outputBuffer;

    if (outputBuffer == null)
      return false;

    outputBuffer.close();

    _outputBuffer = outputBuffer.getNext();

    if (_outputBuffer != null)
      _out = _outputBuffer.getOut();
    else {
      _out = _originalOut;
    }

    return true;
  }

  /**
   * Returns the current directory.
   */
  public Path getPwd()
  {
    return _pwd;
  }

  /**
   * Returns the current directory.
   */
  public Path getWorkDir()
  {
    return _quercus.getWorkDir();
  }

  /**
   * Sets the current directory.
   */
  public void setPwd(Path path)
  {
    _pwd = path;
    _lookupCache.clear();
  }

  /**
   * Returns the initial directory.
   */
  public Path getSelfPath()
  {
    return _selfPath;
  }

  /**
   * Returns the initial directory.
   */
  public Path getSelfDirectory()
  {
    return _selfPath.getParent();
  }

  /**
   * Sets the initial directory.
   */
  public void setSelfPath(Path path)
  {
    _selfPath = path;
  }

  /**
   * Returns the upload directory.
   */
  public Path getUploadDirectory()
  {
    if (_uploadPath == null) {
      String realPath = getRequest().getRealPath("WEB-INF/upload");

      _uploadPath = getPwd().lookup(realPath);

      try {
        if (! _uploadPath.isDirectory())
          _uploadPath.mkdirs();
      }
      catch (IOException e) {
        log.log(Level.FINE, e.toString(), e);
      }

      _uploadPath = _uploadPath.createRoot();
    }

    return _uploadPath;
  }

  /**
   * Adds an auto-remove path.
   */
  public void addRemovePath(Path path)
  {
    if (_removePaths == null)
      _removePaths = new ArrayList<Path>();

    _removePaths.add(path);
  }

  /**
   * Returns the request.
   */
  public HttpServletRequest getRequest()
  {
    return _request;
  }

  /**
   * Returns the most recently modified time of all of the {@link Path}'s that
   * have been used for this Env, or 0 if that cannot be determined.
   */
  /*
  public long getLastModified()
  {
    long lastModified = 0;

    if (_page != null) {
      Path pagePath = _page.getSelfPath(this);

      if (pagePath != null)
        lastModified = pagePath.getLastModified();
    }

    for (Path includePath : _includeSet) {
      long includeLastModified = includePath.getLastModified();

      if (lastModified < includeLastModified)
        lastModified = includeLastModified;
    }

    return lastModified;
  }
  */

  /**
   * Returns the response.
   */
  public HttpServletResponse getResponse()
  {
    return _response;
  }

  /**
   * Sets the session callback.
   */
  public void setSessionCallback(SessionCallback callback)
  {
    _sessionCallback = callback;
  }

  /**
   * Gets the session callback.
   */
  public SessionCallback getSessionCallback()
  {
    return _sessionCallback;
  }

  /**
   * Returns the session.
   */
  public SessionArrayValue getSession()
  {
    return _session;
  }

  /**
   * Returns the Java Http session.
   */
  public HttpSession getJavaSession()
  {
    return _javaSession;
  }

  /**
   * Sets the session.
   */
  public void setSession(SessionArrayValue session)
  {
    _session = session;

    if (session != null) {
      Value var = getGlobalVar("_SESSION");
      
      if (! (var instanceof SessionVar)) {
	var = new SessionVar();
	setGlobalValue("_SESSION", var);
      }

      var.set(session);

      setGlobalValue("HTTP_SESSION_VARS", session);

      session.addUse();
    }
    else {
      // php/1k0v
      Value v = getGlobalVar("_SESSION");

      if (v != null)
        v.set(UnsetValue.UNSET);

      v = getGlobalVar("HTTP_SESSION_VARS");

      if (v != null)
        v.set(UnsetValue.UNSET);
    }
  }

  /**
   * Returns a new session id.
   */
  public String generateSessionId()
  {
    String sessionId = 
      _quercus.getQuercusSessionManager().createSessionId(this);

    if (_javaSession != null)
      sessionId = _javaSession.getId().substring(0, 3) + sessionId.substring(3);

    return sessionId;
  }

  /**
   * Create the session.
   */
  public SessionArrayValue createSession(String sessionId, boolean create)
  {
    long now = Alarm.getCurrentTime();

    SessionCallback callback = getSessionCallback();

    _javaSession = _request.getSession(true);

    if (create && _javaSession.getId().length() >= 3 && sessionId.length() >= 3)
      sessionId = _javaSession.getId().substring(0, 3) + sessionId.substring(3);

    SessionArrayValue session = _quercus.loadSession(this, sessionId);

    if (callback != null) {
      StringValue value = callback.read(this, sessionId);

      if (value != null && value.length() != 0) {
        Value unserialize = VariableModule.unserialize(this, value);

        if (unserialize instanceof ArrayValue) {
          ArrayValue arrayValue = (ArrayValue) unserialize;

          session.reset(now);
          session.putAll(arrayValue);
        }
      }
    }

    setSession(session);

    return session;
  }

  /**
   * Destroy the session.
   */
  public void destroySession(String sessionId)
  {
    SessionCallback callback = getSessionCallback();

    if (callback != null) {
      callback.destroy(this, sessionId);
    }
    else {
      _quercus.destroySession(sessionId);
    }

    setSession(null);
  }

  /**
   * Returns the logger used for syslog.
   */
  public Logger getLogger()
  {
    if (_logger == null)
      _logger = Logger.getLogger("quercus.quercus");

    return _logger;
  }

  /**
   * Returns the configuration value of an init var.
   */
  public Value getConfigVar(String var)
  {
    return _quercus.getIni(var);
  }

  /**
   * Sets an ini value.
   */
  public Value setIni(String var, String value)
  {
    StringValue oldValue = getIni(var);

    if (_iniMap == null)
      _iniMap = new HashMap<String, StringValue>();

    _iniMap.put(var, new StringValueImpl(value));

    return oldValue;
  }

  /**
   * Sets an ini value.
   */
  public Value setIni(String var, StringValue value)
  {
    StringValue oldValue = getIni(var);

    if (_iniMap == null)
      _iniMap = new HashMap<String, StringValue>();

    _iniMap.put(var, value);

    return oldValue;
  }

  /**
   * Sets a boolean ini value.
   */
  public Value setIniBoolean(String var, Value value)
  {
    // XXX: needs testing and correlation with Quercus.setIni

    if (value instanceof StringValue) {
      if ("off".equalsIgnoreCase(value.toString()))
        return setIni(var, "");

      if ("on".equalsIgnoreCase(value.toString()))
        return setIni(var, "1");
    }

    return setIni(var, value.toBoolean() ? "1" : "");
  }

  /**
   * Returns an ini value.
   */
  public StringValue getIni(String var)
  {
    StringValue value = null;

    if (_iniMap != null)
      value = _iniMap.get(var);

    if (value != null)
      return value;

    value = _quercus.getIni(var);

    return value;
  }

  /**
   * Returns an ini value.
   */
  public boolean getIniBoolean(String var)
  {
    Value value = getIni(var);

    if (value != null)
      return value.toBoolean();
    else
      return false;
  }

  /**
   * Returns an ini value as a long.
   */
  public long getIniLong(String var)
  {
    Value value = getIni(var);

    if (value != null)
      return value.toLong();
    else
      return 0;
  }

  /**
   * Returns an ini value as a string.
   */
  public String getIniString(String var)
  {
    Value value = getIni(var);

    if (value != null)
      return value.toString();
    else
      return null;
  }

  /**
   * Returns the ByteToChar converter.
   */
  public ByteToChar getByteToChar()
  {
    return ByteToChar.create();
  }

  /**
   * Returns the 'this' value.
   */
  public Value getThis()
  {
    return _this;
  }

  /**
   * Sets the 'this' value, returning the old value.
   */
  public Value setThis(Value value)
  {
    Value oldThis = _this;

    _this = value.toValue();

    return oldThis;
  }

  /**
   * Gets a value.
   */
  public Value getValue(String name)
  {
    Var var = getRef(name);

    if (var != null)
      return var.toValue();
    else
      return NullValue.NULL;
  }

  /**
   * Gets a special value, a special value is used to store and retrieve module
   * specific values in the env using a unique name.
   */
  public <T> T getSpecialValue(String name)
  {
    return (T) _specialMap.get(name);
  }

  /**
   * Gets a global
   */
  public Value getGlobalValue(String name)
  {
    Var var = getGlobalRef(name);

    if (var != null)
      return var.toValue();
    else
      return NullValue.NULL;
  }

  /**
   * Gets a variable
   *
   * @param name the variable name
   * @param var the current value of the variable
   */
  public final Var getVar(String name, Value value)
  {
    if (value != null)
      return (Var) value;
    
    Var var = _map.get(name);

    if (var != null)
      return var;

    var = getRef(name);

    if (var == null) {
      var = new Var();

      if (_map == _globalMap)
        var.setGlobal();

      _map.put(name, var);
    }

    return var;
  }

  /**
   * Gets a variable
   *
   * @param name the variable name
   * @param value the current value of the variable
   */
  public final Var getGlobalVar(String name, Value value)
  {
    if (value != null)
      return (Var) value;

    Var var = _globalMap.get(name);

    if (var != null)
      return var;

    var = getSpecialRef(name);

    if (var == null) {
      var = new Var();
      var.setGlobal();
    }
    
    _globalMap.put(name, var);

    return var;
  }

  /**
   * Gets a static variable name.
   */
  public final String createStaticName()
  {
    return _quercus.createStaticName();
  }

  /**
   * Gets a static variable
   *
   * @param name the variable name
   */
  public final Var getStaticVar(String name)
  {
    Var var = _staticMap.get(name);

    if (var == null) {
      var = new Var();
      var.setGlobal();
      _staticMap.put(name, var);
    }

    return var;
  }

  /**
   * Unsets variable
   *
   * @param name the variable name
   */
  public final Var unsetVar(String name)
  {
    _map.remove(name);

    return null;
  }

  /**
   * Gets a variable
   *
   * @param name the variable name
   * @param value the current value of the variable
   */
  public final Var setVar(String name, Value value)
  {
    Var var;

    if (value instanceof Var) {
      var = (Var) value;

      if (_map == _globalMap)
        var.setGlobal();
    }
    else
      var = new Var(value.toValue());

    _map.put(name, var);

    return var;
  }

  /**
   * Unsets variable
   *
   * @param name the variable name
   */
  public final Var unsetLocalVar(String name)
  {
    _map.remove(name);

    return null;
  }

  /**
   * Unsets variable
   *
   * @param name the variable name
   */
  public final Var unsetGlobalVar(String name)
  {
    _globalMap.remove(name);

    return null;
  }

  /**
   * Gets a local
   *
   * @param var the current value of the variable
   */
  public static final Value getLocalVar(Value var)
  {
    if (var == null)
      var = new Var();

    return var;
  }

  /**
   * Gets a local value
   *
   * @param var the current value of the variable
   */
  public static final Value getLocalValue(Value var)
  {
    if (var != null)
      return var;
    else
      return NullValue.NULL;
  }

  /**
   * Gets a local
   *
   * @param var the current value of the variable
   */
  public static final Value setLocalVar(Value var, Value value)
  {
    value = value.toValue();

    if (var instanceof Var)
      var.set(value);

    return value;
  }

  /**
   * Gets a value.
   */
  public Var getRef(String name)
  {
    Var var = _map.get(name);

    if (var == null) {
      var = getSpecialRef(name);

      if (var != null) {
        var.setGlobal();
        _globalMap.put(name, var);

        var = _map.get(name);
      }
    }

    return var;
  }

  /**
   * Gets a global value.
   */
  public Var getGlobalRef(String name)
  {
    Var var = _globalMap.get(name);

    if (var == null) {
      var = getSpecialRef(name);
      if (var == null)
	var = new Var();
      
      _globalMap.put(name, var);
    }

    return var;
  }

  /**
   * Gets a value.
   */
  public Var getSpecialRef(String name)
  {
    Var var = null;

    switch (SPECIAL_VARS.get(name)) {
    case _ENV: {
      var = new Var();

      _globalMap.put(name, var);

      var.set(new ArrayValueImpl());

      return var;
    }

    case HTTP_POST_VARS:
      if (! getIniBoolean("register_long_arrays"))
	return null;
    case _POST: {
      var = new Var();

      _globalMap.put(name, var);

      ArrayValue post = new ArrayValueImpl();

      var.set(post);

      if (_request == null)
        return null;

      if (! "POST".equals(_request.getMethod()))
        return var;

      if (_post != null) {
        for (Map.Entry<Value, Value> entry : _post.entrySet()) {
          post.put(entry.getKey(), entry.getValue());
        }
      }

      //XXX: php/081e, need to put uploaded files into $_FILES
      ArrayList<String> keys = new ArrayList<String>();
      if (_request.getParameterMap() != null)
        keys.addAll(_request.getParameterMap().keySet());

      Collections.sort(keys);

      for (String key : keys) {
        String []value = _request.getParameterValues(key);

        Post.addFormValue(post, key, value, getIniBoolean("magic_quotes_gpc"));
      }

    }
    break;


    case HTTP_POST_FILES:
      if (! getIniBoolean("register_long_arrays"))
	return null;
    case _FILES: {
      var = new Var();

      _globalMap.put(name, var);

      ArrayValue files = new ArrayValueImpl();

      if (_files != null) {
        for (Map.Entry<Value, Value> entry : _files.entrySet()) {
          files.put(entry.getKey(), entry.getValue());
        }
      }

      var.set(files);
    }
    break;

    case HTTP_GET_VARS:
      if (! getIniBoolean("register_long_arrays"))
        return null;
      
    case _GET: {
      var = new Var();

      ArrayValue array = new ArrayValueImpl();

      var.set(array);
      _globalMap.put(name, var);

      String queryString = _request.getQueryString();
      if (queryString == null)
        return var;

      StringUtility.parseStr(this,
                             queryString,
                             array,
                             true,
                             getHttpInputEncoding().toString());

      return var;
    }
      
    case _REQUEST: {
      var = new Var();

      ArrayValue array = new ArrayValueImpl();

      var.set(array);

      _globalMap.put(name, var);

      try {
	_request.setCharacterEncoding(getHttpInputEncoding().toString());
      } catch (Exception e) {
	log.log(Level.FINE, e.toString(), e);
      }
	  
      ArrayList<String> keys = new ArrayList<String>();
      keys.addAll(_request.getParameterMap().keySet());

      Collections.sort(keys);

      for (String key : keys) {
        String []value = _request.getParameterValues(key);

        Post.addFormValue(array,
			  key,
			  value,
			  getIniBoolean("magic_quotes_gpc"));
      }

      if (name.equals("_REQUEST") && _post != null) {
        for (Map.Entry<Value, Value> entry : _post.entrySet()) {
          array.put(entry.getKey(), entry.getValue().copy());
        }
      }

      Cookie []cookies = _request.getCookies();
      for (int i = 0; cookies != null && i < cookies.length; i++) {
	Post.addFormValue(array,
			  cookies[i].getName(),
			  new String[] { cookies[i].getValue() },
			  getIniBoolean("magic_quotes_gpc"));
      }

      return var;
    }

    case HTTP_SERVER_VARS:
      if (! getIniBoolean("register_long_arrays"))
	return null;
    case _SERVER: {
      var = new Var();

      _globalMap.put(name, var);

      var.set(new ServerArrayValue(this));

      return var;
    }

    case _GLOBAL: {
      var = new Var();

      _globalMap.put(name, var);

      var.set(new GlobalArrayValue(this));

      return var;
    }

    case HTTP_COOKIE_VARS:
      if (! getIniBoolean("register_long_arrays"))
	return null;
    case _COOKIE: {
      var = new Var();
      _globalMap.put(name, var);

      ArrayValue array = new ArrayValueImpl();

      Cookie []cookies = _request.getCookies();
      if (cookies != null) {
        for (int i = 0; i < cookies.length; i++) {
          Cookie cookie = cookies[i];

          String value = decodeValue(cookie.getValue());

          StringValue valueAsValue;

	  // PHP 6 removes magic_quotes
	  /*
          if (getIniBoolean("magic_quotes_gpc"))
            valueAsValue = StringModule.addslashes(value);
          else
	  */
	  valueAsValue = new StringValueImpl(value);

          array.append(new StringValueImpl(cookie.getName()), valueAsValue);
        }
      }

      var.set(array);

      _globalMap.put(name, var);

      return var;
    }

    case PHP_SELF: {
      var = new Var();
      _globalMap.put(name, var);

      var.set(getGlobalVar("_SERVER").get(PHP_SELF_STRING));

      return var;
    }

    default: {
      if (_scriptContext != null) {
        Object value = _scriptContext.getAttribute(name);

        if (value == null) {
          Bindings bindings
          = _scriptContext.getBindings(ScriptContext.ENGINE_SCOPE);

          if (bindings != null)
            value = bindings.get(name);
        }

        if (value == null) {
          Bindings bindings
          = _scriptContext.getBindings(ScriptContext.GLOBAL_SCOPE);

          if (bindings != null)
            value = bindings.get(name);
        }

        if (value != null) {
          var = new Var();
          _globalMap.put(name, var);

          var.set(wrapJava(value));

          return var;
        }
      }
    }
    } // end switch

    return var;
  }

  private static String decodeValue(String s)
  {
    int len = s.length();
    StringBuilder sb = new StringBuilder();

    for (int i = 0; i < len; i++) {
      char ch = s.charAt(i);

      if (ch == '%' && i + 2 < len) {
        int d1 = s.charAt(i + 1);
        int d2 = s.charAt(i + 2);

        int v = 0;

        if ('0' <= d1 && d1 <= '9')
          v = 16 * (d1 - '0');
        else if ('a' <= d1 && d1 <= 'f')
          v = 16 * (d1 - 'a' + 10);
        else if ('A' <= d1 && d1 <= 'F')
          v = 16 * (d1 - 'A' + 10);
        else {
          sb.append('%');
          continue;
        }

        if ('0' <= d2 && d2 <= '9')
          v += (d2 - '0');
        else if ('a' <= d2 && d2 <= 'f')
          v += (d2 - 'a' + 10);
        else if ('A' <= d2 && d2 <= 'F')
          v += (d2 - 'A' + 10);
        else {
          sb.append('%');
          continue;
        }

        i += 2;
        sb.append((char) v);
      }
      else
        sb.append(ch);
    }

    return sb.toString();
  }

  /**
   * Gets a value.
   */
  public Var getVar(String name)
  {
    Var var = _map.get(name);

    if (var != null)
      return var;
    
    var = getRef(name);

    if (var == null) {
      var = new Var();

      if (_map == _globalMap) {
        // php/379c
        var.setGlobal();
      }

      _map.put(name, var);
    }

    return var;
  }

  /**
   * Gets a value.
   */
  public Var getGlobalVar(String name)
  {
    Var var = getGlobalRef(name);

    if (var == null) {
      var = new Var();
      var.setGlobal();
      _globalMap.put(name, var);
    }

    return var;
  }

  public boolean isGlobalEnv()
  {
    return _map == _globalMap;
  }

  /**
   * Sets a value.
   */
  public Value setValue(String name, Value value)
  {
    if (value instanceof Var)
      _map.put(name, (Var) value);
    else {
      Var var = getVar(name);
      var.set(value);
    }

    return value;
  }

  /**
   * Sets a special value, a special value is used to store and retrieve module
   * specific values in the env using a unique name.
   */
  public Object setSpecialValue(String name, Object value)
  {
    _specialMap.put(name, value);

    return value;
  }

  /**
   * Sets a value.
   */
  public Value setGlobalValue(String name, Value value)
  {
    if (value instanceof Var)
      _globalMap.put(name, (Var) value);
    else {
      Var var = getGlobalVar(name);
      var.set(value);
    }

    return value;
  }

  /**
   * Gets a static class field.
   *
   * @param className of the owning class 
   * @param name of the variable
   */
  public Value getStaticClassFieldValue(String className, String name)
  {
    Var var = getStaticClassFieldVar(className, name);
    
    if (var != null) {
      Value val = var.toValue();
        
      return val;
    }
    else
      return NullValue.NULL;
  }

  /**
   * Gets a static field from a class.
   *
   * @param className of the owning class 
   * @param name of the variable
   */
  public final Var getStaticClassFieldVar(String className, String name)
  {
    Var var = _globalMap.get(name);

    if (var == null) {
      // import in class fields and functions
      QuercusClass cl = findAbstractClass(className);
      cl.init(this);

      var = _globalMap.get(name);
    }
    
    if (var != null)
      return var;
    
    if (var == null) {
      var = new Var();
      var.setGlobal();
    }
    
    _globalMap.put(name, var);

    return var;
  }

  /**
   * Sets the calling function expression.
   */
  public void pushCall(Expr call, Value obj)
  {
    if (_callStack.length <= _callStackTop) {
      Expr []newStack = new Expr[2 * _callStack.length];
      System.arraycopy(_callStack, 0, newStack, 0, _callStack.length);
      _callStack = newStack;
      
      Value []newThisStack = new Value[2 * _callThisStack.length];
      System.arraycopy(_callThisStack,
		       0, newThisStack,
		       0, _callThisStack.length);
      _callThisStack = newThisStack;
    }

    _callStack[_callStackTop] = call;
    _callThisStack[_callStackTop] = obj;

    _callStackTop++;
  }

  /**
   * Pops the top call.
   */
  public Expr popCall()
  {
    return _callStack[--_callStackTop];
  }

  /**
   * Returns the stack depth.
   */
  public int getCallDepth()
  {
    return _callStackTop;
  }
  
  /**
   * Pops the top call.
   */
  public Expr peekCall(int depth)
  {
    if (_callStackTop - depth > 0)
      return _callStack[_callStackTop - depth - 1];
    else
      return null;
  }
  
  /**
   * Pops the top call.
   */
  public Value peekCallThis(int depth)
  {
    if (_callStackTop - depth > 0)
      return _callThisStack[_callStackTop - depth - 1];
    else
      return null;
  }

  /**
   * Pushes a new environment.
   */
  public HashMap<String, Var> pushEnv(HashMap<String, Var> map)
  {
    HashMap<String, Var> oldEnv = _map;

    _map = map;

    return oldEnv;
  }

  /**
   * Restores the old environment.
   */
  public void popEnv(HashMap<String, Var> oldEnv)
  {
    _map = oldEnv;
  }

  /**
   * Returns the current environment.
   */
  public HashMap<String, Var> getEnv()
  {
    return _map;
  }

  /**
   * Returns the current environment.
   */
  public HashMap<String, Var> getGlobalEnv()
  {
    return _globalMap;
  }

  /**
   * Pushes a new environment.
   */
  public final Value []setFunctionArgs(Value []args)
  {
    Value []oldArgs = _functionArgs;

    Value []newArgs = new Value[args.length];

    for (int i = 0; args != null && i < args.length; i++) {
      // php/3715, 3768
      newArgs[i] = args[i].toValue().toArgValue();
    }

    _functionArgs = newArgs;

    return oldArgs;
  }

  /**
   * Pushes a new environment.
   */
  public final Value []setFunctionArgsNoCopy(Value []args)
  {
    Value []oldArgs = _functionArgs;

    for (int i = 0; args != null && i < args.length; i++)
      args[i] = args[i].toValue();

    _functionArgs = args;

    return oldArgs;
  }

  /**
   * Pushes a new environment.
   */
  public final void restoreFunctionArgs(Value []args)
  {
    _functionArgs = args;
  }

  /**
   * Returns the function args.
   */
  public final Value []getFunctionArgs()
  {
    return _functionArgs;
  }

  /**
   * Removes a specialValue
   */
  public Object removeSpecialValue(String name)
  {
    return _specialMap.remove(name);
  }

  /**
   * Returns a constant.
   */
  public Value getConstant(String name)
  {
    Value value = getConstantImpl(name);

    if (value != null)
      return value;

    /* XXX:
       notice(L.l("Converting undefined constant '{0}' to string.",
       name));
    */

    value = new StringValueImpl(name);

    return value;
  }

  /**
   * Returns true if the constant is defined.
   */
  public boolean isDefined(String name)
  {
    return getConstantImpl(name) != null;
  }

  /**
   * Returns a constant.
   */
  private Value getConstantImpl(String name)
  {
    Value value = _constMap.get(name);

    if (value != null)
      return value;

    value = _quercus.getConstant(name);
    if (value != null)
      return value;

    if (_lowerConstMap != null) {
      value = _lowerConstMap.get(name.toLowerCase());

      if (value != null)
	return value;
    }

    return null;
  }
  
  /**
   * Removes a constant.
   */
  public Value removeConstant(String name)
  {
    return _constMap.remove(name);
  }

  /**
   * Sets a constant.
   */
  public Value addConstant(String name,
                           Value value,
                           boolean isCaseInsensitive)
  {
    Value oldValue = _constMap.get(name);

    if (oldValue != null)
      return oldValue;

    _constMap.put(name, value);

    if (_lowerConstMap != null && isCaseInsensitive)
      _lowerConstMap.put(name.toLowerCase(), value);

    return value;
  }

  /**
   * Returns an array of the defined functions.
   */
  public ArrayValue getDefinedConstants()
  {
    ArrayValue result = new ArrayValueImpl();

    for (Map.Entry<String, Value> entry : _quercus.getConstMap().entrySet()) {
      result.put(new StringValueImpl(entry.getKey()), entry.getValue());
    }

    for (Map.Entry<String, Value> entry : _constMap.entrySet()) {
      result.put(new StringValueImpl(entry.getKey()), entry.getValue());
    }

    return result;
  }

  /**
   * Returns true if an extension is loaded.
   */
  public boolean isExtensionLoaded(String name)
  {
    return getQuercus().isExtensionLoaded(name);
  }

  /**
   * Returns true if an extension is loaded.
   */
  public HashSet<String> getLoadedExtensions()
  {
    return getQuercus().getLoadedExtensions();
  }

  /**
   * Returns true if an extension is loaded.
   */
  public Value getExtensionFuncs(String name)
  {
    return getQuercus().getExtensionFuncs(name);
  }

  /**
   * Returns the default stream resource.
   */
  public StreamContextResource getDefaultStreamContext()
  {
    if (_defaultStreamContext == null)
      _defaultStreamContext = new StreamContextResource();

    return _defaultStreamContext;
  }

  /**
   * Returns an array of the defined functions.
   */
  /*
  public ArrayValue getDefinedFunctions()
  {
    ArrayValue result = new ArrayValueImpl();

    ArrayValue internal = _quercus.getDefinedFunctions();
    ArrayValue user = new ArrayValueImpl();

    result.put(new StringValueImpl("internal"), internal);
    result.put(new StringValueImpl("user"), user);

    for (String name : _funMap.keySet()) {
      StringValue key = new StringValueImpl(name);

      if (! internal.contains(key).isset())
        user.put(name);
    }

    return result;
  }
  */
  
  public ArrayValue getDefinedFunctions()
  {
    return _defState.getDefinedFunctions();
  }

  /**
   * Finds the java reflection method for the function with the given name.
   *
   * @param name the method name
   * @return the found method or null if no method found.
   */
  /*
  public AbstractFunction findFunction(String name)
  {
    AbstractFunction fun = _funMap.get(name);

    if (fun != null)
      return fun;
    
    if (! isStrict()) {
      fun = _lowerFunMap.get(name.toLowerCase());

      if (fun != null) {
	_funMap.put(name, fun);

	return fun;
      }
    }

    fun = findFunctionImpl(name);

    if (fun != null) {
      _funMap.put(name, fun);

      return fun;
    }
    else
      return null;
  }
  */
  public AbstractFunction findFunction(String name)
  {
    return _defState.findFunction(name);
  }

  /**
   * Finds the java reflection method for the function with the given name.
   *
   * @param name the method name
   * @return the found method or null if no method found.
   */
  /*
  public AbstractFunction getFunction(String name)
  {
    AbstractFunction fun = _funMap.get(name);

    if (fun != null)
      return fun;
    
    fun = findFunction(name);
    
    if (fun != null) {
      return fun;
    }

    throw errorException(L.l("'{0}' is an unknown function.", name));
  }
  */
  public AbstractFunction getFunction(String name)
  {
    AbstractFunction fun = _defState.findFunction(name);

    if (fun != null)
      return fun;
    else
      throw errorException(L.l("'{0}' is an unknown function", name));
  }

  /**
   * Finds the java reflection method for the function with the given name.
   *
   * @param name the method name
   * @return the found method or null if no method found.
   */
  public AbstractFunction getFunction(Value name)
  {
    name = name.toValue();

    if (name instanceof CallbackFunction)
      return ((CallbackFunction) name).getFunction();

    AbstractFunction fun = _defState.findFunction(name.toString());

    if (fun != null)
      return fun;

    throw errorException(L.l("'{0}' is an unknown function.", name));
  }

  /**
   * Finds the java reflection method for the function with the given name.
   *
   * @param name the method name
   * @return the found method or null if no method found.
   */
  /*
  private AbstractFunction findFunctionImpl(String name)
  {
    AbstractFunction fun = null;

    fun = _quercus.findFunction(name);
    if (fun != null)
      return fun;

    return fun;
  }
  */

  /**
   * Adds a function, e.g. from an include.
   */
  /*
  public Value addFunction(String name, AbstractFunction fun)
  {
    AbstractFunction oldFun = findFunction(name);

    if (oldFun != null) {
      throw new QuercusException(L.l("can't redefine function {0}", name));
    }

    _funMap.put(name, fun);

    if (! isStrict())
      _lowerFunMap.put(name.toLowerCase(), fun);

    return BooleanValue.TRUE;
  }
  */

  public DefinitionState getDefinitionState()
  {
    return _defState;
  }
  
  public Value addFunction(String name, AbstractFunction fun)
  {
    return _defState.addFunction(name, fun);
  }

  /**
   * Adds a function from a compiled include
   *
   * @param name the function name, must be an intern() string
   * @param lowerName the function name, must be an intern() string
   */
  public Value addFunctionFromPage(String name, String lowerName, AbstractFunction fun)
  {
    // XXX: skip the old function check since the include for compiled
    // pages is already verified.  Might have a switch here?
    /*
    AbstractFunction oldFun = _lowerFunMap.get(lowerName);

    if (oldFun == null)
      oldFun = _quercus.findLowerFunctionImpl(lowerName);

    if (oldFun != null) {
      throw new QuercusException(L.l("can't redefine function {0}", name));
    }

    _funMap.put(name, fun);

    if (! isStrict())
      _lowerFunMap.put(lowerName, fun);
    */

    return BooleanValue.TRUE;
  }

  /**
   * Finds the java reflection method for the function with the given name.
   *
   * @param className the class name
   * @param methodName the method name
   * @return the found method or null if no method found.
   */
  public AbstractFunction findMethod(String className, String methodName)
  {
    QuercusClass cl = findClass(className);

    if (cl == null) {
      error(L.l("'{0}' is an unknown class.", className));
      return null;
    }

    AbstractFunction fun = cl.findFunction(methodName);

    if (fun == null && ! isStrict())
      fun = cl.findFunctionLowerCase(methodName.toLowerCase());

    if (fun == null) {
      error(L.l("'{0}::{1}' is an unknown method.",
                className, methodName));
      return null;
    }

    return fun;
  }

  /**
   * Compiles and calluates the given code
   *
   * @param code the code to calluate
   * @return the result
   */
  public Value evalCode(String code)
    throws IOException
  {
    if (log.isLoggable(Level.FINER))
      log.finer(code);

    Quercus quercus = getQuercus();

    QuercusProgram program = quercus.parseEvalExpr(code);

    Value value = program.execute(this);

    return value;
  }

  /**
   * Evaluates the named function.
   *
   * @param name the function name
   * @return the function value
   */
  public Value call(String name)
  {
    AbstractFunction fun = findFunction(name);

    if (fun == null)
      return error(L.l("'{0}' is an unknown function.", name));

    return fun.call(this);
  }

  /**
   * Evaluates the named function.
   *
   * @param name the function name
   * @param a0 the first argument
   * @return the function value
   */
  public Value call(String name, Value a0)
  {
    AbstractFunction fun = findFunction(name);

    if (fun == null)
      return error(L.l("'{0}' is an unknown function.", name));

    return fun.call(this, a0);
  }

  /**
   * Evaluates the named function.
   *
   * @param name the function name
   * @param a0 the first argument
   * @param a1 the second argument
   * @return the function value
   */
  public Value call(String name, Value a0, Value a1)
  {
    return getFunction(name).call(this, a0, a1);
  }

  /**
   * Evaluates the named function.
   *
   * @param name the function name
   * @param a0 the first argument
   * @param a1 the second argument
   * @param a2 the third argument
   * @return the function value
   */
  public Value call(String name, Value a0, Value a1, Value a2)
  {
    return getFunction(name).call(this, a0, a1, a2);
  }

  /**
   * Evaluates the named function.
   *
   * @param name the function name
   * @param a0 the first argument
   * @param a1 the second argument
   * @param a2 the third argument
   * @param a3 the fourth argument
   * @return the function value
   */
  public Value call(String name, Value a0, Value a1, Value a2, Value a3)
  {
    return getFunction(name).call(this, a0, a1, a2, a3);
  }

  /**
   * Evaluates the named function.
   *
   * @param name the function name
   * @param a0 the first argument
   * @param a1 the second argument
   * @param a2 the third argument
   * @param a3 the fourth argument
   * @param a4 the fifth argument
   * @return the function value
   */
  public Value call(String name, Value a0, Value a1,
                    Value a2, Value a3, Value a4)
  {
    return getFunction(name).call(this, a0, a1, a2, a3, a4);
  }

  /**
   * Evaluates the named function.
   *
   * @param name the function name
   * @param args the arguments
   * @return the function value
   */
  public Value call(String name, Value []args)
  {
    return getFunction(name).call(this, args);
  }

  /**
   * Evaluates the named function.
   *
   * @param name the function name
   * @return the function value
   */
  public Value callRef(String name)
  {
    AbstractFunction fun = findFunction(name);

    if (fun == null)
      return error(L.l("'{0}' is an unknown function.", name));

    return fun.callRef(this);
  }

  /**
   * EvalRefuates the named function.
   *
   * @param name the function name
   * @param a0 the first argument
   * @return the function value
   */
  public Value callRef(String name, Value a0)
  {
    AbstractFunction fun = findFunction(name);

    if (fun == null)
      return error(L.l("'{0}' is an unknown function.", name));

    return fun.callRef(this, a0);
  }

  /**
   * EvalRefuates the named function.
   *
   * @param name the function name
   * @param a0 the first argument
   * @param a1 the second argument
   * @return the function value
   */
  public Value callRef(String name, Value a0, Value a1)
  {
    AbstractFunction fun = findFunction(name);

    if (fun == null)
      return error(L.l("'{0}' is an unknown function.", name));

    return fun.callRef(this, a0, a1);
  }

  /**
   * EvalRefuates the named function.
   *
   * @param name the function name
   * @param a0 the first argument
   * @param a1 the second argument
   * @param a2 the third argument
   * @return the function value
   */
  public Value callRef(String name, Value a0, Value a1, Value a2)
  {
    AbstractFunction fun = findFunction(name);

    if (fun == null)
      return error(L.l("'{0}' is an unknown function.", name));

    return fun.callRef(this, a0, a1, a2);
  }

  /**
   * Evaluates the named function.
   *
   * @param name the function name
   * @param a0 the first argument
   * @param a1 the second argument
   * @param a2 the third argument
   * @param a3 the fourth argument
   * @return the function value
   */
  public Value callRef(String name, Value a0, Value a1, Value a2, Value a3)
  {
    AbstractFunction fun = findFunction(name);

    if (fun == null)
      return error(L.l("'{0}' is an unknown function.", name));

    return fun.callRef(this, a0, a1, a2, a3);
  }

  /**
   * Evaluates the named function.
   *
   * @param name the function name
   * @param a0 the first argument
   * @param a1 the second argument
   * @param a2 the third argument
   * @param a3 the fourth argument
   * @param a4 the fifth argument
   * @return the function value
   */
  public Value callRef(String name, Value a0, Value a1,
                       Value a2, Value a3, Value a4)
  {
    AbstractFunction fun = findFunction(name);

    if (fun == null)
      return error(L.l("'{0}' is an unknown function.", name));

    return fun.callRef(this, a0, a1, a2, a3, a4);
  }

  /**
   * Evaluates the named function.
   *
   * @param name the function name
   * @param args the arguments
   * @return the function value
   */
  public Value callRef(String name, Value []args)
  {
    AbstractFunction fun = findFunction(name);

    if (fun == null)
      return error(L.l("'{0}' is an unknown function.", name));

    return fun.callRef(this, args);
  }

  /**
   * Adds a class, e.g. from an include.
   */
  public void addClassDef(String name, ClassDef cl)
  {
    _defState.addClassDef(name, cl);
  }

  public ClassDef findClassDef(String name)
  {
    return _defState.findClassDef(name);
  }

  /**
   * Creates a stdClass object.
   */
  public ObjectValue createObject()
  {
    try {
      return (ObjectValue) _quercus.getStdClass().newInstance(this);
    }
    catch (Throwable e) {
      throw new QuercusModuleException(e);
    }
  }

  /**
   * Creates a string from a byte.
   */
  public Value createString(byte []buffer, int offset, int length)
  {
    return new StringValueImpl(new String(buffer, offset, length));
  }

  /**
   * Generate an object id.
   */
  public int generateId()
  {
    return ++_objectId;
  }

  /**
   * Returns a Java class defintion.
   */
  public JavaClassDef getJavaClassDefinition(String className)
  {
    JavaClassDef def = _quercus.getJavaClassDefinition(className);

    def.init();

    return def;
  }

  /**
   * Returns a PHP value for a Java object
   */
  public Value wrapJava(Object obj)
  {
    return wrapJava(obj, null, false);
  }

  /**
   * Returns a PHP value for a Java object
   *
   * @param isNullAsFalse what to return if <i>obj</i> is null, if true return
   * {@link BooleanValue.FALSE} otherwise return {@link NullValue.NULL)
   */
  public Value wrapJava(Object obj, boolean isNullAsFalse)
  {
    return wrapJava(obj, null, isNullAsFalse);
  }

  /**
   * Returns a PHP value for a Java object
   *
   * @param isNullAsFalse what to return if <i>obj</i> is null, if true return
   * {@link BooleanValue.FALSE} otherwise return {@link NullValue.NULL)
   */
  public Value wrapJava(Object obj, JavaClassDef def, boolean isNullAsFalse)
  {
    if (obj == null) {
      if (isNullAsFalse)
        return BooleanValue.FALSE;
      else
        return NullValue.NULL;
    }

    if (obj instanceof Value)
      return (Value) obj;

    if (def == null)
      def = getJavaClassDefinition(obj.getClass().getName());
    else if (def.getType() != obj.getClass()) {
      // XXX: what if types are incompatible, does it matter?
      // if it doesn't matter, simplify this to one if with no else
      def = getJavaClassDefinition(obj.getClass().getName());
    }

    if (def.isArray()) {
      ArrayValueImpl arrayValueImpl = new ArrayValueImpl();

      Class componentClass = def.getType().getComponentType();

      MarshalFactory factory = _quercus.getModuleContext().getMarshalFactory();
      Marshal componentClassMarshal = factory.create(componentClass);

      int length = Array.getLength(obj);
      
      for (int i = 0; i < length; i++) {
        arrayValueImpl.put(componentClassMarshal.unmarshal(this, Array.get(obj, i)));
      }

      return arrayValueImpl;
    }
    else {
      return def.wrap(this, obj);
    }
  }

  /**
   * Finds the class with the given name.
   *
   * @param name the class name
   * @return the found class or null if no class found.
   */
  public QuercusClass findClass(String name)
  {
    return findClass(name, true);
  }

  /**
   * Finds the class with the given name.
   *
   * @param name the class name
   * @param useAutoload use autoload to locate the class if necessary
   * @return the found class or null if no class found.
   */
  public QuercusClass findClass(String name, boolean useAutoload)
  {
    QuercusClass cl = _classMap.get(name);

    if (cl != null)
      return cl;

    if (_lowerClassMap != null)
      cl = _lowerClassMap.get(name.toLowerCase());

    if (cl != null)
      return cl;

    cl = createClassImpl(name, useAutoload);

    if (cl != null) {
      _classMap.put(name, cl);

      if (_lowerClassMap != null)
	_lowerClassMap.put(name.toLowerCase(), cl);

      return cl;
    }
    else
      return null;
  }

  /**
   * Finds the class with the given name.
   *
   * @param name the class name
   * @param useAutoload use autoload to locate the class if necessary
   * @return the found class or null if no class found.
   */
  private QuercusClass createClassImpl(String name, boolean useAutoload)
  {
    ClassDef classDef = _defState.findClassDef(name);

    if (classDef != null) {
      String parentName = classDef.getParentName();

      QuercusClass parent = null;

      if (parentName != null)
        parent = findClass(parentName);

      if (parentName == null || parent instanceof QuercusClass)
        return createQuercusClass(classDef, (QuercusClass) parent);
      else
        return null; // php/
    }

    ClassDef staticClass = _quercus.findClass(name);
    if (staticClass != null)
      return createQuercusClass(staticClass, null); // XXX: cache

    if (useAutoload) {
      if (_autoload == null)
        _autoload = findFunction("__autoload");
      
      if (_autoload != null) {
	_autoload.call(this, new StringValueImpl(name));
	return createClassImpl(name, false);
      }
    }


    return null;
  }

  /**
   * Returns the declared classes.
   *
   * @return an array of the declared classes()
   */
  public Value getDeclaredClasses()
  {
    return _defState.getDeclaredClasses();
  }

  /**
   * Finds the class with the given name.
   *
   * @param name the class name
   * @return the found class or null if no class found.
   */
  public QuercusClass findAbstractClass(String name)
  {
    QuercusClass cl = findClass(name, true);

    if (cl != null)
      return cl;

    throw errorException(L.l("'{0}' is an unknown class name.", name));
    /*
    // return _quercus.findJavaClassWrapper(name);

    return null;
    */
  }

  /**
   * Finds the class with the given name.
   *
   * @param name the class name
   * @return the found class or null if no class found.
   */
  public QuercusClass getClass(String name)
  {
    QuercusClass cl = findClass(name);

    if (cl != null)
      return cl;
    else
      throw errorException(L.l("'{0}' is an unknown class.", name));
  }

  private QuercusClass createQuercusClass(ClassDef def, QuercusClass parent)
  {
    ClassKey key = new ClassKey(def, parent);

    SoftReference<QuercusClass> qClassRef = _classCache.get(key);
    QuercusClass qClass;

    if (qClassRef != null) {
      qClass = qClassRef.get();

      if (qClass != null) {
        qClass.init(this);
        return qClass;
      }
    }

    qClass = new QuercusClass(def, parent);
    qClass.validate(this);

    _classCache.put(key, new SoftReference<QuercusClass>(qClass));

    qClass.init(this);

    return qClass;
  }

  /**
   * Returns true if class has already been initialized.
   */
  public boolean isInitializedClass(String name)
  {
    return _initializedClassSet.contains(name);
  }
  
  /**
   * Mark this class as being initialized.
   */
  public void addInitializedClass(String name)
  {
    _initializedClassSet.add(name);
  }

  /**
   * Finds the class and method.
   *
   * @param className the class name
   * @param methodName the method name
   * @return the found method or null if no method found.
   */
  public AbstractFunction findFunction(String className, String methodName)
  {
    QuercusClass cl = findClass(className);

    if (cl == null)
      throw new QuercusRuntimeException(L.l("'{0}' is an unknown class",
                                            className));

    return cl.findFunction(methodName);
  }

  /**
   * Returns the appropriate callback.
   */
  public Callback createCallback(Value value)
  {
    if (value == null || value.isNull())
      return null;

    value = value.toValue();

    if (value instanceof Callback)
      return (Callback) value;

    else if (value instanceof StringValue)
      return new CallbackFunction(this, value.toString());

    else if (value instanceof ArrayValue) {
      Value obj = value.get(LongValue.ZERO);
      Value name = value.get(LongValue.ONE);

      if (! (name instanceof StringValue))
        throw new IllegalStateException(L.l("unknown callback name {0}", name));

      if (obj instanceof StringValue) {
        QuercusClass cl = findClass(obj.toString());

        if (cl == null)
          throw new IllegalStateException(L.l("can't find class {0}",
                                              obj.toString()));

        return new CallbackFunction(cl.getFunction(name.toString()));
      }
      else {
        return new CallbackObjectMethod(this, obj, name.toString());
      }
    }
    else
      return null;
  }

  /**
   * Evaluates an included file.
   */
  public Value require_once(String include)
  {
    return include(getSelfDirectory(), include, true, true);
  }

  /**
   * Evaluates an included file.
   */
  public Value require(String include)
  {
    return include(getSelfDirectory(), include, true, false);
  }

  /**
   * Evaluates an included file.
   */
  public Value include(String include)
  {
    return include(getSelfDirectory(), include, false, false);
  }

  /**
   * Evaluates an included file.
   */
  public Value include_once(String include)
  {
    return include(getSelfDirectory(), include, false, true);
  }

  /**
   * Evaluates an included file.
   */
  public Value include(Path scriptPwd, String include,
		       boolean isRequire, boolean isOnce)
  {
    try {
      Path pwd = getPwd();

      Path path = lookupInclude(include, pwd, scriptPwd);

      if (path != null) {
      }
      else if (isRequire) {
	error(L.l("'{0}' is not a valid path", include));
	return NullValue.NULL;
      }
      else {
	warning(L.l("'{0}' is not a valid path", include));
	return NullValue.NULL;
      }

      if (isOnce && _includeSet.contains(path))
	return NullValue.NULL;

      _includeSet.add(path);

      QuercusPage page;

      page = _quercus.parse(path);

      importPage(page);

      return page.execute(this);
    } catch (IOException e) {
      throw new QuercusModuleException(e);
    }
  }

  private void importPage(QuercusPage page)
  {
    long crc = _defState.getCrc();

    DefinitionKey key = new DefinitionKey(crc, page);
      
    DefinitionState defState = _quercus.getDefinitionCache(key);

    if (defState != null) {
      _defState = defState;
    }
    else {
      page.importDefinitions(this);

      _quercus.putDefinitionCache(key, _defState);
    }
  }

  /**
   * Looks up based on the pwd.
   */
  public Path lookupPwd(Value relPathV)
  {
    if (! relPathV.isset())
      return null;

    StringValue relPath = relPathV.toStringValue();

    if (relPath.length() == 0)
      return null;
    
    Path path = _lookupCache.get(relPath);

    if (path == null) {
      path = getPwd().lookup(relPath.toString());
      _lookupCache.put(relPath, path);
    }

    return path;
  }

  /**
   * Looks up the path.
   */
  public Path lookup(String relPath)
  {
    return lookupInclude(getSelfDirectory(), relPath);
  }

  /**
   * Looks up the path.
   */
  public Path lookupInclude(String relPath)
  {
    return lookupInclude(relPath, getPwd(), getSelfDirectory());
  }

  private Path lookupInclude(String include, Path pwd, Path scriptPwd)
  {
    String includePath = getIniString("include_path");

    if (includePath == null)
      includePath = ".";
    
    Path path = _quercus.getIncludeCache(include, includePath, pwd, scriptPwd);

    if (path == null) {
      path = lookupIncludeImpl(include, pwd, scriptPwd);

      if (path != null)
	_quercus.putIncludeCache(include, includePath, pwd, scriptPwd, path);
    }
    
    return path;
  }
    
  private Path lookupIncludeImpl(String include, Path pwd, Path scriptPwd)
  {
    // php/0b0g

    Path path = lookupInclude(pwd, include);

    if (path == null) {
      // php/0b0l
      path = lookupInclude(scriptPwd, include);
    }

    if (path == null) {
      // php/0b21
      path = scriptPwd.lookup(include);

      if (! path.canRead() || path.isDirectory())
	path = null;
    }

    return path;
  }

  /**
   * Looks up the path.
   */
  private Path lookupInclude(Path pwd, String relPath)
  {
    ArrayList<Path> pathList = getIncludePath(pwd);

    for (int i = 0; i < pathList.size(); i++) {
      Path path = pathList.get(i).lookup(relPath);

      if (path.canRead() && ! path.isDirectory()) {
        return path;
      }
    }

    return null;
  }

  /**
   * Returns the include path.
   */
  private ArrayList<Path> getIncludePath(Path pwd)
  {
    String includePath = getIniString("include_path");

    if (includePath == null)
      includePath = ".";

    if (! includePath.equals(_includePath)) {
      _includePathList = new ArrayList<String>();
      _includePathMap = new HashMap<Path,ArrayList<Path>>();

      int head = 0;
      int tail;

      String pathSeparator = FileModule.PATH_SEPARATOR;
      int length = pathSeparator.length();

      while ((tail = includePath.indexOf(pathSeparator, head)) >= 0) {
        String subpath = includePath.substring(head, tail);
        
        _includePathList.add(subpath);

        head = tail + length;
      }

      String subpath = includePath.substring(head);

      _includePathList.add(subpath);

      _includePath = includePath;
    }

    ArrayList<Path> pathList = _includePathMap.get(pwd);

    if (pathList == null) {
      pathList = new ArrayList<Path>();

      for (int i = 0; i < _includePathList.size(); i++) {
	pathList.add(pwd.lookup(_includePathList.get(i)));
      }

      _includePathMap.put(pwd, pathList);
    }

    return pathList;
  }

  /**
   * Sets the include path.
   */
  public String setIncludePath(String path)
  {
    _prevIncludePath = getIniString("include_path");

    if (_prevIncludePath == null)
      _prevIncludePath = "";

    setIni("include_path", path);

    return _prevIncludePath;
  }

  /**
   * Restores the previous include path.
   */
  public void restoreIncludePath()
  {
    String path = getIniString("include_path");

    setIni("include_path", _prevIncludePath);

    _prevIncludePath = path;
  }

  /**
   * Handles error suppression.
   */
  public Value suppress(int errorMask, Value value)
  {
    setErrorMask(errorMask);

    return value;
  }

  /**
   * Handles exit/die
   */
  public Value exit(String msg)
  {
    try {
      getOut().print(msg);
    } catch (IOException e) {
      log.log(Level.WARNING, e.toString(), e);
    }

    throw new QuercusExitException(msg);
  }

  /**
   * Handles exit/die
   */
  public Value exit()
  {
    throw new QuercusExitException();
  }

  /**
   * Handles exit/die
   */
  public Value die(String msg)
  {
    try {
      getOut().print(msg);
    } catch (IOException e) {
      log.log(Level.WARNING, e.toString(), e);
    }

    throw new QuercusDieException(msg);
  }

  /**
   * Handles exit/die
   */
  public Value die()
  {
    throw new QuercusDieException();
  }

  /**
   * Handles exit/die
   */
  public Value cast(Class cl, Value value)
  {
    value = value.toValue();

    if (value.isNull())
      return null;
    else if (cl.isAssignableFrom(value.getClass()))
      return value;
    else {
      error(L.l("{0} ({1}) is not assignable to {2}",
                value, value.getClass().getName(), cl.getName()));

      return value;
    }
  }

  /**
   * Returns the first value
   */
  public static Value first(Value value)
  {
    return value;
  }

  /**
   * Returns the first value
   */
  public static Value first(Value value, Value a1)
  {
    return value;
  }

  /**
   * Returns the first value
   */
  public static Value first(Value value, Value a1, Value a2)
  {
    return value;
  }

  /**
   * Returns the first value
   */
  public static Value first(Value value, Value a1, Value a2, Value a3)
  {
    return value;
  }

  /**
   * Returns the first value
   */
  public static Value first(Value value, Value a1, Value a2, Value a3,
                            Value a4)
  {
    return value;
  }

  /**
   * Returns the first value
   */
  public static Value first(Value value, Value a1, Value a2, Value a3,
                            Value a4, Value a5)
  {
    return value;
  }

  /**
   * A fatal runtime error.
   */
  public Value error(String msg)
  {
    return error(B_ERROR, "", msg + getFunctionLocation());
  }

  /**
   * A fatal runtime error.
   */
  public Value error(String loc, String msg)
  {
    return error(B_ERROR, loc, msg + getFunctionLocation());
  }

  /**
   * A warning with an exception.
   */
  public Value error(String msg, Throwable e)
  {
    log.log(Level.WARNING, e.toString(), e);

    return error(msg);
  }

  /**
   * A warning with an exception.
   */
  public Value error(Throwable e)
  {
    log.log(Level.WARNING, e.toString(), e);

    return error(e.toString());
  }

  /**
   * A fatal runtime error.
   */
  public RuntimeException errorException(String msg)
  {
    Location location = getLocation();
    String prefix = location.getMessagePrefix();
    
    String fullMsg = prefix + msg + getFunctionLocation();

    error(B_ERROR, "", fullMsg);

    throw new QuercusRuntimeException(fullMsg);
  }

  /**
   * A runtime warning.
   */
  public Value warning(String msg)
  {
    if (log.isLoggable(Level.FINER)) {
      QuercusException e = new QuercusException(msg);

      log.log(Level.FINER, e.toString(), e);
    }

    return error(B_WARNING, "", msg + getFunctionLocation());
  }

  /**
   * A warning with an exception.
   */
  public Value warning(String msg, Throwable e)
  {
    log.log(Level.FINE, e.toString(), e);

    return warning(msg);
  }

  /**
   * A warning with an exception.
   */
  public Value warning(Throwable e)
  {
    return warning(e.toString(), e);
  }

  /**
   * A runtime strict warning.
   */
  public Value strict(String msg)
  {
    if (log.isLoggable(Level.FINER)) {
      QuercusException e = new QuercusException(msg);

      log.log(Level.FINER, e.toString(), e);
    }

    return error(B_STRICT, "", msg + getFunctionLocation());
  }

  /**
   * A warning about an invalid argument passed to a function.
   */
  public Value invalidArgument(String name, Object value)
  {
    return warning(L.l("invalid value `{0}' for `{1}'", value, name));
  }

  /**
   * A warning about an deprecated argument passed to a function.
   */
  public Value deprecatedArgument(String name)
  {
    return strict(L.l("argument `{1}' is deprecated", name));
  }

  /**
   * A notice.
   */
  public Value notice(String msg)
  {
    return error(B_NOTICE, "", msg + getFunctionLocation());
  }

  /**
   * A notice with an exception.
   */
  public Value notice(String msg, Throwable e)
  {
    log.log(Level.FINE, e.toString(), e);

    return notice(msg);
  }

  /**
   * A stub notice.
   */
  public Value stub(String msg)
  {
    if (log.isLoggable(Level.FINE))
      log.fine(getLocation().getMessagePrefix() + msg);

    return NullValue.NULL;
  }

  public static Value nullAsFalse(Value value)
  {
    return value == null || value.isNull() ? BooleanValue.FALSE : value;
  }

  /**
   * A parse error
   */
  public Value parse(String msg)
    throws Exception
  {
    return error(B_PARSE, "", msg);
  }

  /**
   * A parse error
   */
  public Value compileError(String msg)
  {
    return error(B_COMPILE_ERROR, "", msg);
  }

  /**
   * A parse warning
   */
  public Value compileWarning(String msg)
  {
    return error(B_COMPILE_WARNING, "", msg);
  }

  /**
   * Returns the error mask.
   */
  public int getErrorMask()
  {
    return _errorMask;
  }

  /**
   * Sets the error mask.
   */
  public int setErrorMask(int mask)
  {
    int oldMask = _errorMask;

    _errorMask = mask;

    return oldMask;
  }

  /**
   * Sets an error handler
   */
  public void setErrorHandler(int mask, Callback fun)
  {
    for (int i = 0; i < _errorHandlers.length; i++)
      _prevErrorHandlers[i] = _errorHandlers[i];

    if ((mask & E_ERROR) != 0)
      _errorHandlers[B_ERROR] = fun;

    if ((mask & E_WARNING) != 0)
      _errorHandlers[B_WARNING] = fun;

    if ((mask & E_PARSE) != 0)
      _errorHandlers[B_PARSE] = fun;

    if ((mask & E_NOTICE) != 0)
      _errorHandlers[B_NOTICE] = fun;

    if ((mask & E_USER_ERROR) != 0)
      _errorHandlers[B_USER_ERROR] = fun;

    if ((mask & E_USER_WARNING) != 0)
      _errorHandlers[B_USER_WARNING] = fun;

    if ((mask & E_USER_NOTICE) != 0)
      _errorHandlers[B_USER_NOTICE] = fun;

    if ((mask & E_STRICT) != 0)
      _errorHandlers[B_STRICT] = fun;
  }

  /**
   * Sets an error handler
   */
  public void restoreErrorHandler()
  {
    for (int i = 0; i < _errorHandlers.length; i++)
      _errorHandlers[i] = _prevErrorHandlers[i];
  }
  
  /**
   * Gets the exception handler
   */
  public Callback getExceptionHandler()
  {
    return _exceptionHandler;
  }
  
  /**
   * Sets an exception handler
   */
  public Value setExceptionHandler(Callback fun)
  {
    _prevExceptionHandler = _exceptionHandler;
    
    _exceptionHandler = fun;

    if (_prevExceptionHandler != null)
      return _prevExceptionHandler.toStringValue();
    else
      return NullValue.NULL;
  }
  
  /**
   * Restore an exception handler
   */
  public void restoreExceptionHandler()
  {
    _exceptionHandler = _prevExceptionHandler;
  }

  /**
   * Writes an error.
   */
  public Value error(int code, String loc, String msg)
  {
    int mask = 1 << code;

    Location location = getLocation();

    String locationMessagePrefix = loc;

    if (loc.equals(""))
      locationMessagePrefix = location.getMessagePrefix();

    if (code >= 0 && code < _errorHandlers.length &&
        _errorHandlers[code] != null) {
      Callback handler = _errorHandlers[code];

      try {
        _errorHandlers[code] = null;

        Value fileNameV = NullValue.NULL;

        String fileName = location.getFileName();

        if (fileName != null)
          fileNameV = new StringValueImpl(fileName);

        Value lineV = NullValue.NULL;
        int line = location.getLineNumber();
        if (line > 0)
          lineV = new LongValue(line);

        Value context = NullValue.NULL;

        handler.call(this, new LongValue(mask), new StringValueImpl(msg),
                     fileNameV, lineV, context);

        return NullValue.NULL;
      }
      catch (RuntimeException e) {
        throw e;
      }
      catch (Throwable e) {
        throw new RuntimeException(e);
      }
      finally {
        _errorHandlers[code] = handler;
      }
    }

    if ((_errorMask & mask) != 0) {
      try {
	String fullMsg = locationMessagePrefix + getCodeName(mask) + msg;

	if (getIniBoolean("track_errors"))
	  setGlobalValue("php_errormsg", new StringValueImpl(fullMsg));

	if (getIniBoolean("display_errors"))
	  getOut().println(fullMsg);

	if (getIniBoolean("log_errors"))
	  log.info(fullMsg);
      }
      catch (IOException e) {
        log.log(Level.FINE, e.toString(), e);
      }
    }

    if ((mask & (E_ERROR | E_CORE_ERROR | E_COMPILE_ERROR | E_USER_ERROR)) != 0)
    {
      if (! "".equals(locationMessagePrefix)) {
	/*
        throw new QuercusLineExitException(getLocation() +
                                              getCodeName(mask) +
                                              msg);
	*/
        throw new QuercusExitException(locationMessagePrefix +
                                       getCodeName(mask) +
                                       msg);
      }
      else
        throw new QuercusExitException(msg);
    }

    return NullValue.NULL;
  }

  /**
   * Returns the error code name.
   */
  private String getCodeName(int code)
  {
    switch (code) {
    case E_ERROR:
      return "Fatal Error: ";
    case E_WARNING:
      return "Warning: ";
    case E_PARSE:
      return "Parse Error: ";
    case E_NOTICE:
      return "Notice: ";
    case E_CORE_ERROR:
      return "Fatal Error: ";
    case E_CORE_WARNING:
      return "Warning: ";
    case E_COMPILE_ERROR:
      return "Fatal Error: ";
    case E_COMPILE_WARNING:
      return "Warning : ";
    case E_USER_ERROR:
      return "Fatal Error: ";
    case E_USER_WARNING:
      return "Warning: ";
    case E_USER_NOTICE:
      return "Notice: ";
    case E_STRICT:
      return "Notice: ";

    default:
      return String.valueOf("ErrorCode(" + code + ")");
    }
  }

  /**
   * Returns the source of an error line.
   */
  public static String []getSourceLine(Path path, int sourceLine, int length)
  {
    if (path == null)
      return null;

    ReadStream is = null;

    try {
      is = path.openRead();
      int ch;
      boolean hasCr = false;
      int line = 1;

      while (line < sourceLine) {
        ch = is.read();

        if (ch < 0)
          return null;
        else if (ch == '\r') {
          hasCr = true;
          line++;
        }
        else if (ch == '\n') {
          if (! hasCr)
            line++;
          hasCr = false;
        }
        else
          hasCr = false;
      }

      String []result = new String[length];

      int i = 0;
      StringBuilder sb = new StringBuilder();
      while (i < length && (ch = is.read()) > 0) {
        if (ch == '\n' && hasCr) {
          hasCr = false;
          continue;
        }
        else if (ch == '\r') {
          hasCr = true;
          result[i++] = sb.toString();
          sb.setLength(0);
        }
        else if (ch == '\n') {
          hasCr = false;
          result[i++] = sb.toString();
          sb.setLength(0);
        }
        else {
          hasCr = false;
          sb.append((char) ch);
        }
      }

      if (i < length)
        result[i] = sb.toString();

      return result;
    }
    catch (IOException e) {
      log.log(Level.FINE, e.toString(), e);
    }
    finally {
      if (is != null)
	is.close();
    }

    return null;
  }

  /**
   * Returns the current execution location.
   *
   * Use with care, for compiled code this can be a relatively expensive
   * operation.
   */
  public Location getLocation()
  {
    Expr call = peekCall(0);

    if (call != null)
      return call.getLocation();

    return Location.UNKNOWN;
  }

  public int getSourceLine(String className, int javaLine)
  {
    return javaLine;
  }

  /**
   * Returns the current function.
   */
  public String getFunctionLocation()
  {
    // XXX: need to work with compiled code, too
    Expr call = peekCall(0);

    if (call != null)
      return call.getFunctionLocation();
    else
      return "";
  }

  /**
   * Converts a boolean to the boolean value
   */
  public static Value toValue(boolean value)
  {
    return value ? BooleanValue.TRUE : BooleanValue.FALSE;
  }

  /**
   * Converts a boolean to the boolean value
   */
  public static Value toValue(long value)
  {
    return new LongValue(value);
  }

  /**
   * Converts to a variable
   */
  public static Var toVar(Value value)
  {
    if (value instanceof Var)
      return (Var) value;
    else if (value == null)
      return new Var();
    else
      return new Var(value);
  }

  /**
   * Sets a vield variable
   */
  public static Value setFieldVar(Value oldValue, Value value)
  {
    if (value instanceof Var)
      return value;
    else if (oldValue instanceof Var)
      return new Var(value);
    else
      return value;
  }

  /**
   * Sets a reference
   */
  public static Value setRef(Value oldValue, Value value)
  {
    // php/3243
    if (value instanceof Var)
      return value;
    /*
    else if (oldValue instanceof Var) {
      oldValue.set(value);

      return oldValue;
    }
    */
    else
      return new Var(value);
  }

  /**
   * Returns the last value.
   */
  public static Value comma(Value a0, Value a1)
  {
    return a1;
  }

  /**
   * Returns the last value.
   */
  public static Value comma(Value a0, Value a1, Value a2)
  {
    return a2;
  }

  /**
   * Returns the last value.
   */
  public static Value comma(Value a0, Value a1, Value a2, Value a3)
  {
    return a3;
  }

  /**
   * Returns the last value.
   */
  public static Value comma(Value a0, Value a1, Value a2, Value a3, Value a4)
  {
    return a4;
  }

  public String toString()
  {
    return "Env[]";
  }

  /**
   * Returns ifNull if condition.isNull(), otherwise returns ifNotNull.
   */
  public Value ifNull(Value condition, Value ifNull, Value ifNotNull)
  {
    return condition.isNull() ? ifNull : ifNotNull;
  }

  /**
   * Returns the locale info.
   */
  public LocaleInfo getLocaleInfo()
  {
    if (_locale == null)
      _locale = new LocaleInfo();

    return _locale;
  }

  /**
   * Registers a shutdown function.
   */
  public void addShutdown(Callback callback, Value []args)
  {
    _shutdownList.add(new Shutdown(callback, args));
  }

  /**
   * Called when the Env is no longer needed.
   */
  public void close()
  {
    while (_outputBuffer != null)
      popOutputBuffer();

    try {
      for (int i = 0; i < _shutdownList.size(); i++)
        _shutdownList.get(i).call(this);
    }
    catch (Throwable e) {
      log.log(Level.FINE, e.toString(), e);
    }

    sessionWriteClose();

    ArrayList<SoftReference<Closeable>> closeList;
    closeList = new ArrayList<SoftReference<Closeable>>(_closeList);
    
    for (SoftReference<Closeable> ref : closeList) {
      try {
        Closeable close = ref.get();

        if (close != null)
          close.close();
      }
      catch (Throwable e) {
        log.log(Level.FINER, e.toString(), e);
      }
    }

    for (int i = 0; _removePaths != null && i < _removePaths.size(); i++) {
      Path path = _removePaths.get(i);

      try {
        path.remove();
      }
      catch (IOException e) {
        log.log(Level.FINER, e.toString(), e);
      }
    }
  }

  public void sessionWriteClose()
  {
    SessionArrayValue session = _session;

    _session = null;

    if (session != null) {
      SessionCallback callback = getSessionCallback();

      if (callback != null) {
        String value;
        
        // php/1k6e
        if (session.getSize() > 0)
          value = VariableModule.serialize(session.getArray());
        else
          value = "";

        callback.write(this, session.getId(), value);

        callback.close(this);
      }
      else {
        _quercus.saveSession(this, session);

        setGlobalValue("_SESSION", session.copy(this));
        setGlobalValue("HTTP_SESSION_VARS", session.copy(this));
      }
    }
  }

  static class ClassKey {
    private final WeakReference<ClassDef> _defRef;
    private final WeakReference<QuercusClass> _parentRef;

    ClassKey(ClassDef def, QuercusClass parent)
    {
      _defRef = new WeakReference<ClassDef>(def);

      if (parent != null)
	_parentRef = new WeakReference<QuercusClass>(parent);
      else
	_parentRef = null;
    }

    public int hashCode()
    {
      int hash = 37;

      ClassDef def = _defRef.get();
      
      QuercusClass parent = null;
      if (_parentRef != null)
	parent = _parentRef.get();

      if (def != null)
	hash = 65521 * hash + def.hashCode();

      if (parent != null)
	hash = 65521 * hash + parent.hashCode();

      return hash;
    }

    public boolean equals(Object o)
    {
      ClassKey key = (ClassKey) o;

      ClassDef aDef = _defRef.get();
      ClassDef bDef = key._defRef.get();

      if (aDef != bDef)
	return false;

      if (_parentRef == key._parentRef)
	return true;
      
      else if (_parentRef != null && key._parentRef != null)
	return _parentRef.get() == key._parentRef.get();

      else
	return false;
    }
  }

  static class ConnectionEntry {
    private DataSource _ds;
    private String _user;
    private String _password;
    private Connection _conn;

    public void init(DataSource ds, String user, String password)
    {
      _ds = ds;
      _user = user;
      _password = password;
    }

    public void setConnection(Connection conn)
    {
      _conn = conn;
    }

    public Connection getConnection()
    {
      return _conn;
    }

    public int hashCode()
    {
      int hash = _ds.hashCode();
      
      if (_user == null)
	return hash;
      else
	return 65521 * hash + _user.hashCode();
    }

    public boolean equals(Object o)
    {
      if (! (o instanceof ConnectionEntry))
	return false;

      ConnectionEntry entry = (ConnectionEntry) o;

      if (_ds != entry._ds)
	return false;
      else if (_user == null)
	return entry._user == null;
      else
	return _user.equals(entry._user);
    }
  }

  static {
    SPECIAL_VARS.put("GLOBALS", _GLOBAL);
    SPECIAL_VARS.put("_SERVER", _SERVER);
    SPECIAL_VARS.put("_GET", _GET);
    SPECIAL_VARS.put("_POST", _POST);
    SPECIAL_VARS.put("_FILES", _FILES);
    SPECIAL_VARS.put("_REQUEST", _REQUEST);
    SPECIAL_VARS.put("_COOKIE", _COOKIE);
    SPECIAL_VARS.put("_SESSION", _SESSION);
    SPECIAL_VARS.put("_ENV", _ENV);
    SPECIAL_VARS.put("HTTP_GET_VARS", HTTP_GET_VARS);
    SPECIAL_VARS.put("HTTP_POST_VARS", HTTP_POST_VARS);
    SPECIAL_VARS.put("HTTP_POST_FILES", HTTP_POST_FILES);
    SPECIAL_VARS.put("HTTP_COOKIE_VARS", HTTP_COOKIE_VARS);
    SPECIAL_VARS.put("HTTP_SERVER_VARS", HTTP_SERVER_VARS);
    SPECIAL_VARS.put("PHP_SELF", PHP_SELF);
  }
}

