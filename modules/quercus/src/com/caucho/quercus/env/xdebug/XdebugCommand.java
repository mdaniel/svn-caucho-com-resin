package com.caucho.quercus.env.xdebug;

import java.util.HashMap;
import java.util.Map;

public abstract class XdebugCommand
{
	public final static String PARAM_TRANSACTION_ID = "-i";
	public final static String PARAM_DATA = "--";
	
	public XdebugResponse getResponse(XdebugConnection conn, String[] requestParts) {
		if (requestParts.length  < 1) {
			throw new RuntimeException("invalid command (no command name given)");
		}
		String commandName = requestParts[0];
		Map<String, String> parameters = new HashMap<String, String>();
		String currentKey = null;
		
		for (int i = 1;  i < requestParts.length; i++) {
			String currentPart = requestParts[i];
			if (currentKey != null) {
				parameters.put(currentKey, currentPart);
				currentKey = null;
			} else if (currentPart.startsWith("-")){
				currentKey = currentPart;
			} else {
				throw new RuntimeException("Did not expect value without leading dash '-'");
			}
		}
		String transactionId = parameters.get(PARAM_TRANSACTION_ID);
		return getInternalResponse(commandName, parameters, transactionId, conn);
	}
	
	protected String getBase64DecodedData(Map<String, String> parameters) {
		String base64String = parameters.get(PARAM_DATA);
		if (base64String == null) {
			return null;
		} else {
			return new String(javax.xml.bind.DatatypeConverter.parseBase64Binary(base64String));
		}
	}
	
	protected abstract XdebugResponse getInternalResponse(String commandName, Map<String, String> parameters, String transactionId, XdebugConnection conn);
}
