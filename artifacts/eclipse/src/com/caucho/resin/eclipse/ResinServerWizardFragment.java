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

package com.caucho.resin.eclipse;

import org.eclipse.jst.server.generic.core.internal.GenericServer;
import org.eclipse.jst.server.generic.core.internal.GenericServerRuntime;
import org.eclipse.jst.server.generic.ui.internal.GenericServerWizardFragment;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.wst.common.project.facet.core.runtime.IRuntime;
import org.eclipse.wst.server.core.IServerWorkingCopy;
import org.eclipse.wst.server.core.TaskModel;
import org.eclipse.wst.server.ui.wizard.IWizardHandle;

@SuppressWarnings("restriction")
public class ResinServerWizardFragment extends GenericServerWizardFragment 
                                       implements ResinPropertyIds
{
  public static final String SERVER_PROPERTIES_COMPLETE =
    "resin.server.properties.complete";
  
  private String _resinConfType = ResinServer.RESIN_CONF_BUNDLE;
  private Text _resinHomeTextField = null;
  private Text _resinRootTextField = null;
  private Text _userConfTextField = null;
  
  @Override
  public void createContent(final Composite parent, IWizardHandle handle)
  {
    createResinPathsContent(parent);
    super.createContent(parent, handle);
    createConfigContent(parent);
  }
  
  private void createResinPathsContent(final Composite parent)
  {
    // layout
    Composite composite = new Composite(parent, SWT.NONE); 
    composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
    composite.setLayout(new GridLayout(3, false));
    
    Label resinHomeLabel = new Label(composite, SWT.NONE);
    resinHomeLabel.setText("Resin Home");
    
    _resinHomeTextField = new Text(composite, 
                                   SWT.SINGLE 
                                   | SWT.SHADOW_IN 
                                   | SWT.BORDER);
    _resinHomeTextField.setText("/usr/share/resin");
    _resinHomeTextField.setLayoutData(new GridData(SWT.FILL, SWT.FILL, 
                                                   true, true,
                                                   1, 1));
    
    final Button resinHomeBrowseButton = new Button(composite, SWT.PUSH);
    resinHomeBrowseButton.setText("Browse");

    if (isChangeableRoot()) {
      final Button resinRootButton = new Button(composite, SWT.CHECK);
      resinRootButton.setText("Use Resin home as Resin root");
      resinRootButton.setLayoutData(new GridData(SWT.FILL, SWT.FILL, 
                                                 true, false,
                                                 3, 1));
      resinRootButton.setSelection(true);

      final Label resinRootLabel = new Label(composite, SWT.NONE);
      resinRootLabel.setText("Resin Root");
      resinRootLabel.setEnabled(false);
    
    
      _resinRootTextField = new Text(composite, 
                                     SWT.SINGLE 
                                     | SWT.SHADOW_IN 
                                     | SWT.BORDER);
      _resinRootTextField.setText("/usr/share/resin");
      _resinRootTextField.setEnabled(false);
      _resinRootTextField.setLayoutData(new GridData(SWT.FILL, SWT.FILL, 
                                                     true, true,
                                                     1, 1));

      final Button resinRootBrowseButton = new Button(composite, SWT.PUSH);
      resinRootBrowseButton.setText("Browse");
      resinRootBrowseButton.setEnabled(false);
      
    // listeners
    
      _resinHomeTextField.addModifyListener(new ModifyListener() {
        public void modifyText(ModifyEvent event)
        {
          if (resinRootButton.getSelection())
            _resinRootTextField.setText(_resinHomeTextField.getText());
        }
      });

      resinRootButton.addSelectionListener(new SelectionListener() {
        public void widgetSelected(SelectionEvent e) {
          if (resinRootButton.getSelection()) {
            resinRootLabel.setEnabled(false);
            _resinRootTextField.setEnabled(false);
            resinRootBrowseButton.setEnabled(false);

            _resinRootTextField.setText(_resinHomeTextField.getText());
          }
          else {
            resinRootLabel.setEnabled(true);
            _resinRootTextField.setEnabled(true);
            resinRootBrowseButton.setEnabled(true);
          }       
        }

        public void widgetDefaultSelected(SelectionEvent e) {
          widgetSelected(e);
        }
      });

      resinRootBrowseButton.addSelectionListener(new SelectionListener() {
        public void widgetSelected(SelectionEvent e) {
          DirectoryDialog dialog = new DirectoryDialog(parent.getShell());
          String currentText = 
            _resinRootTextField.getText().replace('\\', '/');
          dialog.setFilterPath(currentText);
          String filename = dialog.open();

          if (filename != null)
            _resinRootTextField.setText(filename.replace('\\', '/'));
        }

        public void widgetDefaultSelected(SelectionEvent e) {
          widgetSelected(e);
        }
      });
    }
    
    resinHomeBrowseButton.addSelectionListener(new SelectionListener() {
      public void widgetSelected(SelectionEvent e) {
        DirectoryDialog dialog = new DirectoryDialog(parent.getShell());
        String currentText = 
          _resinHomeTextField.getText().replace('\\', '/');
        dialog.setFilterPath(currentText);
        String filename = dialog.open();

        if (filename != null)
          _resinHomeTextField.setText(filename.replace('\\', '/'));
      }

      public void widgetDefaultSelected(SelectionEvent e) {
        widgetSelected(e);
      }
    });
  }
    
  private void createConfigContent(final Composite parent)
  { 
    // layout
    Composite composite = new Composite(parent, SWT.NONE); 
    composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
    composite.setLayout(new GridLayout(3, false));
    
    Label label = new Label(composite, SWT.NONE);
    label.setText("Select which Resin configuration you want to use with this server:");
    label.setLayoutData(new GridData(SWT.FILL, SWT.FILL, 
                                     true, false,
                                     3, 1));
    
    final Button projectConfig = new Button(composite, SWT.RADIO);
    projectConfig.setText("Copy default configuration into project");
    projectConfig.setLayoutData(new GridData(SWT.FILL, SWT.FILL, 
                                             true, false,
                                             3, 1));
    projectConfig.setSelection(true);
    
    final Button resinHomeConfig = new Button(composite, SWT.RADIO);
    resinHomeConfig.setLayoutData(new GridData(SWT.FILL, SWT.FILL, 
                                               true, false,
                                               3, 1));
    resinHomeConfig.setText("Use configuration in Resin Home"); 
    
    final Button userConfig = new Button(composite, SWT.RADIO);
    userConfig.setText("Use a configuration file from another location");                               

    _userConfTextField = new Text(composite, 
                                  SWT.SINGLE 
                                  | SWT.SHADOW_IN 
                                  | SWT.BORDER);
    _userConfTextField.setText("/path/to/resin.conf_or_resin.xml");
    _userConfTextField.setEnabled(false);
    
    final Button userBrowseButton = new Button(composite, SWT.PUSH);
    userBrowseButton.setText("Browse");
    userBrowseButton.setEnabled(false);

    // listeners

    projectConfig.addSelectionListener(new SelectionListener() {
      public void widgetSelected(SelectionEvent e) {
        userBrowseButton.setEnabled(false);
        _userConfTextField.setEnabled(false);
        
        _resinConfType = ResinServer.RESIN_CONF_BUNDLE;
      }

      public void widgetDefaultSelected(SelectionEvent e) {
        widgetSelected(e);
      }
    });

    resinHomeConfig.addSelectionListener(new SelectionListener() {
      public void widgetSelected(SelectionEvent e) {
        userBrowseButton.setEnabled(false);
        _userConfTextField.setEnabled(false);
        
        _resinConfType = ResinServer.RESIN_CONF_RESIN_HOME;
      }

      public void widgetDefaultSelected(SelectionEvent e) {
        widgetSelected(e);
      }
    });

    userConfig.addSelectionListener(new SelectionListener() {
      public void widgetSelected(SelectionEvent e) {
        userBrowseButton.setEnabled(true);
        _userConfTextField.setEnabled(true);
        
        _resinConfType = ResinServer.RESIN_CONF_USER;        
      }

      public void widgetDefaultSelected(SelectionEvent e) {
        widgetSelected(e);
      }
    });
    
    userBrowseButton.addSelectionListener(new SelectionListener() {
      public void widgetSelected(SelectionEvent e) {
        FileDialog dialog = new FileDialog(parent.getShell());
        dialog.setFileName(_userConfTextField.getText().replace('\\', '/'));
        String filename = dialog.open();
        
        if (filename != null)
          _userConfTextField.setText(filename.replace('\\', '/'));
      }

      public void widgetDefaultSelected(SelectionEvent e) {
        widgetSelected(e);
      }
    });
  }

  private IServerWorkingCopy getServer() 
  {
    IServerWorkingCopy server = 
      (IServerWorkingCopy) getTaskModel().getObject(TaskModel.TASK_SERVER);
    
    return server;
  }
 
  private ResinServer getResinServer()
  {
    return (ResinServer) getServer().loadAdapter(GenericServer.class, null);
  }
  
  private void setProperty(String key, String value)
  {
    getResinServer().getServerInstanceProperties().put(key, value);
   
    // without setting the properties in both places, certain resolutions
    // will fail, e.g. resin.home for the class path.
    GenericServerRuntime runtime = getResinServer().getRuntimeDelegate();
    runtime.getServerInstanceProperties().put(key, value);
  }
  
  private String getProperty(String key)
  {
    return (String) getResinServer().getServerInstanceProperties().get(key);
  }
  
  private boolean isChangeableRoot()
  {
    ResinServer server = getResinServer();
    String changeableRoot = 
      server.getPropertyDefault(ResinPropertyIds.CHANGEABLE_ROOT); 
     
    return "true".equals(changeableRoot);
  }
  
  @Override
  public void exit()
  {
    super.exit();
    
    setProperty(ResinPropertyIds.RESIN_HOME, _resinHomeTextField.getText());
    
    if (isChangeableRoot())
      setProperty(ResinPropertyIds.RESIN_ROOT, _resinRootTextField.getText());

    setProperty(ResinServer.RESIN_CONF_TYPE, _resinConfType);
    
    if (ResinServer.RESIN_CONF_USER.equals(_resinConfType)) {
      setProperty(ResinServer.RESIN_CONF_USER_LOCATION, 
                  _userConfTextField.getText());
    }
    
    setProperty(SERVER_PROPERTIES_COMPLETE, "true");
    
    getResinServer().getRuntimeDelegate().validate();
  }
}
