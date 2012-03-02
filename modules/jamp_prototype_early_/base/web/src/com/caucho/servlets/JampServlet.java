package com.caucho.servlets;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.caucho.amp.Message;
import com.caucho.amp.ServiceInvoker;
import com.caucho.encoder.JampMessageReaderDecoder;

/**
 * Servlet implementation class JampServlet
 */
@WebServlet("/JampServlet")
public class JampServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private ServiceInvoker serviceInvoker;

    /**
     * Default constructor. 
     */
    public JampServlet() {
        serviceInvoker = new ServiceInvoker(EmployeeServiceImpl.class);
        serviceInvoker.setMessageDecoder(new JampMessageReaderDecoder());
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {
            Message message = serviceInvoker.invokeMessage(request.getReader());
            
            response.getWriter().print(message.toString());
        }catch (Exception ex) {
            throw new ServletException(ex);
        }
	}

}
