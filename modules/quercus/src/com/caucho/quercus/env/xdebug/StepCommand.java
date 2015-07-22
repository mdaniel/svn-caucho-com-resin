package com.caucho.quercus.env.xdebug;

import java.util.Map;

import com.caucho.quercus.env.xdebug.XdebugConnection.State;

public class StepCommand extends XdebugCommand
{
	
	@Override
  protected XdebugResponse getInternalResponse(String commandName, Map<String, String> parameters,
      String transactionId, XdebugConnection conn) {
		if ("step_over".equals(commandName)) {
			conn.setBreakAtExpectedStackDepth(conn.getEnv().getCallDepth());
		} else if ("step_into".equals(commandName)) {
			conn.setBreakAtExpectedStackDepth(null);
		} else if ("step_out".equals(commandName)) {
			conn.setBreakAtExpectedStackDepth(conn.getEnv().getCallDepth() - 1);
		}
	  return new XdebugResponse(State.BREAK, null, transactionId);
  }

}
