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

/**
 * This class tokenizes RFC822 and MIME headers into the basic symbols
 * specified by RFC822 and MIME.
 *
 * This class handles folded headers (ie headers with embedded CRLF
 * SPACE sequences). The folds are removed in the returned tokens.
 */
public class HeaderTokenizer {

  /**
   * MIME specials
   * See Also:Constant Field Values
   */
  public static final String MIME="()<>@,;:\\\"\t []/?=";

  /**
   * RFC822 specials
   * See Also:Constant Field Values
   */
  public static final String RFC822="()<>@,;:\\\"\t .[]";

  /**
   * Constructor. The RFC822 defined delimiters - RFC822 - are used to
   * delimit ATOMS. Also comments are skipped and not returned as
   * tokens
   */
  public HeaderTokenizer(String header)
  {
    throw new UnsupportedOperationException("not implemented");
  }

  /**
   * Constructor. Comments are ignored and not returned as tokens
   * header - The header that is tokenizeddelimiters - The delimiters
   * to be used
   */
  public HeaderTokenizer(String header, String delimiters)
  {
    throw new UnsupportedOperationException("not implemented");
  }

  /**
   * Constructor that takes a rfc822 style header.  header - The
   * rfc822 header to be tokenizeddelimiters - Set of delimiter
   * characters to be used to delimit ATOMS. These are usually RFC822
   * or MIMEskipComments - If true, comments are skipped and not
   * returned as tokens
   */
  public HeaderTokenizer(String header, String delimiters, boolean skipComments)
  {
    throw new UnsupportedOperationException("not implemented");
  }

  /**
   * Return the rest of the Header.
   */
  public String getRemainder()
  {
    throw new UnsupportedOperationException("not implemented");
  }

  /**
   * Parses the next token from this String.  Clients sit in a loop
   * calling next() to parse successive tokens until an EOF Token is
   * returned.
   */
  public HeaderTokenizer.Token next() throws ParseException
  {
    throw new UnsupportedOperationException("not implemented");
  }

  /**
   * Peek at the next token, without actually removing the token from
   * the parse stream. Invoking this method multiple times will return
   * successive tokens, until next() is called.
   */
  public HeaderTokenizer.Token peek() throws ParseException
  {
    throw new UnsupportedOperationException("not implemented");
  }

  /**
   * The Token class represents tokens returned by the HeaderTokenizer.
   */
  public static class Token {

    /**
     * Token type indicating an ATOM.
     * See Also:Constant Field Values
     */
    public static final int ATOM=-1;

    /**
     * Token type indicating a comment. The value field contains the
     * comment string without the comment start and end symbols.  See
     * Also:Constant Field Values
     */
    public static final int COMMENT=-3;

    /**
     * Token type indicating end of input.
     * See Also:Constant Field Values
     */
    public static final int EOF=-4;

    /**
     * Token type indicating a quoted string. The value field contains
     * the string without the quotes.  See Also:Constant Field Values
     */
    public static final int QUOTEDSTRING=-2;

    /**
     * Constructor.
     * type - Token typevalue - Token value
     */
    public Token(int type, String value)
    {
      throw new UnsupportedOperationException("not implemented");
    }

    /**
     * Return the type of the token. If the token represents a
     * delimiter or a control character, the type is that character
     * itself, converted to an integer. Otherwise, it's value is one
     * of the following: ATOM A sequence of ASCII characters delimited
     * by either SPACE, CTL, "(", <"> or the specified SPECIALS
     * QUOTEDSTRING A sequence of ASCII characters within quotes
     * COMMENT A sequence of ASCII characters within "(" and ")". EOF
     * End of header
     */
    public int getType()
    {
      throw new UnsupportedOperationException("not implemented");
    }

    /**
     * Returns the value of the token just read. When the current
     * token is a quoted string, this field contains the body of the
     * string, without the quotes. When the current token is a
     * comment, this field contains the body of the comment.
     */
    public String getValue()
    {
      throw new UnsupportedOperationException("not implemented");
    }

  }
}
