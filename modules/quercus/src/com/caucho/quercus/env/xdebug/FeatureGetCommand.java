package com.caucho.quercus.env.xdebug;

import java.util.Map;

public class FeatureGetCommand extends XdebugCommand
{
	public final static String PARAM_FEATURE_NAME = "-n";
	
	@Override
  protected XdebugResponse getInternalResponse(String commandName, Map<String, String> parameters,
      String transactionId, XdebugConnection conn) {
		String featureName = parameters.get(PARAM_FEATURE_NAME);
	  return new XdebugResponse(null, "<response command=\"feature_get\" feature_name=\"" + featureName + "\" supported=\"0\" transaction_id=\"" + transactionId + "\"/>", transactionId);
  }

}
