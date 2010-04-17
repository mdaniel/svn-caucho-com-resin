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
using System.Collections.Specialized;
using System.Diagnostics;
using System.IO;
using System.Text;
using System.Web;

namespace Caucho.IIS
{
  public class HmuxHandler : IHttpHandler
  {
    private const int HTTP_STATUS_SERVICE_UNAVAIL = 503;

    private const int EXIT_MASK = 0x1;
    private const int QUIT = 0x0;
    private const int EXIT = 0x1;

    private const int STATUS_MASK = 0x6;
    private const int OK = 0x0; // request succeeded
    private const int BUSY = 0x2; // server sends busy (retry GET/POST)
    private const int FAIL = 0x4; // server failed (retry GET)

    bool _isStickySessions = true;
    LoadBalancer _loadBalancer;

    public HmuxHandler()
    {
      _loadBalancer = new LoadBalancer();
    }

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

    public String GetRequestedSessionId(HttpRequest request)
    {
      return null;
    }

    private void DoHmux(HttpContext context)
    {
      HttpRequest request = context.Request;
      HttpResponse response = context.Response;

      String sessionId;

      if (!_isStickySessions)
        sessionId = null;
      else
        sessionId = GetRequestedSessionId(request);

      TempBuffer tempBuf = TempBuffer.Allocate();
      byte[] buf = tempBuf.GetBuffer();

      try {
        Stream is_ = request.InputStream;
        int len = is_.Read(buf, 0, buf.Length);
        bool isComplete;

        if (len < 0)
          isComplete = true;
        else if (len < buf.Length) {
          int sublen = is_.Read(buf, len, 1);

          if (sublen < 0)
            isComplete = true;
          else {
            len += sublen;
            isComplete = false;
          }
        } else
          isComplete = false;

        HmuxChannelFactory client = null;
        int result = OK | EXIT;

        HmuxChannel channel = _loadBalancer.OpenServer(sessionId);

        // If everything fails, return an error
        if (channel == null) {
          response.StatusCode = HTTP_STATUS_SERVICE_UNAVAIL;

          return;
        }

        client = channel.GetPool();

        BufferedStream rs = channel.GetSocketStream();
        BufferedStream ws = channel.GetSocketStream();
        result = FAIL | EXIT;
        long requestStartTime = DateTime.Now.Ticks;

        try {
          result = HandleRequest(request, response, channel, rs, ws,
                                 buf, len, isComplete, isComplete);

          if ((result & STATUS_MASK) == OK)
            return;
          else if ((result & STATUS_MASK) == BUSY)
            client.Busy();
          else
            client.FailSocket();
        } catch (ClientDisconnectException e) {
          //log.fine(stream.getDebugId() + e.toString());
          //log.log(Level.FINER, stream.getDebugId() + e.toString(), e);

          return;
        } catch (IOException e) {
          client.FailSocket();

          //log.log(Level.WARNING, stream.getDebugId() + e.toString(), e);
        } finally {
          if ((result & EXIT_MASK) == QUIT)
            channel.Free(requestStartTime);
          else
            channel.Close();
        }

        // server/2675
        if (isComplete && (result & STATUS_MASK) == BUSY
            || "GET".Equals(request.HttpMethod)) {
          channel = _loadBalancer.OpenServer(sessionId);

          // If everything fails, return an error
          if (channel == null) {
            //if (log.isLoggable(Level.INFO))
            //log.info("load-balance failed" + (client != null ? (" for " + client.getDebugId()) : ""));

            response.StatusCode = HTTP_STATUS_SERVICE_UNAVAIL;

            return;
          }

          HmuxChannelFactory client2 = channel.GetPool();

          //if (log.isLoggable(Level.INFO))
          //log.info("load-balance failing over"
          //  + (client != null ? (" from " + client.getDebugId()) : "")
          // + " to " + client2.getDebugId());

          rs = channel.GetSocketStream();
          ws = channel.GetSocketStream();

          result = FAIL | EXIT;
          requestStartTime = DateTime.Now.Ticks;

          try {
            result = HandleRequest(request, response, channel, rs, ws,
                                   buf, len, isComplete, false);

            if ((result & STATUS_MASK) == OK)
              return;
            else if ((result & STATUS_MASK) == BUSY)
              client2.Busy();
            else
              client2.FailSocket();
          } catch (IOException e) {
            client2.FailSocket();

            //log.log(Level.FINE, e.toString(), e);
          } finally {
            if ((result & EXIT_MASK) == QUIT)
              channel.Free(requestStartTime);
            else
              channel.Close();
          }
        }

        response.StatusCode = HTTP_STATUS_SERVICE_UNAVAIL;
      } finally {
        TempBuffer.Free(tempBuf);
        tempBuf = null;
      }
    }

