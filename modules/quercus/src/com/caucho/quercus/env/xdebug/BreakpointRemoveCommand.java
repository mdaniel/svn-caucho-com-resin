package com.caucho.quercus.env.xdebug;

import java.util.Map;

public class BreakpointRemoveCommand extends XdebugCommand
{
	public static final String PARAM_ID = "-d";
	
	@Override
  protected XdebugResponse getInternalResponse(String commandName, Map<String, String> parameters,
      String transactionId, XdebugConnection conn) {
		String id = parameters.get(PARAM_ID);

		conn.removeBreakpoint(id);
		String response = "<response xmlns=\"urn:debugger_protocol_v1\" xmlns:xdebug=\"http://xdebug.org/dbgp/xdebug\" command=\"breakpoint_remove\" transaction_id=\"" + transactionId 
				+ "\"></response>";
	  return new XdebugResponse(null, response, transactionId);
  }

}
