package com.caucho.quercus.env.xdebug;

import java.util.Map;

public class StdoutCommand extends XdebugCommand
{
	public final static String PARAM_COPY = "-c";
  private String command;
	
	public StdoutCommand(String command) {
	  this.command = command;
    }
	@Override
  protected XdebugResponse getInternalResponse(String commandName, Map<String, String> parameters,
      String transactionId, XdebugConnection conn) {
		String copy = parameters.get(PARAM_COPY);
	  return new XdebugResponse(null, "<response command=\"" + command + "\" success=\"0\" transaction_id=\"" + transactionId + "\"/>", transactionId);
  }

}
