//package com.caucho.resin.admin.service;
//
//import java.io.IOException;
//
//import javax.servlet.ServletException;
//import javax.servlet.ServletRequest;
//import javax.servlet.ServletResponse;
//import javax.servlet.http.HttpServletRequest;
//import javax.servlet.http.HttpServletResponse;
//
//import com.caucho.amp.jamp.JampServlet;
//
//public class AdminServlet extends JampServlet
//{
//  @Override
//  public void service(ServletRequest request, ServletResponse response)
//    throws IOException, ServletException
//  {
//    HttpServletRequest req = (HttpServletRequest) request;
//    HttpServletResponse res = (HttpServletResponse) response;
//
//    String pathInfo = req.getPathInfo();
//
//    System.err.println(getClass().getSimpleName() + ".service0: " + pathInfo);
//
//    super.service(request, response);
//  }
//}
