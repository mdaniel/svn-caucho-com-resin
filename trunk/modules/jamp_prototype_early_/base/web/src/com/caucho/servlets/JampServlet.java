package com.caucho.servlets;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.caucho.amp.AmpFactory;
import com.caucho.amp.Message;
import com.caucho.amp.SkeletonServiceInvoker;
import com.caucho.websocket.WebSocketListener;
import com.caucho.websocket.WebSocketServletRequest;

/**
 * Servlet implementation class JampServlet
 */
@WebServlet("/JampServlet")
public class JampServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private SkeletonServiceInvoker serviceInvoker;

    /**
     * Default constructor. 
     */
    public JampServlet() {
        serviceInvoker = AmpFactory.factory().createJampServerSkeleton(EmployeeServiceImpl.class);
    }


	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
	    
	    String protocol = request.getHeader("Sec-WebSocket-Protocol");
	    
	    System.out.println("___________ SERVICE MEHTOD ________________ " + protocol);

	    WebSocketListener listener;

	    if ("employee-add-protocol".equals(protocol)) {
	        
	      System.out.println("___________ WEBSOCKET ________________ " + protocol);

	      listener = new JampWebSocketListener();
	      response.setHeader("Sec-WebSocket-Protocol", "employee-add-protocol");
	      WebSocketServletRequest wsRequest = (WebSocketServletRequest) request;
	      wsRequest.startWebSocket(listener);

	    }
	    else {
	        if (request.getMethod()=="POST") {
    	        try {
    	            Message message = serviceInvoker.invokeMessage(request.getReader());
    	            
    	            response.getWriter().print(message.toString());
    	        }catch (Exception ex) {
    	            throw new ServletException(ex);
    	        }
	        } else {

	            response.sendError(404);
	            return;
	        }
	    }
	    
	    
	}

}
