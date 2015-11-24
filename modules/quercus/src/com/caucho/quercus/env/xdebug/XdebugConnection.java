package com.caucho.quercus.env.xdebug;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.quercus.Location;
import com.caucho.quercus.QuercusContext;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.EnvVar;
import com.caucho.quercus.env.StringValue;
import com.caucho.quercus.env.Value;
import com.caucho.quercus.program.QuercusProgram;

/**
 * This class is used to implement DBGP (http://xdebug.org/docs-dbgp.php)
 * 
 * @author Immanuel Scheerer, apparent media
 */
public class XdebugConnection
{
  private static final Logger log = Logger.getLogger(XdebugConnection.class
      .getName());
  private static XdebugConnection INSTANCE;
  private final static String XML_DECLARATION = "<?xml version=\"1.0\" encoding=\"iso-8859-1\"?>\n";

  public static enum State {
    STARTING("starting"), RUNNING("running"), BREAK("break"), STOPPED("stopped"), STOPPING(
        "stopping");
    private String name;

    private State(String name) {
      this.name = name;
    }

    @Override
    public String toString() {
      return name;
    }
  };

  private Map<String, XdebugCommand> commandsMap;
  private Socket _socket;
  private Env _env;
  private State _state = State.STARTING;
  private String _lastCommand;
  private XdebugResponse _lastStateChangingResponse;
  private int nextBreakpointId = 1000000;
  private Map<Integer, Breakpoint> breakpointIdsMapping = new HashMap<Integer, Breakpoint>();
  private Map<String, Breakpoint> breakpointFileAndLineNumberMapping = new HashMap<String, Breakpoint>();
  private Location _locationToSkip;
  private Location _currentLocation;
  /**
   * If this flag is set, debugger does not react to changes on stack (which are
   * necessary for evaluation values)
   */
  private boolean _isEvaluating;
  private Integer _breakAtExpectedStackDepth;
  private Map<StringValue, EnvVar>[] _envStack;

  private XdebugConnection(Env env) {
    _env = env;
    if (!isXdebugSessionActivated()) {
      return;
    }
    commandsMap = new HashMap<String, XdebugCommand>();
    commandsMap.put("feature_set", new FeatureSetCommand());
    commandsMap.put("feature_get", new FeatureGetCommand());
    commandsMap.put("status", new StatusCommand());
    commandsMap.put("stdout", new StdoutCommand("stdout"));
    commandsMap.put("stderr", new StdoutCommand("stderr"));
    commandsMap.put("step_into", new StepCommand());
    commandsMap.put("step_over", new StepCommand());
    commandsMap.put("step_out", new StepCommand());
    commandsMap.put("eval", new EvalCommand());
    commandsMap.put("property_get", new PropertyGetCommand());
    commandsMap.put("run", new RunCommand());
    commandsMap.put("stack_get", new StackGetCommand());
    commandsMap.put("context_names", new ContextNamesCommand());
    commandsMap.put("context_get", new ContextGetCommand());
    commandsMap.put("breakpoint_set", new BreakpointSetCommand());
    commandsMap.put("breakpoint_remove", new BreakpointRemoveCommand());
    commandsMap.put("stop", new StopCommand());
    try {
      _socket = new Socket("localhost", 9000);

      new Thread("xdebug reader") {
        public void run() {
          try {
            StringBuilder sb = new StringBuilder();
            Reader reader = new InputStreamReader(_socket.getInputStream());
            char nextChar;
            while ((nextChar = (char) reader.read()) != -1 && nextChar != 65535) {
              if (nextChar == 0) {
                try {
                  receivedPacket(sb.toString());
                } catch (Exception e) {
                  log.log(Level.WARNING, "caught exception while trying to interpret xdebug command", e);
                }
                sb.delete(0, sb.length());
              } else {
                if (sb.length() > 100000) {
                  System.err
                      .println("Received packet is larger than 100000 bytes -> skipping");
                } else {
                  sb.append(nextChar);
                }
              }
            }
          } catch (IOException e) {
          } finally {
            // make sure that thread is not sleeping anymore
            synchronized (XdebugConnection.this) {
              XdebugConnection.this.notifyAll();
            }
            close();
          }
        };
      }.start();
    } catch (UnknownHostException e) {
      return;
    } catch (IOException e) {
      return;
    }
  }

  public static XdebugConnection getInstance(Env env) {
    if (INSTANCE != null && INSTANCE.isConnected()) {
      return INSTANCE;
    }
    INSTANCE = new XdebugConnection(env);
    return INSTANCE;
  }

