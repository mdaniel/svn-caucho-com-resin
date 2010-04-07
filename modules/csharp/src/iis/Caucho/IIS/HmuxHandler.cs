/*
 * Copyright (c) 1998-2010 Caucho Technology -- all rights reserved
 *
 * This file is part of Resin(R) Open Source
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Resin Open Source is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * Resin Open Source is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, or any warranty
 * of NON-INFRINGEMENT.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Resin Open Source; if not, write to the
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Alex Rojkov
 */

using System;
using System.Collections.Generic;
using System.Text;
using System.Web;
using System.Net.Sockets;
using System.Diagnostics;

namespace Caucho.IIS
{
  public class HmuxHandler : IHttpHandler
  {
    public bool IsReusable
    {
      get { return true; }
    }

    public void ProcessRequest(HttpContext context)
    {
      String path = context.Request.Path;
      if ("/__caucho__test__basic".Equals(path)) {
        DoTestBasic(context);
      } else if ("/__caucho__test__chunked".Equals(path)) {
        DoTestChunked(context);
      } else if ("/__caucho__test__ssl".Equals(path)) {
        DoTestSSL(context);
      } else {
        DoHmux(context);
      }
    }

    public void DoHmux(HttpContext context)
    {
      Socket hmuxSocket = new Socket(AddressFamily.InterNetwork, SocketType.Stream, ProtocolType.Tcp);
      hmuxSocket.Connect("localhost", 6810);
      HmuxChannel channel = new HmuxChannel(hmuxSocket);
      channel.StartChannel();
      channel.RelayUri(context.Request.RawUrl);
      channel.RelayHttpMethod(context.Request.HttpMethod);
      channel.RelayHeaders(context.Request.Headers);
      channel.RelayServerVariables(context.Request);
      channel.RelayRequestBody(context.Request);
      channel.MarkQuit();
      //channel.WriteExit();
      channel.FlushBuffer();

      channel.DoResponse(context);

      //byte []buffer = new byte[1024];
      //int x = channel.Read(buffer);
      //context.Response.OutputStream.Write(buffer, 0, x);

      /**      hmuxSocket.FlushBuffer(
            cse_write_byte(hmuxSocket, HMUX_CHANNEL);
            cse_write_byte(hmuxSocket, channel >> 8);
            cse_write_byte(hmuxSocket, channel);

            cse_write_string(hmuxSocket, HMUX_URL, uri);
            write_var(hmuxSocket, r, "REQUEST_METHOD", HMUX_METHOD);
            write_var(hmuxSocket, r, "SERVER_PROTOCOL", CSE_PROTOCOL);
            //	write_var(hmuxSocket, r, "PATH_TRANSLATED", CSE_PATH_TRANSLATED);
            write_var(hmuxSocket, r, "QUERY_STRING", CSE_QUERY_STRING);
            write_var(hmuxSocket, r, "SERVER_NAME", HMUX_SERVER_NAME);
            write_var(hmuxSocket, r, "SERVER_PORT", CSE_SERVER_PORT);
            write_var(hmuxSocket, r, "REMOTE_HOST", CSE_REMOTE_HOST);
            write_var(hmuxSocket, r, "REMOTE_ADDR", CSE_REMOTE_ADDR);
            write_var(hmuxSocket, r, "REMOTE_USER", CSE_REMOTE_USER);
            write_var(hmuxSocket, r, "AUTH_TYPE", CSE_AUTH_TYPE);

            cse_write_string(hmuxSocket, CSE_SERVER_TYPE, "ISAPI");
      */
    }

    public void DoTestBasic(HttpContext context)
    {
      context.Response.Write("Basic");
    }

    public void DoTestChunked(HttpContext context)
    {
      for (int i = 0; i < 10; i++) {
        context.Response.Write("chunk:" + i);
        context.Response.Flush();
      }
    }

    public void DoTestSSL(HttpContext context)
    {
      HttpClientCertificate certificate = context.Request.ClientCertificate;
      context.Response.Output.WriteLine("issuer: " + certificate.Issuer);
      context.Response.Output.WriteLine("server-issuer: " + certificate.ServerIssuer);
      context.Response.Output.WriteLine("server-subject" + certificate.ServerSubject);
      context.Response.Output.WriteLine("valid-from: " + certificate.ValidFrom);
      context.Response.Output.WriteLine("valid-until: " + certificate.ValidUntil);
      foreach (String key in certificate.AllKeys) {
        context.Response.Output.Write(key + ": ");
        if (certificate.GetValues(key) != null)
          foreach (String value in certificate.GetValues(key))
            context.Response.Output.Write(value + ", ");
        context.Response.Output.WriteLine();
      }
      context.Response.Output.WriteLine("is-valid: " + certificate.IsValid);
      context.Response.Output.WriteLine("is-present: " + certificate.IsPresent);
      context.Response.Output.WriteLine("cert-cookie: " + certificate.Cookie);
      context.Response.Output.WriteLine("binary-cert: " + certificate.Certificate.Length);
      context.Response.Output.WriteLine("cert-encoding: " + certificate.CertEncoding);
    }
  }
}