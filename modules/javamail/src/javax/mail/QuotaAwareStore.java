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

package javax.mail;

/**
 * An interface implemented by Stores that support quotas. The
 * getQuota and setQuota methods support the quota model defined by
 * the IMAP QUOTA extension. Refer to RFC 2087 for more information.
 * Since: JavaMail 1.4
 */
public interface QuotaAwareStore {

  /**
   * Get the quotas for the named quota root. Quotas are controlled on
   * the basis of a quota root, not (necessarily) a folder. The
   * relationship between folders and quota roots depends on the
   * server. Some servers might implement a single quota root for all
   * folders owned by a user. Other servers might implement a separate
   * quota root for each folder. A single folder can even have
   * multiple quota roots, perhaps controlling quotas for different
   * resources.
   */
  public abstract Quota[] getQuota(String root) throws MessagingException;

  /**
   * Set the quotas for the quota root specified in the quota
   * argument. Typically this will be one of the quota roots obtained
   * from the getQuota method, but it need not be.
   */
  public abstract void setQuota(Quota quota) throws MessagingException;

}
