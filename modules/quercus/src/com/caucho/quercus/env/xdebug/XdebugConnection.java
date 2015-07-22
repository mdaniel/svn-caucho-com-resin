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

import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.Value;
import com.caucho.quercus.expr.Expr;

/**
 * This class is used to implement DBGP (http://xdebug.org/docs-dbgp.php)
 * 
 * @author Immanuel Scheerer, apparent media
 */
public class XdebugConnection
{
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
	private XdebugResponse _lastStateChangingResponse;
	private int nextBreakpointId = 1000000;
	private Map<Integer, Breakpoint> breakpointIdsMapping = new HashMap<Integer, Breakpoint>();
	private Map<String, Breakpoint> breakpointFileAndLineNumberMapping = new HashMap<String, Breakpoint>();

	private XdebugConnection() {
		commandsMap = new HashMap<String, XdebugCommand>();
		commandsMap.put("feature_set", new FeatureSetCommand());
		commandsMap.put("status", new StatusCommand());
		commandsMap.put("step_into", new StepIntoCommand());
		commandsMap.put("eval", new EvalCommand());
		commandsMap.put("run", new RunCommand());
		commandsMap.put("stack_get", new StackGetCommand());
		commandsMap.put("context_names", new ContextNamesCommand());
		commandsMap.put("context_get", new ContextGetCommand());
		commandsMap.put("breakpoint_set", new BreakpointSetCommand());
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
								receivedPacket(sb.toString());
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
						System.out.println("xdebug connection has been closed");
						synchronized (XdebugConnection.this) {
							_state = XdebugConnection.State.STOPPED;
						}
						if (_socket != null) {
							try {
								_socket.close();
							} catch (IOException e1) {
							}
							_socket = null;
						}
					}
				};
			}.start();
		} catch (UnknownHostException e) {
			return;
		} catch (IOException e) {
			return;
		}
	}

	public static XdebugConnection getInstance() {
		if (INSTANCE != null && INSTANCE.isConnected()) {
			return INSTANCE;
		}
		INSTANCE = new XdebugConnection();
		return INSTANCE;
	}

	public synchronized void connect(Env env, Expr call) {
		if (!isConnected()) {
			return;
		}
		_env = env;
		_state = State.STARTING;
		_lastStateChangingResponse = null;
		breakpointIdsMapping = new HashMap<Integer, Breakpoint>();
		breakpointFileAndLineNumberMapping = new HashMap<String, Breakpoint>();
		String fileuri = getFileURI(call.getFileName());
		String initMsg = "<init xmlns=\"urn:debugger_protocol_v1\" xmlns:xdebug=\"http://xdebug.org/dbgp/xdebug\" fileuri=\""
		    + fileuri
		    + "\" language=\"PHP\" protocol_version=\"1.0\" appid=\"15986\" idekey=\"XDEBUG_ECLIPSE\"><engine version=\"2.2.7\"><![CDATA[Xdebug]]></engine><author><![CDATA[Derick Rethans]]></author><url><![CDATA[http://xdebug.org]]></url><copyright><![CDATA[Copyright (c) 2002-2015 by Derick Rethans]]></copyright></init>";
		try {
			sendPacket(initMsg);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public boolean isConnected() {
		return _socket != null && _socket.isConnected() && _state != State.STOPPED;
	}

	public synchronized void close() {
		if (isConnected()) {
			if (_state != State.STOPPED) {
				// send packet to client that debugging ended
				_state = State.STOPPED;
				XdebugResponse response = new XdebugStatusResponse("run", _state,
				    _lastStateChangingResponse.transactionId);
				try {
					sendPacket(response.responseToSend);
				} catch (IOException e) {
					e.printStackTrace();
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
		return _state == State.BREAK;
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
		String msg = XML_DECLARATION + packet;
		System.out.println(msg);
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
		System.out.println(packet);
		String[] parts = packet.split(" ");
		XdebugCommand command = commandsMap.get(parts[0]);
		if (command == null) {
			close();
			throw new RuntimeException("unknown command: " + packet);
		}
		try {
			XdebugResponse response = command.getResponse(this, parts);
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

	public synchronized void notifyPushCall(Expr call, Value obj, Value[] args) {
		try {
			switch (_state) {
			case BREAK:
				String fileuri = getFileURI(call.getFileName());
				int lineNumber = call.getLine();
				String transactionId = _lastStateChangingResponse.transactionId;
				_state = State.BREAK;
				sendPacket("<response xmlns=\"urn:debugger_protocol_v1\" xmlns:xdebug=\"http://xdebug.org/dbgp/xdebug\" command=\"step_into\" transaction_id=\""
				    + transactionId
				    + "\" status=\"break\" reason=\"ok\"><xdebug:message filename=\""
				    + fileuri
				    + "\" lineno=\""
				    + lineNumber
				    + "\"></xdebug:message></response>");
				synchronized (this) {
					try {
						wait();
					} catch (InterruptedException e) {
						e.printStackTrace(System.err);
					}
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
}
