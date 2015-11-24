package com.caucho.quercus.env.xdebug;

import java.io.File;

public class Breakpoint
{
	private XdebugConnection _conn;

	private final int _id;

	private String _fileAndLineNumber;
	
	public Breakpoint(XdebugConnection connection, String type, String state,
	    String filename, String lineno, String function, String exception,
	    String hitValue, String hitCondition, String isTemporary,
	    String expression) {
		_conn = connection;
        String fileName = new File(filename.replace("file://", "").replace("file:/",  "")).getAbsolutePath();
		_fileAndLineNumber = (fileName + ":" + lineno).toLowerCase();
		_id = _conn.addBreakpoint(this);
  }
	
	public int getId() {
		return _id;
  }

	public String getFileAndLineNumber() {
	  return _fileAndLineNumber;
  }
}
