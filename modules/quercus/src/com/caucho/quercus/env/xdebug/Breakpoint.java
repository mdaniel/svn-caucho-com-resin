package com.caucho.quercus.env.xdebug;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;

public class Breakpoint
{
	private XdebugConnection _conn;

	private final int _id;

	private String _fileName;

	private String _lineNumber;
	
	public Breakpoint(XdebugConnection connection, String type, String state,
	    String filename, String lineno, String function, String exception,
	    String hitValue, String hitCondition, String isTemporary,
	    String expression) {
		_conn = connection;
		_id = _conn.addBreakpoint(this);
		try {
	    _fileName = new File(new URI(filename)).getAbsolutePath();
    } catch (URISyntaxException e) {
	    throw new RuntimeException("could not parse file URI", e);
    }
		_lineNumber = lineno;
  }
	
	public int getId() {
		return _id;
  }

	public String getFileAndLineNumber() {
	  return _fileName + ":" + _lineNumber;
  }
}
