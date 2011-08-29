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
 * @author Scott Ferguson, Emil Ong
 * 
 */

package hessian.io 
{
  /**
   * Constants used in the Hessian 2.0 protocol.
   *
   */
  public class Hessian2Constants
  {
    public static const INT_DIRECT_MIN:int = -0x10;
    public static const INT_DIRECT_MAX:int = 0x2f;
    public static const INT_ZERO:int = 0x90;

    public static const INT_BYTE_MIN:int = -0x800;
    public static const INT_BYTE_MAX:int = 0x7ff;
    public static const INT_BYTE_ZERO:int = 0xc8;

    public static const INT_SHORT_MIN:int = -0x40000;
    public static const INT_SHORT_MAX:int = 0x3ffff;
    public static const INT_SHORT_ZERO:int = 0xd4;

    public static const LONG_DIRECT_MIN:int = -0x08;
    public static const LONG_DIRECT_MAX:int = 0x0f;
    public static const LONG_ZERO:int = 0xe0;

    public static const LONG_BYTE_MIN:int = -0x800;
    public static const LONG_BYTE_MAX:int = 0x7ff;
    public static const LONG_BYTE_ZERO:int = 0xf8;

    public static const LONG_SHORT_MIN:int = -0x40000;
    public static const LONG_SHORT_MAX:int = 0x3ffff;
    public static const LONG_SHORT_ZERO:int = 0x3c;

    public static const STRING_DIRECT_MAX:int = 0x1f;
    public static const STRING_DIRECT:int = 0x00;

    public static const BYTES_DIRECT_MAX:int = 0x0f;
    public static const BYTES_DIRECT:int = 0x20;
    // 0x30-0x37 is reserved

    public static const LONG_INT:int = 0x77;

    public static const DOUBLE_ZERO:int = 0x67;
    public static const DOUBLE_ONE:int = 0x68;
    public static const DOUBLE_BYTE:int = 0x69;
    public static const DOUBLE_SHORT:int = 0x6a;
    public static const DOUBLE_FLOAT:int = 0x6b;
    
    public static const LENGTH_BYTE:int = 0x6e;
    public static const LIST_FIXED:int = 0x76; // 'v'

    public static const REF_BYTE:int = 0x4a;
    public static const REF_SHORT:int = 0x4b;

    public static const TYPE_REF:int = 0x75;
  }
}
