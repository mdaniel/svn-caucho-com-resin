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
 */

package hessian.io
{
	import flash.errors.IllegalOperationError;
	import flash.utils.ByteArray;
	import flash.utils.IDataOutput;
  import flash.utils.getQualifiedClassName;

  public class HessianOutput extends AbstractHessianOutput
  {
    private var _out:IDataOutput;
    private var _version:int = 1;
    private var _refs:Object;
    private var _numRefs:int = 0;

    public function HessianOutput(out:IDataOutput = null)
    {
      init(out);
    }

    /**
     * Initialize the Hessian stream with the underlying input stream.
     */
    public override function init(out:IDataOutput):void
    {
      _out = out;
      resetReferences();
    }

    /**
     * Starts the method call:
     *
     * <code><pre>
     * c major minor
     * m b16 b8 method-namek
     * </pre></code>
     *
     * @param method the method name to call.
     */
    public override function startCall(method:String = null):void
    {
      if (method == null) {
        _out.writeByte('c'.charCodeAt());
        _out.writeByte(0);
        _out.writeByte(1);
      }
      else {
        _out.writeByte('c'.charCodeAt());
        _out.writeByte(_version);
        _out.writeByte(0);

        _out.writeByte('m'.charCodeAt());
        var len:int = method.length;
        _out.writeByte(len >> 8);
        _out.writeByte(len);
        printString(method, 0, len);
      }
    }

    /**
     * Writes the method tag.
     *
     * <code><pre>
     * m b16 b8 method-name
     * </pre></code>
     *
     * @param method the method name to call.
     */
    public override function writeMethod(method:String):void
    {
      _out.writeByte('m'.charCodeAt());
      var len:int = method.length;
      _out.writeByte(len >> 8);
      _out.writeByte(len);
      printString(method, 0, len);
    }

    /**
     * Completes the method call:
     *
     * <code><pre>
     * z
     * </pre></code>
     */
    public override function completeCall():void
    {
      _out.writeByte('z'.charCodeAt());
    }

    /**
     * Starts the reply
     *
     * <p>A successful completion will have a single value:
     *
     * <pre>
     * r
     * </pre>
     */
    public override function startReply():void
    {
      _out.writeByte('r'.charCodeAt());
      _out.writeByte(1);
      _out.writeByte(0);
    }

    /**
     * Completes reading the reply
     *
     * <p>A successful completion will have a single value:
     *
     * <pre>
     * z
     * </pre>
     */
    public override function completeReply():void
    {
      _out.writeByte('z'.charCodeAt());
    }

    /**
     * Writes a header name.  The header value must immediately follow.
     *
     * <code><pre>
     * H b16 b8 foo <em>value</em>
     * </pre></code>
     */
    public override function writeHeader(name:String):void
    {
      var len:int = name.length;

      _out.writeByte('H'.charCodeAt());
      _out.writeByte(len >> 8);
      _out.writeByte(len);

      printString(name);
    }

    /**
     * Writes a fault.  The fault will be written
     * as a descriptive string followed by an object:
     *
     * <code><pre>
     * f
     * &lt;string>code
     * &lt;string>the fault code
     *
     * &lt;string>message
     * &lt;string>the fault mesage
     *
     * &lt;string>detail
     * mt\x00\xnnjavax.ejb.FinderException
     *     ...
     * z
     * z
     * </pre></code>
     *
     * @param code the fault code, a three digit
     */
    public override function writeFault(code:String, 
                                        message:String, 
                                        detail:Object):void
    {
      _out.writeByte('f'.charCodeAt());
      writeString("code");
      writeString(code);

      writeString("message");
      writeString(message);

      if (detail != null) {
        writeString("detail");
        writeObject(detail);
      }
      _out.writeByte('z'.charCodeAt());
    }

    /**
     * Writes a generic object to the output stream.
     */
    public override function writeObject(object:Object, 
                                         className:String = null):void
    {
      if (object == null) {
        writeNull();
        return;
      }

      if (object is Boolean || className == "Boolean") {
        writeBoolean(object as Boolean);
        return;
      }
      else if (object is int || className == "int") {
        writeInt(object as int);
        return;
      }
      else if (object is Number || className == "Number") {
        writeDouble(object as Number); // XXX should this be writeLong?
        return;
      }
      else if (object is Date || className == "Date") {
        writeUTCDate((object as Date).valueOf());
        return;
      }
      else if (object is Array || className == "Array") {
        var array:Array = object as Array;

        var hasEnd:Boolean = writeListBegin(array.length, className);

        for (var i:int = 0; i < array.length; i++)
          writeObject(array[i]);

        if (hasEnd)
          writeListEnd();

        return;
      }
      else if (object is String || className == "String") {
        writeString(object as String);
        return;
      }
      else if (object is ByteArray || className == "ByteArray") {
        writeBytes(object as ByteArray);
        return;
      }

      if (addRef(object))
        return;

      // writeReplace not supported at this time
      // to save processing time

      className = getQualifiedClassName(object) as String;
      var ref:int = writeObjectBegin(className);

      // XXX
      // if (ref < -1) {
        writeObject10(object);
      /*
      }
      else {
        if (ref == -1) {
          writeDefinition20();
          out.writeObjectBegin(className);
        }

        writeInstance(object);
      }*/
    }

    private function writeObject10(obj:Object):void
    {
      for (var key:Object in obj) {
        writeObject(key);
        writeObject(obj[key]);
      }

      writeMapEnd();
    }

    /**
     * Writes the list header to the stream.  List writers will call
     * <code>writeListBegin</code> followed by the list contents and then
     * call <code>writeListEnd</code>.
     *
     * <code><pre>
     * &lt;list>
     *   &lt;type>java.util.ArrayList&lt;/type>
     *   &lt;length>3&lt;/length>
     *   &lt;int>1&lt;/int>
     *   &lt;int>2&lt;/int>
     *   &lt;int>3&lt;/int>
     * &lt;/list>
     * </pre></code>
     */
    public override function writeListBegin(length:int, 
                                            type:String = null):Boolean
    {
      _out.writeByte('V'.charCodeAt());

      if (type != null) {
        _out.writeByte('t'.charCodeAt());
        printLenString(type);
      }

      if (length >= 0) {
        _out.writeByte('l'.charCodeAt());
        _out.writeByte(length >> 24);
        _out.writeByte(length >> 16);
        _out.writeByte(length >> 8);
        _out.writeByte(length);
      }

      return true;
    }

    /**
     * Writes the tail of the list to the stream.
     */
    public override function writeListEnd():void
    {
      _out.writeByte('z'.charCodeAt());
    }

    /**
     * Writes the map header to the stream.  Map writers will call
     * <code>writeMapBegin</code> followed by the map contents and then
     * call <code>writeMapEnd</code>.
     *
     * <code><pre>
     * Mt b16 b8 type (<key> <value>)z
     * </pre></code>
     */
    public override function writeMapBegin(type:String):void
    {
      _out.writeByte('M'.charCodeAt());
      _out.writeByte('t'.charCodeAt());

      if (type == null || type == "Object")
        type = "";

      printLenString(type);
    }

    /**
     * Writes the tail of the map to the stream.
     */
    public override function writeMapEnd():void
    {
      _out.writeByte('z'.charCodeAt());
    }

    /**
     * Writes a boolean value to the stream.  The boolean will be written
     * with the following syntax:
     *
     * <code><pre>
     * T
     * F
     * </pre></code>
     *
     * @param value the boolean value to write.
     */
    public override function writeBoolean(value:Boolean):void
    {
      if (value)
        _out.writeByte('T'.charCodeAt());
      else
        _out.writeByte('F'.charCodeAt());
    }

    /**
     * Writes an integer value to the stream.  The integer will be written
     * with the following syntax:
     *
     * <code><pre>
     * I b32 b24 b16 b8
     * </pre></code>
     *
     * @param value the integer value to write.
     */
    public override function writeInt(value:int):void
    {
      _out.writeByte('I'.charCodeAt());
      _out.writeByte(value >> 24);
      _out.writeByte(value >> 16);
      _out.writeByte(value >> 8);
      _out.writeByte(value);
    }

    /**
     * Writes a long value to the stream.  The long will be written
     * with the following syntax:
     *
     * <code><pre>
     * L b64 b56 b48 b40 b32 b24 b16 b8
     * </pre></code>
     *
     * @param value the long value to write.
     */
    public override function writeLong(value:Number):void
    {
      _out.writeByte('L'.charCodeAt());
      _out.writeByte(0xFF & (value >> 56));
      _out.writeByte(0xFF & (value >> 48));
      _out.writeByte(0xFF & (value >> 40));
      _out.writeByte(0xFF & (value >> 32));
      _out.writeByte(0xFF & (value >> 24));
      _out.writeByte(0xFF & (value >> 16));
      _out.writeByte(0xFF & (value >> 8));
      _out.writeByte(0xFF & (value));
    }

    /**
     * Writes a double value to the stream.  The double will be written
     * with the following syntax:
     *
     * <code><pre>
     * D b64 b56 b48 b40 b32 b24 b16 b8
     * </pre></code>
     *
     * @param value the double value to write.
     */
    public override function writeDouble(value:Number):void
    {
      var bits:ByteArray = Double.doubleToLongBits(value);

      _out.writeByte('D'.charCodeAt());
      _out.writeBytes(bits);
    }

    /**
     * Writes a date to the stream.
     *
     * <code><pre>
     * T  b64 b56 b48 b40 b32 b24 b16 b8
     * </pre></code>
     *
     * @param time the date in milliseconds from the epoch in UTC
     */
    public override function writeUTCDate(time:Number):void
    {
      _out.writeByte('d'.charCodeAt());
      _out.writeByte(0xFF & (time / 0x100000000000000));
      _out.writeByte(0xFF & (time / 0x1000000000000));
      _out.writeByte(0xFF & (time / 0x10000000000));
      _out.writeByte(0xFF & (time / 0x100000000));
      _out.writeByte(0xFF & (time >> 24));
      _out.writeByte(0xFF & (time >> 16));
      _out.writeByte(0xFF & (time >> 8));
      _out.writeByte(0xFF & (time));
    }

    /**
     * Writes a null value to the stream.
     * The null will be written with the following syntax
     *
     * <code><pre>
     * N
     * </pre></code>
     *
     * @param value the string value to write.
     */
    public override function writeNull():void
    {
      _out.writeByte('N'.charCodeAt());
    }

    /**
     * Writes a string value to the stream using UTF-8 encoding.
     * The string will be written with the following syntax:
     *
     * <code><pre>
     * S b16 b8 string-value
     * </pre></code>
     *
     * If the value is null, it will be written as
     *
     * <code><pre>
     * N
     * </pre></code>
     *
     * @param value the string value to write.
     */
    public override function writeString(value:*, 
                                         offset:int = 0, 
                                         length:int = 0):void
    {
      if (value == null)
        _out.writeByte('N'.charCodeAt());

      else if (value is Array)
        writeCharArray(value as Array, offset, length);

      else {
        length = value.length;
        offset = 0;

        while (length > 0x8000) {
          var sublen:int = 0x8000;

          // chunk can't end in high surrogate
          var tail:int = value.charCodeAt(offset + sublen - 1);

          if (0xd800 <= tail && tail <= 0xdbff)
            sublen--;

          _out.writeByte('s'.charCodeAt());
          _out.writeByte(sublen >> 8);
          _out.writeByte(sublen);

          printString(value, offset, sublen);

          length -= sublen;
          offset += sublen;
        }

        _out.writeByte('S'.charCodeAt());
        _out.writeByte(length >> 8);
        _out.writeByte(length);

        printString(value, offset, length);
      }
    }

    /**
     * Writes a string value to the stream using UTF-8 encoding.
     * The string will be written with the following syntax:
     *
     * <code><pre>
     * S b16 b8 string-value
     * </pre></code>
     *
     * If the value is null, it will be written as
     *
     * <code><pre>
     * N
     * </pre></code>
     *
     * @param value the string value to write.
     */
    private function writeCharArray(buffer:Array, offset:int, length:int):void
    {
      while (length > 0x8000) {
        var sublen:int = 0x8000;

        // chunk can't end in high surrogate
        var tail:int = buffer[offset + sublen - 1];

        if (0xd800 <= tail && tail <= 0xdbff)
          sublen--;

        _out.writeByte('s'.charCodeAt());
        _out.writeByte(sublen >> 8);
        _out.writeByte(sublen);

        printCharArray(buffer, offset, sublen);

        length -= sublen;
        offset += sublen;
      }

      _out.writeByte('S'.charCodeAt());
      _out.writeByte(length >> 8);
      _out.writeByte(length);

      printCharArray(buffer, offset, length);
    }

    /**
     * Writes a byte array to the stream.
     * The array will be written with the following syntax:
     *
     * <code><pre>
     * B b16 b18 bytes
     * </pre></code>
     *
     * If the value is null, it will be written as
     *
     * <code><pre>
     * N
     * </pre></code>
     *
     * @param value the string value to write.
     */
    public override function writeBytes(buffer:ByteArray, 
                                        offset:int = 0, 
                                        length:int = -1):void
    {
      if (buffer == null)
        _out.writeByte('N'.charCodeAt());

      else {
        if (length < 0)
          length = buffer.length;

        while (length > 0x8000) {
          var sublen:int = 0x8000;

          _out.writeByte('b'.charCodeAt());
          _out.writeByte(sublen >> 8);
          _out.writeByte(sublen);

          _out.writeBytes(buffer, offset, sublen);

          length -= sublen;
          offset += sublen;
        }

        _out.writeByte('B'.charCodeAt());
        _out.writeByte(length >> 8);
        _out.writeByte(length);
        _out.writeBytes(buffer, offset, length);
      }
    }
  
    /**
     * Writes a byte buffer to the stream.
     */
    public override function writeByteBufferStart():void
    {
    }
  
    /**
     * Writes a byte buffer to the stream.
     *
     * <code><pre>
     * b b16 b18 bytes
     * </pre></code>
     *
     * @param value the string value to write.
     */
    public override function writeByteBufferPart(buffer:ByteArray,
                                                 offset:int,
                                                 length:int):void
    {
      while (length > 0) {
        var sublen:int = length;

        if (0x8000 < sublen)
          sublen = 0x8000;

        _out.writeByte('b'.charCodeAt());
        _out.writeByte(sublen >> 8);
        _out.writeByte(sublen);

        _out.writeBytes(buffer, offset, sublen);

        length -= sublen;
        offset += sublen;
      }
    }
  
    /**
     * Writes the last chunk of a byte buffer to the stream.
     *
     * <code><pre>
     * b b16 b18 bytes
     * </pre></code>
     *
     * @param value the string value to write.
     */
    public override function writeByteBufferEnd(buffer:ByteArray,
                                                offset:int,
                                                length:int):void
    {
      writeBytes(buffer, offset, length);
    }

    /**
     * Writes a reference.
     *
     * <code><pre>
     * R b32 b24 b16 b8
     * </pre></code>
     *
     * @param value the integer value to write.
     */
    public override function writeRef(value:int):void
    {
      _out.writeByte('R'.charCodeAt());
      _out.writeByte(value >> 24);
      _out.writeByte(value >> 16);
      _out.writeByte(value >> 8);
      _out.writeByte(value);
    }

    /**
     * Adds an object to the reference list.  If the object already exists,
     * writes the reference, otherwise, the caller is responsible for
     * the serialization.
     *
     * <code><pre>
     * R b32 b24 b16 b8
     * </pre></code>
     *
     * @param object the object to add as a reference.
     *
     * @return true if the object has already been written.
     */
    public override function addRef(object:Object):Boolean
    {
      if (_refs == null)
        _refs = new Object();

      var ref:Object = _refs[object];

      if (ref != null) {
        var value:int = ref as int;

        writeRef(value);
        return true;
      }
      else {
        _refs[object] = _numRefs++;

        return false;
      }
    }

    /**
     * Resets the references for streaming.
     */
    public override function resetReferences():void
    {
      if (_refs != null) {
        _refs = new Object();
        _numRefs = 0;
      }
    }

    /**
     * Removes a reference.
     */
    public override function removeRef(obj:Object):Boolean
    {
      if (_refs != null) {
        delete _refs[obj];
        _numRefs--;

        return true;
      }
      else
        return false;
    }

    /**
     * Replaces a reference from one object to another.
     */
    public override function replaceRef(oldRef:Object, newRef:Object):Boolean
    {
      var value:Object = _refs[oldRef];

      if (value != null) {
        delete _refs[oldRef];
        _refs[newRef] = value;
        return true;
      }
      else
        return false;
    }

    /**
     * Prints a string to the stream, encoded as UTF-8 with preceeding length
     *
     * @param v the string to print.
     */
    public function printLenString(v:String):void
    {
      if (v == null) {
        _out.writeByte(0);
        _out.writeByte(0);
      }
      else {
        var len:int = v.length;
        _out.writeByte(len >> 8);
        _out.writeByte(len);

        printString(v, 0, len);
      }
    }

    /**
     * Prints a string to the stream, encoded as UTF-8
     *
     * @param v the string to print.
     */
    public function printString(v:String, offset:int = 0, length:int = -1):void
    {
      if (length < 0)
        length = v.length;

      for (var i:int = 0; i < length; i++) {
        var ch:int = v.charCodeAt(i + offset);

        if (ch < 0x80)
          _out.writeByte(ch);

        else if (ch < 0x800) {
          _out.writeByte(0xc0 + ((ch >> 6) & 0x1f));
          _out.writeByte(0x80 + (ch & 0x3f));
        }

        else {
          _out.writeByte(0xe0 + ((ch >> 12) & 0xf));
          _out.writeByte(0x80 + ((ch >> 6) & 0x3f));
          _out.writeByte(0x80 + (ch & 0x3f));
        }
      }
    }

    /**
     * Prints a string to the stream, encoded as UTF-8
     *
     * @param v the string to print.
     */
    public function printCharArray(v:Array, offset:int, length:int):void
    {
      for (var i:int = 0; i < length; i++) {
        var ch:int = v[i + offset];

        if (ch < 0x80)
          _out.writeByte(ch);

        else if (ch < 0x800) {
          _out.writeByte(0xc0 + ((ch >> 6) & 0x1f));
          _out.writeByte(0x80 + (ch & 0x3f));
        }

        else {
          _out.writeByte(0xe0 + ((ch >> 12) & 0xf));
          _out.writeByte(0x80 + ((ch >> 6) & 0x3f));
          _out.writeByte(0x80 + (ch & 0x3f));
        }
      }
    }
  }
}
