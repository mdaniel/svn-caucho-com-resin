package com.caucho.quercus.env.xdebug;

import java.util.Map;

import com.caucho.quercus.env.xdebug.XdebugConnection.State;

public class StopCommand extends XdebugCommand
{
	
	@Override
  protected XdebugResponse getInternalResponse(String commandName, Map<String, String> parameters,
      String transactionId, XdebugConnection conn) {
	  return new XdebugStatusResponse("stop", State.STOPPED, State.STOPPED, transactionId);
  }

}
