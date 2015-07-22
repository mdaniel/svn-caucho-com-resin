package com.caucho.quercus.env.xdebug;

import java.util.Map;

import com.caucho.quercus.env.xdebug.XdebugConnection.State;

public class StepIntoCommand extends XdebugCommand
{
	
	@Override
  protected XdebugResponse getInternalResponse(String commandName, Map<String, String> parameters,
      String transactionId, XdebugConnection conn) {
	  return new XdebugResponse(State.BREAK, null, transactionId);
  }

}