    private int HandleRequest(HttpRequest request,
                            HttpResponse response,
                            HmuxChannel hmuxChannel,
                            BufferedStream rs,
                            BufferedStream ws,
                            byte[] buf, int length, bool isComplete,
                            bool allowBusy)
    //throws ServletException, IOException
    {
      String traceId = hmuxChannel.GetTraceId();

      StringBuilder cb = new StringBuilder();

      bool isDebugFiner = true;//log.isLoggable(Level.FINER);

      String uri = Uri.EscapeUriString(request.RawUrl);
      Trace.TraceInformation("Hmux[{0}] >>U:uri {1}->{2}", traceId, request.RawUrl, uri);
      WriteRequestString(ws, HmuxChannel.HMUX_URI, uri, traceId);

      Trace.TraceInformation("Hmux[{0}] >>m:method {1}", traceId, request.HttpMethod);
      WriteRequestString(ws, HmuxChannel.HMUX_METHOD, request.HttpMethod, traceId);

      Trace.TraceInformation("Hmux[{0}] >>u:server type {1}", traceId, "IIS");
      WriteRequestString(ws, HmuxChannel.CSE_SERVER_TYPE, "IIS", traceId);

      NameValueCollection serverVariables = request.ServerVariables;

      String serverPort = serverVariables.Get("SERVER_PORT");
      String serverName = serverVariables.Get("SERVER_NAME") + ':' + serverPort;
      Trace.TraceInformation("Hmux[{0}] >>v:server name {1}", traceId, serverName);
      WriteRequestString(ws, HmuxChannel.HMUX_SERVER_NAME, serverName, traceId);

      Trace.TraceInformation("Hmux[{0}] >>g:server port {1}", traceId, serverPort);
      WriteRequestString(ws, HmuxChannel.CSE_SERVER_PORT, serverPort, traceId);

      String remoteAddr = serverVariables.Get("REMOTE_ADDR");
      Trace.TraceInformation("Hmux[{0}] >>i:remote address {1}", traceId, remoteAddr);
      WriteRequestString(ws, HmuxChannel.CSE_REMOTE_ADDR, remoteAddr, traceId);

      String remoteHost = serverVariables.Get("REMOTE_HOST");
      if (remoteHost == null)
        remoteHost = remoteAddr;

      Trace.TraceInformation("Hmux[{0}] >>h:remote host {1}", traceId, remoteHost);
      WriteRequestString(ws, HmuxChannel.CSE_REMOTE_HOST, remoteHost, traceId);

      String protocol = serverVariables.Get("HTTP_VERSION");
      Trace.TraceInformation("Hmux[{0}] >>c:protocol {1}", traceId, protocol);
      WriteRequestString(ws, HmuxChannel.CSE_PROTOCOL, protocol, traceId);

      HttpClientCertificate clientCertificate = request.ClientCertificate;
      if (request.IsSecureConnection) {
        Trace.TraceInformation("Hmux[{0}] >>r:secure", traceId);
        WriteRequestString(ws, HmuxChannel.CSE_IS_SECURE, "", traceId);

        WriteRequestHeader(ws, "HTTPS", "on", traceId);
        WriteRequestHeader(ws, "SSL_SECRETKEYSIZE", clientCertificate.KeySize.ToString(), traceId);
      }

      if (clientCertificate.IsPresent) {
        Trace.TraceInformation("Hmux[{0}] >>r:certificate ({1})", traceId, clientCertificate.Certificate.Length);
        ws.WriteByte(HmuxChannel.CSE_CLIENT_CERT);
        WriteHmuxLength(ws, clientCertificate.Certificate.Length);
        ws.Write(clientCertificate.Certificate, 0, clientCertificate.Certificate.Length);
      }


      NameValueCollection headers = request.Headers;
      foreach (String key in headers.AllKeys) {
        if ("Connection".Equals(key, StringComparison.OrdinalIgnoreCase))
          continue;

        String[] values = headers.GetValues(key);
        foreach (String value in values) {
          WriteRequestHeader(ws, key, value, traceId);
        }
      }

      Stream requestStream = request.InputStream;
      Stream responseStream = null;

      bool hasHeader = true;
      bool hasStatus = false;


      if (length > 0) {
        Trace.TraceInformation("Hmux[{0}] >>D: data ({1})", traceId, length);
        WriteRequestData(ws, HmuxChannel.HMUX_DATA, buf, length, traceId);
      }

      int len;

      int code;

      Trace.TraceInformation(":::0");

      while (!isComplete && (len = requestStream.Read(buf, 0, buf.Length)) > 0) {
        Trace.TraceInformation("Hmux[{0}] >>D: data ({1})", traceId, length);
        WriteRequestData(ws, HmuxChannel.HMUX_DATA, buf, len, traceId);

        Trace.TraceInformation("Hmux[{0}] >>Y: (yield)", traceId);
        ws.WriteByte(HmuxChannel.HMUX_YIELD);
        ws.Flush();

        Trace.TraceInformation(":::1");
        while (true) {
          code = rs.ReadByte();

          if (code < 0) {
            Trace.TraceInformation("Hmux[{0}] <<w: end of file", traceId);

            if (hasStatus)
              return OK | EXIT;
            else {
              Trace.TraceInformation("Hmux[{0}] <<w: unexpected end of file", traceId);

              return FAIL | EXIT;
            }
          } else if (code == HmuxChannel.HMUX_QUIT) {
            Trace.TraceInformation("Hmux[{0}] <<Q: (keepalive)", traceId);

            if (hasStatus)
              return OK | QUIT;
            else {
              Trace.TraceInformation("Hmux[{0}] <<Q: unexpected quit file", traceId);

              return FAIL | QUIT;
            }
          } else if (code == HmuxChannel.HMUX_EXIT) {
            Trace.TraceInformation("Hmux[{0}] <<X: (exit)", traceId);

            if (hasStatus) {
              return OK | EXIT;
            } else {
              Trace.TraceInformation("Hmux[{0}] <<X: unexpected exit", traceId);

              return FAIL | EXIT;
            }
          } else if (code == HmuxChannel.HMUX_YIELD) {
            Trace.TraceInformation("Hmux[{0}] <<Y: (yield)", traceId);

            continue;
          }

          int sublen = ReadHmuxLength(rs);

          if (code == HmuxChannel.HMUX_ACK) {
            if (isDebugFiner)
              Trace.TraceInformation("Hmux[{0}] <<A: (ack) ({1})", traceId, sublen);

            break;
          } else if (code == HmuxChannel.HMUX_CHANNEL) {
            int channel = sublen;
            Trace.TraceInformation("Hmux[{0}] <<C: (channel) ({1})", traceId, channel);
          } else if (code == HmuxChannel.HMUX_STATUS && hasHeader) {
            String status = ReadHmuxString(rs, sublen);
            Trace.TraceInformation("Hmux[{0}] <<s: (status) ({1})", traceId, status);
            int statusCode = 0;
            for (int i = 0; i < 3; i++)
              statusCode = 10 * statusCode + status[i] - '0';

            if (statusCode != 200)
              response.StatusCode = statusCode;

            hasStatus = true;
          } else if (code == HmuxChannel.HMUX_HEADER && hasHeader) {
            String name = ReadHmuxString(rs, sublen);
            rs.ReadByte();
            sublen = ReadHmuxLength(rs);
            String value = ReadHmuxString(rs, sublen);

            Trace.TraceInformation("Hmux[{0}] <<H,S: (header) ({1}={2})", traceId, name, value);

            RelayResponseHeader(response, name, value);
          } else if (code == HmuxChannel.HMUX_DATA) {
            Trace.TraceInformation("Hmux[{0}] <<D: (data)({1})", traceId, sublen);

            if (responseStream == null)
              responseStream = response.OutputStream;

            RelayResponseData(rs, responseStream, sublen);
          } else if (code == HmuxChannel.HMUX_META_HEADER) {
            String name = ReadHmuxString(rs, sublen);
            rs.ReadByte();
            sublen = ReadHmuxLength(rs);
            String value = ReadHmuxString(rs, sublen);

            Trace.TraceInformation("Hmux[{0}] <<M,S: header ({1}={2})", traceId, name, value);

            if ("cpu-load".Equals(name)) {
              double loadAvg = 0.001 * long.Parse(value);

              hmuxChannel.GetPool().SetCpuLoadAvg(loadAvg);
            }
          } else {
            Skip(rs, sublen);
          }
        }
      }

      Trace.TraceInformation(":::2");

      ws.WriteByte(HmuxChannel.HMUX_QUIT);
      ws.Flush();

      Trace.TraceInformation(":::3");

      code = rs.ReadByte();
      Trace.TraceInformation(":::4");

      // #2369 - A slow modem can cause the app-tier and web-tier times
      // to get out of sync, with the app-tier thinking it's completed
      // (and starts the keepalive timeout) 30s before the web-tier reads
      // its data.
      // As a temporary measure, we start the idle time at the first data
      // read (later we might mark the time it takes to read an app-tier
      // packet.  If it's short, e.g. 250ms, don't update the time.)
      hmuxChannel.SetIdleStartTime(DateTime.Now.Ticks);

      bool isBusy = false;
      Trace.TraceInformation(":::5");
      for (; code >= 0; code = rs.ReadByte()) {
        Trace.TraceInformation(":::6");
        if (code == HmuxChannel.HMUX_QUIT) {
          if (isDebugFiner)
            Trace.TraceInformation("Hmux[{0}] <<Q: (keepalive)", traceId);

          return isBusy ? BUSY | QUIT : OK | QUIT;
        } else if (code == HmuxChannel.HMUX_EXIT) {

          Trace.TraceInformation("Hmux[{0}] <<X: (exit)", traceId);

          return (isBusy || !hasStatus) ? BUSY | EXIT : OK | EXIT;
        } else if (code == HmuxChannel.HMUX_YIELD) {
          Trace.TraceInformation("Hmux[{0}] <<Y: (yield)", traceId);

          continue;
        }

        int sublen = (rs.ReadByte() << 8) + rs.ReadByte();

        if (code == HmuxChannel.HMUX_DATA) {
          if (responseStream == null)
            responseStream = response.OutputStream;

          Trace.TraceInformation("Hmux[{0}] <<D: (data)({1})", traceId, sublen);

          if (!isBusy)
            RelayResponseData(rs, responseStream, sublen);
          else
            Skip(rs, sublen);
        } else if (code == HmuxChannel.HMUX_STATUS && hasHeader) {
          hasStatus = true;
          String status = ReadHmuxString(rs, sublen);
          Trace.TraceInformation("Hmux[{0}] <<s: (status) ({1})", traceId, status);

          int statusCode = 0;
          for (int i = 0; i < 3; i++)
            statusCode = 10 * statusCode + status[i] - '0';

          if (statusCode == 503 && allowBusy)
            isBusy = true;
          else if (statusCode != 200)
            response.StatusCode = statusCode;
        } else if (code == HmuxChannel.HMUX_HEADER && hasHeader) {
          String name = ReadHmuxString(rs, sublen);
          rs.ReadByte();
          sublen = ReadHmuxLength(rs);
          String value = ReadHmuxString(rs, sublen);

          Trace.TraceInformation("Hmux[{0}] <<H,S: (header) ({1}={2})", traceId, name, value);

          if (!isBusy)
            RelayResponseHeader(response, name, value);
        } else if (code == HmuxChannel.HMUX_META_HEADER) {
          String name = ReadHmuxString(rs, sublen);
          rs.ReadByte();
          sublen = ReadHmuxLength(rs);
          String value = ReadHmuxString(rs, sublen);

          Trace.TraceInformation("Hmux[{0}] <<M,S: header ({1}={2})", traceId, name, value);

          if ("cpu-load".Equals(name)) {
            double loadAvg = 0.001 * long.Parse(value);

            hmuxChannel.GetPool().SetCpuLoadAvg(loadAvg);
          }
        } else if (code == HmuxChannel.HMUX_CHANNEL) {
          int channel = sublen;
          Trace.TraceInformation("Hmux[{0}] <<C: (channel) ({1})", traceId, channel);
        } else if (code == 0) {
          Trace.TraceInformation("Hmux[{0}] <<0: unknown code (0)", traceId);

          return FAIL | EXIT;
        } else {
          Trace.TraceInformation("Hmux[{0}] <<?: unknown code ({1})", traceId, code);
          Skip(rs, sublen);
        }
      }
      Trace.TraceInformation("Hmux[{0}] end of file", traceId);

      // server/269q
      if (hasStatus)
        return isBusy ? BUSY | EXIT : OK | EXIT;
      else {
        Trace.TraceInformation("Hmux[{0}] unexpected end of file", traceId, code);
        return FAIL | EXIT;
      }
    }

