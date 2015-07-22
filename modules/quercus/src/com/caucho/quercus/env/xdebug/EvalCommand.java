package com.caucho.quercus.env.xdebug;

import java.util.Map;

public class EvalCommand extends XdebugCommand
{
	
	@Override
  protected XdebugResponse getInternalResponse(String commandName, Map<String, String> parameters,
      String transactionId, XdebugConnection conn) {
		String data = getBase64DecodedData(parameters);
		System.out.println("Requested eval expression: " + data);
		Object value = conn.eval(data);

		String type = null;
		String size = null;
		String serializedValue = "";
		if (value instanceof Boolean) {
			type = "bool";
			serializedValue = ((Boolean) value) ? "1" : "0";
		} else if (value instanceof String) {
			type = "string";
			size = "" + ((String) value).length();
			serializedValue = javax.xml.bind.DatatypeConverter.printBase64Binary(((String) value).getBytes());
		} else if (value == null) {
			type ="null";
			serializedValue = "";
		} else {
			System.err.println("unknown type: " + value.getClass());
		}
		String response = "<response xmlns=\"urn:debugger_protocol_v1\" xmlns:xdebug=\"http://xdebug.org/dbgp/xdebug\" command=\"eval\" transaction_id=\""
				+ transactionId + "\"><property address=\"140734717470128\" type=\"" + type + "\"" + (size == null ? "" : " size=\"" + size + "\" encoding=\"base64\"") + ">"
				+ "<![CDATA[" + serializedValue + "]]></property></response>";
		
	  return new XdebugResponse(null, response, transactionId);
  }
}