  private boolean isXdebugSessionActivated() {
    boolean isActive = false;
    if (System.getenv("XDEBUG_CONFIG") != null) {
      isActive = true;
    } else {
      Value sessionStart = eval("isset($_GET['XDEBUG_SESSION_START']) ? $_GET['XDEBUG_SESSION_START'] : null");
      if (sessionStart != null && !sessionStart.isNull()) {
        eval("$_COOKIE['XDEBUG_SESSION'] = '" + sessionStart + "'");
        isActive = true;
      } else {
        Value xdebugSession = eval("isset($_COOKIE['XDEBUG_SESSION']) ? $_COOKIE['XDEBUG_SESSION'] : null");
        if (xdebugSession != null && !xdebugSession.isNull() && xdebugSession.length() > 0) {
          isActive = true;
        }
      }
    }
    return isActive;
  }

  public void connect(Location location) {
    if (!isConnected() || !isXdebugSessionActivated()) {
      return;
    }
    _state = State.STARTING;
    _lastStateChangingResponse = null;
    _lastCommand = null;
    _isEvaluating = false;
    breakpointIdsMapping = new HashMap<Integer, Breakpoint>();
    breakpointFileAndLineNumberMapping = new HashMap<String, Breakpoint>();
    String fileuri = getFileURI(location.getFileName());
    final String initMsg = "<init xmlns=\"urn:debugger_protocol_v1\" xmlns:xdebug=\"http://xdebug.org/dbgp/xdebug\" fileuri=\""
        + fileuri
        + "\" language=\"PHP\" protocol_version=\"1.0\" appid=\"15986\" idekey=\"XDEBUG_ECLIPSE\"><engine version=\"2.2.7\"><![CDATA[Xdebug]]></engine><author><![CDATA[Derick Rethans]]></author><url><![CDATA[http://xdebug.org]]></url><copyright><![CDATA[Copyright (c) 2002-2015 by Derick Rethans]]></copyright></init>";
    try {
      sendPacket(initMsg);
    } catch (IOException e) {
      e.printStackTrace();
    }

    synchronized (this) {
      try {
        // give debugger time to prepare if he wants to break in first line
        wait(1000);
      } catch (InterruptedException e) {
      }
    }
  }

  public boolean isConnected() {
    return _socket != null && _socket.isConnected() && _state != State.STOPPED;
  }

  public void close() {
    if (isConnected()) {
      if (_state != State.STOPPED) {
        // send packet to client that debugging ended
        _state = State.STOPPED;
        if (_lastStateChangingResponse != null) {
          XdebugResponse response = new XdebugStatusResponse(_lastCommand,
              _state, _lastStateChangingResponse.transactionId);
          try {
            sendPacket(response.responseToSend);
          } catch (IOException e) {
            e.printStackTrace();
          }
        }
      }
      // make sure that thread is not sleeping anymore
      synchronized (this) {
        notifyAll();
      }

      try {
        _socket.close();
      } catch (IOException e) {
      } finally {
        _socket = null;
      }

    }
  }

  public boolean isWaitingForUpdatesFromPhp() {
    return (_state == State.BREAK || _state == State.RUNNING) && !_isEvaluating;
  }

  public Env getEnv() {
    return _env;
  }

  public synchronized State getState() {
    return _state;
  }

  public int addBreakpoint(Breakpoint breakpoint) {
    if (breakpoint.getId() != 0) {
      // this breakpoint has already been added
      return breakpoint.getId();
    }
    int newId = nextBreakpointId++;
    breakpointIdsMapping.put(newId, breakpoint);
    breakpointFileAndLineNumberMapping.put(breakpoint.getFileAndLineNumber(),
        breakpoint);
    return newId;
  }

  protected synchronized void sendPacket(String packet) throws IOException {
    if (_socket == null) {
      return;
    }
    String msg = XML_DECLARATION + packet;
    // System.out.println(msg);
    OutputStream out = _socket.getOutputStream();
    byte[] bytes = msg.getBytes();
    String length = "" + bytes.length;
    out.write(length.getBytes());
    out.write(0);
    out.write(bytes);
    out.write(0);
    out.flush();
  }

  protected synchronized void receivedPacket(String packet) throws IOException {
    // System.out.println(packet);
    String[] parts = packet.split(" ");
    XdebugCommand command = commandsMap.get(parts[0]);
    _lastCommand = parts[0];
    if (command == null) {
      close();
      throw new RuntimeException("unknown command: " + packet);
    }
    try {
      boolean wasPreviouslyEvaluating = _isEvaluating;
      _isEvaluating = true;
      XdebugResponse response = command.getResponse(this, parts);
      _isEvaluating = wasPreviouslyEvaluating;
      if (response.nextState != null) {
        _state = response.nextState;
        _lastStateChangingResponse = response;
        if (_state == State.RUNNING || _state == State.BREAK) {
          synchronized (this) {
            // wake waiting thread
            notifyAll();
          }
        }
      }
      if (response.responseToSend != null) {
        sendPacket(response.responseToSend);
      }
      if (response.nextState == State.STOPPED) {
        try {
          _socket.close();
        } catch (IOException e) {
        } finally {
          _socket = null;
        }
      }
    } catch (Exception e) {
      throw new RuntimeException(
          "Could not interpret command '" + packet + "'", e);
    }
  }

