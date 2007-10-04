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
  import flash.events.Event;
  import flash.events.TimerEvent;
  import flash.net.URLRequest;
  import flash.net.URLStream;
  import flash.utils.Timer;

  import hessian.io.Hessian2StreamingInput;

  import mx.rpc.IResponder;

  /**
   * The HessianStreamingService class provides access to streaming
   * Hessian-based web services on remote servers.
   *
   * @see hessian.client.HessianStreamingOperation
   * @see hessian.mxml.HessianStreamingService
   */
  public dynamic class HessianStreamingService 
  {
    // We are using a timer instead of the URLStream ProgressEvent because
    // the ProgressEvent doesn't seem to fire when the URLStream has new
    // data.  Using the timer, we poll for new data every 1/2 second.
    // We might want to make this configurable at some point.
    private const _timer:Timer = new Timer(500);
    private const _stream:URLStream = new URLStream();
    private const _input:Hessian2StreamingInput = new Hessian2StreamingInput();

    private var _destination:String;
    private var _responder:IResponder;

    /**
     * Constructor.
     *
     * @param destination The URL of the destination service.
     * @param api The API associated with this HessianStreamingService, if any.
     *
     */
    public function HessianStreamingService(destination:String = null)
    {
      _destination = destination;
    }

    /** @private */
    public function handleComplete(event:Event):void
    {
      _stream.close();
      _timer.stop();
    }

    /** @private */
    public function handleTimer(event:Event):void
    {
      if (_stream.bytesAvailable <= 0)
        return;

      _input.read(_stream);

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

      var request:URLRequest = new URLRequest();
      request.data = "";
      request.url = destination;
      request.method = "POST";
      request.contentType = "binary/octet-stream";

      _stream.addEventListener(Event.COMPLETE, handleComplete);
      _stream.load(request);

      _timer.addEventListener(TimerEvent.TIMER, handleTimer);
      _timer.start();
    }
  }
}
