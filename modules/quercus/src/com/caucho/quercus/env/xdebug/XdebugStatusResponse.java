package com.caucho.quercus.env.xdebug;

import com.caucho.quercus.env.xdebug.XdebugConnection.State;

public class XdebugStatusResponse extends XdebugResponse
{

	public XdebugStatusResponse(String command, State currentState,
	    String transactionId) {
		this(command, currentState, null, transactionId);
	}

	public XdebugStatusResponse(String command, State currentState, State newState,
	    String transactionId) {
		super(newState, "<response command=\"" + command + "\" status=\"" + currentState + "\" reason=\"ok\" transaction_id=\"" + transactionId + "\"></response>", transactionId);
	}

}