  public synchronized void notifyNewLocation(Location location) {
    if (location == null) {
      return;
    }
    _currentLocation = location;
    try {
      switch (_state) {
      case RUNNING:
        String filenameAndLineNumber = location.getFileName() + ":"
            + location.getLineNumber();
        Breakpoint breakpoint = breakpointFileAndLineNumberMapping
            .get(filenameAndLineNumber.toLowerCase());
        if (breakpoint == null) {
          break;
        }
      case BREAK:
        if (_breakAtExpectedStackDepth != null
            && _breakAtExpectedStackDepth < _env.getCallDepth()) {
          // expected stack depth is not yet reached
          break;
        }
        if (_locationToSkip != null && _currentLocation.equals(_locationToSkip)) {
          // still at the same location
          break;
        }
        _locationToSkip = null;
        String fileName = location.getFileName();
        int lineNumber = location.getLineNumber();
        String transactionId = _lastStateChangingResponse.transactionId;
        _state = State.BREAK;
        // sendPacket("<response xmlns=\"urn:debugger_protocol_v1\" xmlns:xdebug=\"http://xdebug.org/dbgp/xdebug\" command=\"step_into\" transaction_id=\""
        // + transactionId
        // + "\" status=\"break\" reason=\"ok\"><xdebug:message filename=\""
        XdebugStatusResponse response = new XdebugStatusResponse(_lastCommand,
            _state, _state, transactionId, "<xdebug:message filename=\""
                + fileName + "\" lineno=\"" + lineNumber
                // + "\"></xdebug:message></response>");
                + "\"></xdebug:message>");
        sendPacket(response.responseToSend);
        _lastCommand = null;
        _lastStateChangingResponse = null;
        try {
          if (_state != State.STOPPED) {
            wait();
          }
        } catch (InterruptedException e) {
          e.printStackTrace(System.err);
        }
        break;
      default:
      }
    } catch (IOException e) {
      e.printStackTrace(System.err);
    }
  }

  /**
   * Java file URI starts with "file:/", but xdebug protocol expects "file:///"
   * 
   * @param fileName
   * @return
   */
  public String getFileURI(String fileName) {
    String result = new File(fileName).toURI().toString();
    // replace "file:/" with "file:///"
    return "file://" + result.substring(5);
  }

  public void setBreakAtExpectedStackDepth(Integer stackDepth) {
    _breakAtExpectedStackDepth = stackDepth;
  }

  public void removeBreakpoint(String id) {
    Breakpoint breakpoint = breakpointIdsMapping.get(id);
    if (breakpoint != null) {
      breakpointFileAndLineNumberMapping.remove(breakpoint
          .getFileAndLineNumber());
    }
  }

  public Value eval(String expr) {
    boolean wasPreviouslyEvaluating = _isEvaluating;
    _isEvaluating = true;
    QuercusContext quercus = _env.getQuercus();

    QuercusProgram program;
    try {
      _env.setCurrent();
      program = quercus.parseCode((StringValue) StringValue.create(expr));
      Value value = program.createExprReturn().execute(_env);

      return value;
    } catch (IOException e) {
      e.printStackTrace(System.err);
      return null;
    } finally {
      _isEvaluating = wasPreviouslyEvaluating;
    }
  }

  @SuppressWarnings({ "rawtypes", "unchecked" })
  public void notifyPushEnv(Map<StringValue, EnvVar> map, int callStackTop) {
    if (_envStack == null) {
      _envStack = new Map[256];
    }

    if (_envStack.length <= callStackTop) {
      Map[] newStack = new Map[2 * _envStack.length];
      System.arraycopy(_envStack, 0, newStack, 0, _envStack.length);
      _envStack = newStack;
    }
    _envStack[callStackTop] = map;
  }

  public Map<StringValue, EnvVar> getVarEnvAtStackDepth(int stackDepth) {
    return _envStack[_env.getCallDepth() - stackDepth];
  }

  public void notifyPopEnv(int _callStackTop) {
    if (_envStack != null) {
      _envStack[_callStackTop + 1] = null;
    }
  }

  public Location getCurrentLocation() {
    return _currentLocation;
  }

  public void stepOver() {
    setBreakAtExpectedStackDepth(_env.getCallDepth());
    skipCurrentLocationForNextBreak();
  }

  public void stepInto() {
    setBreakAtExpectedStackDepth(null);
    skipCurrentLocationForNextBreak();
  }

  public void stepOut() {
    setBreakAtExpectedStackDepth(_env.getCallDepth() - 1);
    skipCurrentLocationForNextBreak();
  }

  public void skipCurrentLocationForNextBreak() {
    _locationToSkip = _currentLocation;
  }
}
