package com.caucho.quercus.env.xdebug;

import java.util.Map;

public class FeatureSetCommand extends XdebugCommand
{
	public final static String PARAM_FEATURE_NAME = "-n";
	public final static String PARAM_VALUE = "-v";
	
	@Override
  protected XdebugResponse getInternalResponse(String commandName, Map<String, String> parameters,
      String transactionId, XdebugConnection conn) {
		String featureName = parameters.get(PARAM_FEATURE_NAME);
		String value = parameters.get(PARAM_VALUE);
	  return new XdebugResponse(null, "<response command=\"feature_set\" feature=\"" + featureName + "\" success=\"0\" transaction_id=\"" + transactionId + "\"/>", transactionId);
  }

}