    private int ReadHmuxLength(BufferedStream stream)
    {
      int length = (stream.ReadByte() << 8) + stream.ReadByte();
      return length;
    }

    private String ReadHmuxString(BufferedStream stream, int length)
    {
      if (length == 0)
        return "";

      byte[] data = new byte[length];
      stream.Read(data, 0, length);

      return System.Text.Encoding.ASCII.GetString(data);
    }

    private void RelayResponseData(BufferedStream hmuxIn, Stream response, int length)
    {
      if (length <= 0)
        return;

      byte[] data = new byte[length];
      while (length > 0) {
        int len = hmuxIn.Read(data, 0, length);
        response.Write(data, 0, len);
        length -= len;
      }
    }

    private void RelayResponseHeader(HttpResponse response, String name, String value)
    {
      if ("Cache-Control".Equals(name)) {
        String []directives = value.Split(',', '=', ' ');
        for(int i = 0; i < directives.Length; i++) {
          String directive = directives[i];
          if ("no-cache".Equals(directive, StringComparison.OrdinalIgnoreCase)) {
            response.Cache.SetCacheability(HttpCacheability.NoCache);
          } else if ("public".Equals(directive, StringComparison.OrdinalIgnoreCase)) {
            response.Cache.SetCacheability(HttpCacheability.Public);
          } else if ("private".Equals(directive, StringComparison.OrdinalIgnoreCase)) {
            response.Cache.SetCacheability(HttpCacheability.Private);
          } else if ("must-revalidate".Equals(directive, StringComparison.OrdinalIgnoreCase)) {
            response.Cache.SetRevalidation(HttpCacheRevalidation.AllCaches);
          } else if ("proxy-revalidate".Equals(directive, StringComparison.OrdinalIgnoreCase)) {
            response.Cache.SetRevalidation(HttpCacheRevalidation.ProxyCaches);
          } else if ("max-age".Equals(directive, StringComparison.OrdinalIgnoreCase)) {
            response.Cache.SetMaxAge(TimeSpan.FromSeconds(int.Parse(directives[++i])));
          } else if ("s-maxage".Equals(directive, StringComparison.OrdinalIgnoreCase)) {
            response.Cache.SetProxyMaxAge(TimeSpan.FromSeconds(int.Parse(directives[++i])));
          } else if ("post-check".Equals(directive, StringComparison.OrdinalIgnoreCase)) {
            response.Cache.AppendCacheExtension("post-check="+directives[++i]);
          } else if ("pre-check".Equals(directive, StringComparison.OrdinalIgnoreCase)) {
            response.Cache.AppendCacheExtension("pre-check="+directives[++i]);
          }
        }
      } else if ("Content-Type".Equals(name)) {
        response.ContentType = value;
        int charsetIdx = value.IndexOf("charset");
        if (charsetIdx > -1) {
          String charset = null;
          int start = -1;
          int end = value.Length;
          for (int i = charsetIdx + 7; i < value.Length; i++) {
            char c = value[i];
            switch (c) {
              case '=': {
                  start = i;
                  break;
                }
              case ';':
              case ' ': {
                  end = i;
                  break;
                }
            }
          }
          if (start > -1 && end > start) {
            charset = value.Substring(start + 1, end - start - 1);
            response.Charset = charset;
          }
        }
      } else {
        response.Headers.Add(name, value);
      }
    }

