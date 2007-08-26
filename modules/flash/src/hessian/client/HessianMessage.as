/*
 * Copyright (c) 2001-2007 Caucho Technology, Inc.  All rights reserved.
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
  import mx.messaging.messages.IMessage;

  public class HessianMessage implements IMessage
  {
    private var _body:Object;
    private var _clientId:String;
    private var _destination:String;
    private var _headers:Object;
    private var _messageId:String;
    private var _timestamp:Number;
    private var _timeToLive:Number;

    public function HessianMessage(body:Object, destination:String)
    {
      _body = body;
      _destination = destination;
    }

    public function get body():Object
    {
      return _body;
    }

    public function set body(value:Object):void 
    {
      _body = value;
    }

    public function get clientId():String
    {
      return _clientId;
    }

    public function set clientId(value:String):void
    {
      _clientId = value;
    }

    public function get destination():String
    {
      return _destination;
    }

    public function set destination(value:String):void 
    {
      _destination = value;
    }

    public function get headers():Object
    {
      return _headers;
    }

    public function set headers(value:Object):void 
    {
      _headers = value;
    }

    public function get messageId():String
    {
      return _messageId;
    }

    public function set messageId(value:String):void 
    {
      _messageId = value;
    }

    public function get timestamp():Number
    {
      return _timestamp;
    }

    public function set timestamp(value:Number):void 
    {
      _timestamp = value;
    }

    public function get timeToLive():Number
    {
      return _timeToLive;
    }

    public function set timeToLive(value:Number):void
    {
      _timeToLive = value;
    }

    public function toString():String
    {
      return "HessianMessage[body=" + body + "," +
                            "clientId=" + clientId + "," +
                            "destination=" + destination + "," +
                            "headers=" + headers + "," + 
                            "messageId=" + messageId + "," + 
                            "timestamp=" + timestamp + "," +
                            "timeToLive=" + timeToLive + "]";
    }
  }
}
