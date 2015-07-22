package com.caucho.quercus.env.xdebug;

import com.caucho.quercus.env.xdebug.XdebugConnection.State;

public class XdebugStatusResponse extends XdebugResponse
{

	public XdebugStatusResponse(String command, State currentState,
	    String transactionId) {
		this(command, currentState, null, transactionId, "");
	}

	public XdebugStatusResponse(String command, State currentState, State newState,
	    String transactionId) {
		this(command, currentState, newState, transactionId, "");
	}
	
	public XdebugStatusResponse(String command, State currentState, State newState,
	    String transactionId, String xmlChild) {
		super(newState, "<response xmlns=\"urn:debugger_protocol_v1\" xmlns:xdebug=\"http://xdebug.org/dbgp/xdebug\" command=\"" 
	    + command + "\" transaction_id=\"" + transactionId + "\" status=\"" + currentState + "\" reason=\"ok\">" + xmlChild + "</response>", transactionId);
	}

}
