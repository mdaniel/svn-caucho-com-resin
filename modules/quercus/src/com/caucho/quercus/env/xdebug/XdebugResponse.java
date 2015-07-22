package com.caucho.quercus.env.xdebug;

import com.caucho.quercus.env.xdebug.XdebugConnection.State;

public class XdebugResponse
{
	
	public XdebugResponse(State nextState, String responseToSend, String transactionId) {
	  super();
	  this.nextState = nextState;
	  this.responseToSend = responseToSend;
	  this.transactionId = transactionId;
  }
	public XdebugConnection.State nextState;
	public String responseToSend;
	public String transactionId;
}
