/*
 * Copyright (c) 1998-2009 Caucho Technology -- all rights reserved
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
 * @author Emil Ong
 */

package com.caucho.ant;

import java.io.File;
import java.io.IOException;

import com.caucho.loader.EnvironmentClassLoader;
import com.caucho.server.admin.DeployClient;
import com.caucho.server.admin.TagResult;
import com.caucho.util.QDate;
import com.caucho.vfs.Vfs;

import org.apache.tools.ant.AntClassLoader;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.Path;

/**
 * Ant task to deploy war files to resin
 */
public class ResinUploadWar extends ResinDeployClientTask {
  private String _warFile;
  private String _archive;
  private boolean _isHead = true;
  private boolean _isTest = false;

  /**
   * For ant.
   **/
  public ResinUploadWar()
  {
  }

  public void setWarFile(String warFile)
    throws BuildException
  {
    if (! warFile.endsWith(".war"))
      throw new BuildException("war-file must have .war extension");

    _warFile = warFile;

    if (getContextRoot() == null) {
      int lastSlash = _warFile.lastIndexOf("/");

      if (lastSlash < 0)
        lastSlash = 0;

      setContextRoot(_warFile.substring(lastSlash, 
                                        _warFile.length() - ".war".length()));
    }
  }

  public void setArchive(String tag)
  {
    _archive = tag;
  }

  public void setIsHead(boolean isHead)
  {
    _isHead = isHead;
  }

  public void setIsTest(boolean isTest)
  {
    _isTest = isTest;
  }

  private String getDefaultArchiveTag()
  {
    QDate qDate = new QDate();
    long time = qDate.getTimeOfDay() / 1000;

    StringBuilder sb = new StringBuilder();

    if (_isTest) {
      sb.append("archive/1994-12-01T16:00:00");
    }
    else {
      sb.append("archive/");

      sb.append(qDate.printISO8601Date());

      sb.append('T');
      sb.append((time / 36000) % 10);
      sb.append((time / 3600) % 10);

      sb.append(':');
      sb.append((time / 600) % 6);
      sb.append((time / 60) % 10);

      sb.append(':');
      sb.append((time / 10) % 6);
      sb.append((time / 1) % 10);
    }

    sb.append("/wars/");

    sb.append(getVirtualHost());
    sb.append('/');

    sb.append(getContextRoot());

    if (getVersion() != null) {
      sb.append('-');
      sb.append(getVersion());
    }

    return sb.toString();
  }

  @Override
  protected void validate()
    throws BuildException
  {
    super.validate();

    if (_warFile == null)
      throw new BuildException("war-file is required by " + getTaskName());
  }

  @Override
  protected void doTask(DeployClient client)
    throws BuildException
  {
    try {
      // upload
      com.caucho.vfs.Path path = Vfs.lookup(_warFile);

      String tag = buildVersionedWarTag();
      client.deployJarContents(path, tag, getUser(), getCommitMessage(), 
                               getVersion(), null);

      log("Deployed " + path);
      log("  tag = " + tag);

      // archive

      String archiveTag = _archive;

      if ("true".equals(archiveTag)) {
        archiveTag = getDefaultArchiveTag();
      }
      else if ("false".equals(archiveTag)) {
        archiveTag = null;
      }

      if (archiveTag != null) {
        client.copyTag(archiveTag, tag, 
                       getUser(), getCommitMessage(), getVersion());
        log("  archive tag = " + archiveTag, Project.MSG_VERBOSE);
      }

      // publish (copy tag to head)

      if (getVersion() != null) {
        boolean isHead = _isHead;
        String head = buildWarTag();

        TagResult []tags = client.queryTags(tag);
        TagResult []headTags = client.queryTags(head);
        TagResult []otherTags = client.queryTags(head + "-.*");

        if (tags.length != 1) {
          throw new BuildException("Tag of war file not in repository");
        }

        boolean existingHead = (headTags.length == 1);

        if (existingHead) {
          // ignore isHead == false if updating the "current" version in-place
          if (tags[0].getRoot().equals(headTags[0].getRoot()))
            isHead = true;

          log("  found existing head = " + head, Project.MSG_VERBOSE);
          log("    (hash: " + headTags[0].getRoot() + ")", Project.MSG_VERBOSE);
        }

        if (isHead) {
          if (existingHead) {
            client.removeTag(head, getUser(), getCommitMessage());

            for (TagResult other : otherTags) {
              if (other.getRoot().equals(headTags[0].getRoot())) {
                log("  removing old head version = " + other.getTag(), 
                    Project.MSG_VERBOSE);
                log("    (hash: " + headTags[0].getRoot() + ")", 
                    Project.MSG_VERBOSE);
              }
            }
          }

          client.copyTag(head, tag, 
                         getUser(), getCommitMessage(), getVersion());

          if (existingHead)
            log("  rewrote head = " + head, Project.MSG_VERBOSE);
          else
            log("  wrote head = " + head, Project.MSG_VERBOSE);
        }
      }
    }
    catch (IOException e) {
      throw new BuildException(e);
    }
  }
}
