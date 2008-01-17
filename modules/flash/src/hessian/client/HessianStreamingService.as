/*
 * Copyright (c) 2001-2008 Caucho Technology, Inc.  All rights reserved.
 *
 * The Apache Software License, Version 1.1
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowlegement:
 *       "This product includes software developed by the
 *        Caucho Technology (http://www.caucho.com/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "Burlap", "Resin", and "Caucho" must not be used to
 *    endorse or promote products derived from this software without prior
 *    written permission. For written permission, please contact
 *    info@caucho.com.
 *
 * 5. Products derived from this software may not be called "Resin"
 *    nor may "Resin" appear in their names without prior written
 *    permission of Caucho Technology.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL CAUCHO TECHNOLOGY OR ITS CONTRIBUTORS
 * BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY,
 * OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT
 * OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
 * BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
 * IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * @author Emil Ong
 * 
 */

package hessian.client
{
  import flash.events.Event;
  import flash.events.ProgressEvent;
  import flash.events.TimerEvent;
  import flash.net.URLRequest;
  import flash.net.URLStream;
  import flash.net.Socket;
  import flash.system.Security;
  import flash.utils.describeType;
  import flash.utils.Timer;

  import hessian.io.Hessian2StreamingInput;
  import hessian.util.URL;

  import mx.core.Application;
  import mx.events.FlexEvent;
  import mx.rpc.IResponder;
  import mx.utils.URLUtil;

  /**
   * The HessianStreamingService class provides access to streaming
   * Hessian-based web services on remote servers.
   *
   * @see hessian.client.HessianStreamingOperation
   * @see hessian.mxml.HessianStreamingService
   */
  public dynamic class HessianStreamingService 
  {
    private const _input:Hessian2StreamingInput = new Hessian2StreamingInput();

    private var _url:URL;
    private var _socket:Socket = new Socket();
    private var _destination:String;
    private var _responder:IResponder;
    private var _readHTTPHeader:Boolean = false;
    private var _headerHistory:Array = new Array(4);
    private var _policyPort:int = -1;

    /**
     * Constructor.
     *
     * @param dst  The URL of the destination service.
     * @param port The port of the policy server, if any.
     *
     */
    public function HessianStreamingService(dst:String = null, 
                                            port:int = -1)
    {
      destination = dst;
      policyPort = port;
    }
    
    private function handleCreation(event:Event):void
    {
      initSocket();
    }

    private function handleConnect(event:Event):void
    {
      _socket.writeUTFBytes("GET " + _url.path + " HTTP/1.0\r\n");
      _socket.writeUTFBytes("Host: " + _url.host + ":" + _url.port + "\r\n");
      _socket.writeUTFBytes("\r\n");
      _socket.addEventListener(ProgressEvent.SOCKET_DATA, handleData);
    }

    private function handleData(event:Event):void
    {
      if (_socket.bytesAvailable <= 0)
        return;

      if (! _readHTTPHeader) {
        while (_socket.bytesAvailable > 0) {
          if (_headerHistory[0] == '\r' && _headerHistory[1] == '\n' &&
              _headerHistory[2] == '\r' && _headerHistory[3] == '\n') {
            _headerHistory = null;
            _readHTTPHeader = true;
            break;
          }

          _headerHistory.push(_socket.readUTFBytes(1));
          _headerHistory.shift();
        }
      }

      _input.read(_socket);

      while (_input.hasMoreObjects()) 
        responder.result(_input.nextObject());
    }

    /**
      * The responder that this service calls when a new object comes in.
      */
    public function get responder():IResponder
    {
      return _responder;
    }

    public function set responder(value:IResponder):void
    {
      _responder = value;
    }

    /**
      * The URL of the service.  A connection is established when the 
      * destination is set.
      */
    public function get destination():String
    {
      return _destination;
    }

    public function set destination(value:String):void
    {
      _destination = value;

      if (Application.application.url == null) {
        Application.application.addEventListener(FlexEvent.CREATION_COMPLETE,
                                                 handleCreation);
        return;
      }

      initSocket();
    }

    /**
     * Sets the port on which the XMLSocket server is listening to serve
     * the policy file.
     */
    public function get policyPort():int
    {
      return _policyPort;
    }

    public function set policyPort(policyPort:int):void
    {
      _policyPort = policyPort;
    }

    private function initSocket():void
    {
      _url = 
        new URL(URLUtil.getFullURL(Application.application.url, destination));

      var policy:String = "xmlsocket://" + _url.host + ":" + 
                          (_policyPort < 0 ? _url.port : _policyPort);

      Security.loadPolicyFile(policy);

      _socket = new Socket(_url.host, _url.port);
      _socket.addEventListener(Event.CONNECT, handleConnect);
    }
  }
}
