package com.caucho.quercus.env.xdebug;

import java.util.Map;

public class ContextGetCommand extends XdebugCommand
{
	public final static String PARAM_STACK_DEPTH = "-d";
	public final static String PARAM_CONTEXT_ID = "-c";

	@Override
  protected XdebugResponse getInternalResponse(String commandName, Map<String, String> parameters,
      String transactionId, XdebugConnection conn) {
		String stackDepth = parameters.get(PARAM_STACK_DEPTH);
		String contextId = parameters.get(PARAM_CONTEXT_ID);
		return new XdebugResponse(
		    null,
		    "<response xmlns=\"urn:debugger_protocol_v1\" xmlns:xdebug=\"http://xdebug.org/dbgp/xdebug\" command=\"context_get\" transaction_id=\""
		        + transactionId
		        + "\" context=\"" + contextId + "\"></response>",
		    transactionId);
  }

}
