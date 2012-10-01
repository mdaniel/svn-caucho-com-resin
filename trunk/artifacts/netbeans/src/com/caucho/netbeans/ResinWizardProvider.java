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
 * @author Alex Rojkov
 */
package com.caucho.netbeans;

import org.netbeans.api.server.ServerInstance;
import org.netbeans.spi.server.ServerWizardProvider;
import org.openide.WizardDescriptor;
import org.openide.WizardDescriptor.InstantiatingIterator;
import org.openide.WizardDescriptor.Panel;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

public class ResinWizardProvider implements ServerWizardProvider {

  private final static Logger log = Logger.getLogger(ResinWizardProvider.class.getName());
  private static ResinWizardProvider _instance;
  private NewResinWizardPanels _instantiatingIterator;
  private static PluginL10N L = new PluginL10N(ResinWizardProvider.class);

  public final static ResinWizardProvider getInstance() {

    if (_instance == null) {
      _instance = new ResinWizardProvider();
    }

    return _instance;
  }

  private ResinWizardProvider() {
    log.finest("creating a new instance of "
            + ResinWizardProvider.class.getName());
  }

  // ------------------------------------------------------------------------
  // ServerWizardProvider interface implementation
  // ------------------------------------------------------------------------
  @Override
  public String getDisplayName() {
    return "Resin";
  }

  @Override
  public InstantiatingIterator getInstantiatingIterator() {
    if (_instantiatingIterator == null) {
      _instantiatingIterator = new NewResinWizardPanels(new AddResinServerPanel());
    }

    return _instantiatingIterator;
  }
}

class NewResinWizardPanels<Data> extends WizardDescriptor.ArrayIterator
        implements InstantiatingIterator {

  private final static Logger log = Logger.getLogger(NewResinWizardPanels.class.getName());
  private final AddResinServerPanel _addResinServerPanel;
  private WizardDescriptor _wizardDescriptor;

  public NewResinWizardPanels(AddResinServerPanel addResinServerPanel) {
    super(new Panel[]{addResinServerPanel.getWizardDescriptorPanel()});
    _addResinServerPanel = addResinServerPanel;

    log.finest("creating " + NewResinWizardPanels.class.getSimpleName());
  }

  @Override
  public Set instantiate()
          throws IOException {
    String displayName = (String) _wizardDescriptor.getProperty(
            "ServInstWizard_displayName");
    String home = _addResinServerPanel.getHome();
    String root = _addResinServerPanel.getRoot();
    String host = _addResinServerPanel.getHost();
    String address = _addResinServerPanel.getAddress();
    int port = _addResinServerPanel.getPort();
    String user = "";//_addResinServerPanel.getUser();
    String password = "";//_addResinServerPanel.getPassword();
    String conf = "";
    String webapps = _addResinServerPanel.getWebapps();

    ResinInstance resin = new ResinInstance(displayName,
            home,
            root,
            host,
            address,
            port,
            user,
            password,
            conf,
            webapps);

    ServerInstance server = ResinInstanceProvider.getInstance().instantiate(
            resin);
    resin.setServerInstance(server);

    HashSet result = new HashSet();
    result.add(server);

    return result;
  }

  @Override
  public Panel current() {
//    _addResinServerPanel.initPluginConfFileName();
    return super.current();
  }

  @Override
  public void initialize(WizardDescriptor wd) {
    //org.netbeans.modules.server.ui.wizard.AddServerInstanceWizard.
    _wizardDescriptor = wd;
    _addResinServerPanel.setWizardDescriptor(wd);
  }

  @Override
  public void uninitialize(WizardDescriptor wd) {
    _wizardDescriptor = null;
  }
}
