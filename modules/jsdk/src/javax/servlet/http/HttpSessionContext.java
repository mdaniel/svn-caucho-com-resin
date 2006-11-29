/*
 * Copyright 1998-1998 Caucho Technology -- all rights reserved
 *
 * Caucho Technology forbids redistribution of any part of this software
 * in any form, including derived works and generated binaries.
 *
 * This Software is provided "AS IS," without a warranty of any kind. 
 * ALL EXPRESS OR IMPLIED REPRESENTATIONS AND WARRANTIES, INCLUDING ANY
 * IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE
 * OR NON-INFRINGEMENT, ARE HEREBY EXCLUDED.

 * CAUCHO TECHNOLOGY AND ITS LICENSORS SHALL NOT BE LIABLE FOR ANY DAMAGES
 * SUFFERED BY LICENSEE OR ANY THIRD PARTY AS A RESULT OF USING OR
 * DISTRIBUTING SOFTWARE. IN NO EVENT WILL CAUCHO OR ITS LICENSORS BE LIABLE
 * FOR ANY LOST REVENUE, PROFIT OR DATA, OR FOR DIRECT, INDIRECT, SPECIAL,
 * CONSEQUENTIAL, INCIDENTAL OR PUNITIVE DAMAGES, HOWEVER CAUSED AND
 * REGARDLESS OF THE THEORY OF LIABILITY, ARISING OUT OF THE USE OF OR
 * INABILITY TO USE SOFTWARE, EVEN IF HE HAS BEEN ADVISED OF THE POSSIBILITY
 * OF SUCH DAMAGES.      
 *
 * $Id: HttpSessionContext.java,v 1.1.1.1 2004/09/11 05:06:23 cvs Exp $
 */

package javax.servlet.http;

import java.util.Enumeration;

/**
 * @deprecated
 */
public interface HttpSessionContext {
  /**
   * @deprecated
   */
  public Enumeration getIds();
  /**
   * @deprecated
   */
  public HttpSession getSession(String id);
}
