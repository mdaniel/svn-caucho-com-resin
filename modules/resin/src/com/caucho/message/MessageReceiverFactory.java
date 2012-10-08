/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
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

package com.caucho.message;


/**
 * AMQP client receiver factory
 */
public interface MessageReceiverFactory {
  public MessageReceiverFactory setAddress(String address);
  
  public String getAddress();
  
  public MessageReceiverFactory setListener(MessageReceiverListener<?> listener);
  
  public MessageReceiverListener<?> getListener();
  
  public MessageReceiverFactory setDistributionMode(DistributionMode mode);
  
  public DistributionMode getDistributionMode();
  
  public MessageReceiverFactory setPrefetch(int prefetch);
  
  public int getPrefetch();
  
  public MessageReceiverFactory setSettleMode(SettleMode settleMode);
  
  public SettleMode getSettleMode();
  
  public MessageReceiverFactory setSettleTime(SettleTime settleTime);
  
  public SettleTime getSettleTime();
  
  // public MessageReceiverFactory setMessageListener(MessageListener listener);
  
  public MessageReceiverFactory setMessageDecoder(MessageDecoder<?> decoder);
  
  public MessageDecoder<?> getMessageDecoder();
  
  public MessageReceiver<?> build();
}
