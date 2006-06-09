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

package javax.mail.internet;
import javax.mail.*;
import java.io.*;

/**
 * An InputStream that is backed by data that can be shared by
 * multiple readers may implement this interface. This allows users of
 * such an InputStream to determine the current position in the
 * InputStream, and to create new InputStreams representing a subset
 * of the data in the original InputStream. The new InputStream will
 * access the same underlying data as the original, without copying
 * the data.
 *
 * Note that implementations of this interface must ensure that the
 * close method does not close any underlying stream that might be
 * shared by multiple instances of SharedInputStream until all shared
 * instances have been closed.
 *
 * Since: JavaMail 1.2
 */
public interface SharedInputStream {

    /**
     * Return the current position in the InputStream, as an offset
     * from the beginning of the InputStream.
     */
    public abstract long getPosition();

    /**
     * Return a new InputStream representing a subset of the data from
     * this InputStream, starting at start (inclusive) up to end
     * (exclusive). start must be non-negative. If end is -1, the new
     * stream ends at the same place as this stream. The returned
     * InputStream will also implement the SharedInputStream
     * interface.
     */
    public abstract InputStream newStream(long start, long end);

}
