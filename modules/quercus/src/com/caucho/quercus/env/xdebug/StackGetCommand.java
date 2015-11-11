package com.caucho.quercus.env.xdebug;

import java.util.List;
import java.util.Map;

import com.caucho.quercus.Location;
import com.caucho.quercus.env.QuercusClass;
import com.caucho.quercus.function.AbstractFunction;

public class StackGetCommand extends XdebugCommand
{
	
	@Override
  protected XdebugResponse getInternalResponse(String commandName, Map<String, String> parameters,
      String transactionId, XdebugConnection conn) {
		StringBuilder response = new StringBuilder();
		response.append("<response xmlns=\"urn:debugger_protocol_v1\" xmlns:xdebug=\"http://xdebug.org/dbgp/xdebug\" command=\"stack_get\" transaction_id=\"" + transactionId + "\">");
		int level = 0;
		List<Location> locations = conn.getEnv().getStackTraceAsLocations();
		locations.add(0, conn.getCurrentLocation());
		for (Location location : locations) {
			if (location != null) {
			    String where = "{main}";
			    if (location.getClassName() != null) {
			      where = location.getClassName();
			      if (location.getFunctionName() != null) {
			        QuercusClass quercusClass = conn.getEnv().getClass(location.getClassName());
			        if (quercusClass != null && !location.getFunctionName().startsWith("__quercus")) {
			          try {
			            AbstractFunction function = quercusClass.getFunction(conn.getEnv().createString(location.getFunctionName()));
			            if (function.isStatic()) {
			              where += "::";
			            } else {
			              where += "->";
			            }
			          } catch (Exception e) {
			            where += "->";
			          }
			        } else {
			          where += "->";
			        }
			        where += location.getFunctionName();
			      }
			    }
				response.append("<stack where=\"").append(where).append("\" level=\"");
				response.append(level);
				response.append("\" type=\"file\" filename=\"");
				response.append(location.getFileName() == null ? "" : conn.getFileURI(location.getFileName()));
				response.append("\" lineno=\"");
				response.append(location.getLineNumber());
				response.append("\"></stack>"); 
			}
			level++;
		}
		response.append("</response>");
	  return new XdebugResponse(null, response.toString(), transactionId);
  }

}
