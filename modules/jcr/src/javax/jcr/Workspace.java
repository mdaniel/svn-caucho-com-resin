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

package javax.jcr;

import java.io.*;

import org.xml.sax.*;

import javax.jcr.lock.*;
import javax.jcr.nodetype.*;
import javax.jcr.observation.*;
import javax.jcr.query.*;
import javax.jcr.version.*;

public interface Workspace {
  public Session getSession();
  
  public String getName();
  
  public void copy(String srcAbsPath, String destAbsPath)
    throws ConstraintViolationException,
	   VersionException,
	   AccessDeniedException,
	   PathNotFoundException,
	   ItemExistsException,
	   LockException,
	   RepositoryException;
  
  public void copy(String srcWorkspace,
		   String srcAbsPath,
		   String destAbsPath)
    throws NoSuchWorkspaceException,
	   ConstraintViolationException,
	   VersionException,
	   AccessDeniedException,
	   PathNotFoundException,
	   ItemExistsException,
	   LockException,
	   RepositoryException;
  
  public void clone(String srcWorkspace,
		    String srcAbsPath,
		    String destAbsPath,
		    boolean removeExisting)
    throws NoSuchWorkspaceException,
	   ConstraintViolationException,
	   VersionException,
	   AccessDeniedException,
	   PathNotFoundException,
	   ItemExistsException,
	   LockException,
	   RepositoryException;
  
  public void move(String srcAbsPath, String destAbsPath)
    throws ConstraintViolationException,
	   VersionException,
	   AccessDeniedException,
	   PathNotFoundException,
	   ItemExistsException,
	   LockException,
	   RepositoryException;
  
  public void restore(Version[] versions, boolean removeExisting)
    throws ItemExistsException,
	   UnsupportedRepositoryOperationException,
	   VersionException,
	   LockException,
	   InvalidItemStateException,
	   RepositoryException;
  
  public QueryManager getQueryManager()
    throws RepositoryException;
  
  public NamespaceRegistry getNamespaceRegistry()
    throws RepositoryException;
  
  public NodeTypeManager getNodeTypeManager()
    throws RepositoryException;
  
  public ObservationManager getObservationManager()
    throws UnsupportedRepositoryOperationException,
	   RepositoryException;
  
  public String[] getAccessibleWorkspaceNames()
    throws RepositoryException;
  
  public ContentHandler getImportContentHandler(String parentAbsPath,
						int uuidBehavior)
    throws PathNotFoundException,
	   ConstraintViolationException,
	   VersionException,
	   LockException,
	   AccessDeniedException,
	   RepositoryException;
  
  public void importXML(String parentAbsPath,
			InputStream in,
			int uuidBehavior)
    throws IOException,
	   PathNotFoundException,
	   ItemExistsException,
	   ConstraintViolationException,
	   InvalidSerializedDataException,
	   LockException,
	   AccessDeniedException,
	   RepositoryException;
}
