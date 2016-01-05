package com.caucho.quercus.env.xdebug;

import java.util.Map;

import com.caucho.quercus.env.xdebug.XdebugConnection.State;

public class RunCommand extends XdebugCommand
{
	
	@Override
  protected XdebugResponse getInternalResponse(String commandName, Map<String, String> parameters,
      String transactionId, XdebugConnection conn) {
	    conn.skipCurrentLocationForNextBreak();
		return new XdebugResponse(State.RUNNING, null, transactionId);
  }

}
