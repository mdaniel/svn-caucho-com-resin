package com.caucho.quercus.env.xdebug;

import java.util.Map;

public class StatusCommand extends XdebugCommand
{
	
	@Override
  protected XdebugResponse getInternalResponse(String commandName, Map<String, String> parameters,
      String transactionId, XdebugConnection conn) {
		return new XdebugStatusResponse("status", conn.getState(), transactionId);
  }

}
