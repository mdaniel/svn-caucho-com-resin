package com.caucho.quercus.env.xdebug;

import java.util.Map;

public class BreakpointSetCommand extends XdebugCommand
{
	public static final String PARAM_TYPE = "-t";
	public static final String PARAM_STATE = "-s";
	public static final String PARAM_FILENAME = "-f";
	public static final String PARAM_LINENO = "-n";
	public static final String PARAM_FUNCTION = "-m";
	public static final String PARAM_EXCEPTION = "-x";
	public static final String PARAM_HIT_VALUE = "-h";
	public static final String PARAM_HIT_CONDITION = "-o";
	public static final String PARAM_IS_TEMPORARY = "-r";
	
	@Override
  protected XdebugResponse getInternalResponse(String commandName, Map<String, String> parameters,
      String transactionId, XdebugConnection conn) {
		String type = parameters.get(PARAM_TYPE);
		String state = parameters.get(PARAM_STATE);
		String filename = parameters.get(PARAM_FILENAME);
		String lineno = parameters.get(PARAM_LINENO);
		String function = parameters.get(PARAM_FUNCTION);
		String exception = parameters.get(PARAM_EXCEPTION);
		String hitValue = parameters.get(PARAM_HIT_VALUE);
		String hitCondition = parameters.get(PARAM_HIT_CONDITION);
		String isTemporary = parameters.get(PARAM_IS_TEMPORARY);
		String expression = parameters.get(PARAM_DATA);
		
		Breakpoint breakpoint = new Breakpoint(conn, type, state, filename, lineno, function, exception, hitValue, hitCondition, isTemporary, expression);
		String response = "<response xmlns=\"urn:debugger_protocol_v1\" xmlns:xdebug=\"http://xdebug.org/dbgp/xdebug\" command=\"breakpoint_set\" transaction_id=\"" + transactionId 
				+ "\" id=\"" + breakpoint.getId() + "\"></response>";
	  return new XdebugResponse(null, response, transactionId);
  }

}
