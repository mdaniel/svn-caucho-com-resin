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

package hessian.io
{
  import flash.utils.ByteArray;
	import flash.utils.IDataInput;

  /**
   * An input for streaming Hessian data.  Data that is sent in Hessian
   * streaming packets is buffered until a whole object is received.  The
   * object is then decoded and stored within this object.  Such received
   * objects can be retrieved by calling #nextObject().
   *
   * @see hessian.io.Hessian2Input
   */
  public class Hessian2StreamingInput
  {
    private var _tag:int = -1;
    private var _length:int = 0;
    private var _offset:int = 0;
    private var _buffer:ByteArray = new ByteArray();
    private var _input:Hessian2Input = new Hessian2Input();
    private var _queue:Array = new Array();

    public function Hessian2StreamingInput()
    {
    }

    /**
      * @return true if there are objects available to read from this input.
      */
    public function hasMoreObjects():Boolean
    {
      return _queue.length > 0;
    }

    /**
      * @return The next object available from this input.
      */
    public function nextObject():Object
    {
      return _queue.shift();
    }

    /**
      * Submits data to be read as stream data.  If the data contains any
      * headers, they must be complete.  
      * 
      * @param di The source from which to add the streaming data.
      *
      */
    public function read(di:IDataInput):void
    {
      while (di.bytesAvailable > 0) {
        while (_length == 0) {
          // We expect at least three bytes at the beginning of a packet.
          // If we don't get them, we don't read anything. This is important
          // to note if you're throwing away the bytes everytime you call
          // this method.
          if (di.bytesAvailable < 3)
            return;

          _tag = di.readByte();
          if (_tag != 'p'.charCodeAt() && _tag != 'P'.charCodeAt()) {
            throw new HessianProtocolError("expected streaming packet at 0x"
                                           + (_tag & 0xff).toString(16)
                                           + "(" + String.fromCharCode(_tag) 
                                           + ")");
          }

          var d1:int = di.readUnsignedByte();
          var d2:int = di.readUnsignedByte();

          _length = (d1 << 8) + d2;
        }

        var length:int = _length;
        if (di.bytesAvailable < _length)
          length = di.bytesAvailable;

        di.readBytes(_buffer, _offset, length);

        _offset = _buffer.length;

        _length -= length;

        if (_length == 0 && _tag == 'P'.charCodeAt()) {
          _buffer.position = 0;
          _input.init(_buffer);
          _input.resetReferences();
          _queue.push(_input.readObject());

          _offset = 0;
          _buffer.length = 0;
        }
      }
    }
  }
}

