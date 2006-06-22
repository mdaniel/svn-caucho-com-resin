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
import java.util.*;

/**
 * RFC2045, Section 2.4
 */
public interface MimePart extends Part {

    /**
     * Add a raw RFC822 header-line.
     */
    public abstract void addHeaderLine(String line) throws MessagingException;

    /**
     * Get all header lines as an Enumeration of Strings.
     */
    public abstract Enumeration getAllHeaderLines() throws MessagingException;

    /**
     * Get the Content-ID of this part. Returns null if none present.
     */
    public abstract String getContentID() throws MessagingException;

    /**
     * Get the language tags specified in the Content-Language header
     * of this MimePart. The Content-Language header is defined by RFC
     * 1766. Returns null if this header is not available.
     */
    public abstract String[] getContentLanguage() throws MessagingException;

    /**
     * Get the Content-MD5 digest of this part. Returns null if none present.
     */
    public abstract String getContentMD5() throws MessagingException;

    /**
     * Get the transfer encoding of this part.
     */
    public abstract String getEncoding() throws MessagingException;

    /**
     * Get the values of all header fields available for this header,
     * returned as a single String, with the values separated by the
     * delimiter. If the delimiter is null, only the first value is
     * returned.
     */
    public abstract String getHeader(String name, String delimiter)
      throws MessagingException;

    /**
     * Get matching header lines as an Enumeration of Strings. A
     * Header line is a raw RFC822 header-line, containing both the
     * "name" and "value" field.
     */
    public abstract Enumeration getMatchingHeaderLines(String[] names)
      throws MessagingException;

    /**
     * Get non-matching header lines as an Enumeration of Strings. A
     * Header line is a raw RFC822 header-line, containing both the
     * "name" and "value" field.
     */
    public abstract Enumeration getNonMatchingHeaderLines(String[] names)
      throws MessagingException;

    /**
     * Set the Content-Language header of this MimePart. The
     * Content-Language header is defined by RFC1766.
     */
    public abstract void setContentLanguage(String[] languages)
      throws MessagingException;

    /**
     * Set the Content-MD5 of this part.
     */
    public abstract void setContentMD5(String md5) throws MessagingException;

    /**
     * Convenience method that sets the given String as this part's
     * content, with a MIME type of "text/plain". If the string
     * contains non US-ASCII characters. it will be encoded using the
     * platform's default charset. The charset is also used to set the
     * "charset" parameter.
     *
     * Note that there may be a performance penalty if text is large,
     * since this method may have to scan all the characters to
     * determine what charset to use.
     *
     * If the charset is already known, use the setText method that
     * takes the charset parameter.
     */
    public abstract void setText(String text) throws MessagingException;

    /**
     * Convenience method that sets the given String as this part's
     * content, with a MIME type of "text/plain" and the specified
     * charset. The given Unicode string will be charset-encoded using
     * the specified charset. The charset is also used to set
     * "charset" parameter.
     */
    public abstract void setText(String text, String charset)
      throws MessagingException;

    /**
     * Convenience method that sets the given String as this part's
     * content, with a primary MIME type of "text" and the specified
     * MIME subtype. The given Unicode string will be charset-encoded
     * using the specified charset. The charset is also used to set
     * the "charset" parameter.
     */
    public abstract void setText(String text, String charset, String subtype)
      throws MessagingException;

}
