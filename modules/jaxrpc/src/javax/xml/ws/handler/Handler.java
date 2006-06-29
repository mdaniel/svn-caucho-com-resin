/*
* Copyright (c) 1998-2006 Caucho Technology -- all rights reserved
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
* @author Scott Ferguson
*/

package javax.xml.ws.handler;

/**
 * The Handler interface is the base interface for JAX-WS handlers. Since:
 * JAX-WS 2.0
 */
public interface Handler<C extends MessageContext> {

  /**
   * Called at the conclusion of a message exchange pattern just prior to the
   * JAX-WS runtime disptaching a message, fault or exception. Refer to the
   * description of the handler framework in the JAX-WS specification for full
   * details.
   */
  abstract void close(MessageContext context);


  /**
   * The handleFault method is invoked for fault message processing. Refer to
   * the description of the handler framework in the JAX-WS specification for
   * full details.
   */
  abstract boolean handleFault(C context);


  /**
   * The handleMessage method is invoked for normal processing of inbound and
   * outbound messages. Refer to the description of the handler framework in
   * the JAX-WS specification for full details.
   */
  abstract boolean handleMessage(C context);

}