    private void Skip(BufferedStream stream, int length)
    {
      byte[] junk = new byte[length];
      while (length > 0) {
        int len = stream.Read(junk, 0, length);
        length -= len;
      }
    }

    private void WriteHmuxLength(BufferedStream stream, int length)
    {
      stream.WriteByte((byte)(length >> 8));
      stream.WriteByte((byte)length);
    }

    private void WriteRequestString(BufferedStream stream, int code, String value, String traceId)
    {
      stream.WriteByte((byte)code);
      if (value == null) {
        WriteHmuxLength(stream, 0);
      } else {
        byte[] bytes = System.Text.Encoding.ASCII.GetBytes(value.ToCharArray());
        WriteHmuxLength(stream, bytes.Length);
        stream.Write(bytes, 0, bytes.Length);
      }
    }

    private void WriteRequestHeader(BufferedStream stream, String name, String value, String traceId)
    {
      Trace.TraceInformation("Hmux[{0}] >>H:{1}", traceId, name);
      WriteRequestString(stream, HmuxChannel.HMUX_HEADER, name, traceId);
      Trace.TraceInformation("Hmux[{0}] >>S:{1}", traceId, value);
      WriteRequestString(stream, HmuxChannel.HMUX_STRING, value, traceId);
    }

    private void WriteRequestData(BufferedStream stream, int code, byte[] data, int length, String traceId)
    {
      stream.WriteByte(HmuxChannel.HMUX_DATA);
      WriteHmuxLength(stream, length);
      stream.Write(data, 0, length);
    }

    private void WriteSSLCertificate(BufferedStream stream, byte[] cert, String traceId)
    {
      Trace.TraceInformation("Hmux[{0}] >>t:certificate({1})", traceId, cert.Length);
      stream.WriteByte(HmuxChannel.CSE_CLIENT_CERT);
      WriteHmuxLength(stream, cert.Length);
      stream.Write(cert, 0, cert.Length);
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
