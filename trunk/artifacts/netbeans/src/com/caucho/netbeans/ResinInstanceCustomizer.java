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

import org.openide.WizardDescriptor;
import org.openide.util.ChangeSupport;

import javax.swing.*;
import java.awt.*;
import java.util.logging.Logger;

public class ResinInstanceCustomizer extends JPanel
{

  private final static Logger log
    = Logger.getLogger(AddResinServerPanel.class.getName());
  //
  private JComboBox _versionsBox;
  private JButton _goBtn;
  private JProgressBar _progressBar;
  private JTextField _home;
  private JCheckBox _useHomeAsRootChk;
  private JLabel _rootLbl;
  private JTextField _root;
  private JButton _rootBtn;
  private JTextField _hostName;
  private JTextField _address;
  private JTextField _port;
  private JTextField _user;
  private JTextField _password;
  private JTextField _conf;
  private java.util.List<String> _versions;
  private ChangeSupport _support;
  private WizardDescriptor.ValidatingPanel _panel;
  private WizardDescriptor _wd;

  ResinInstanceCustomizer(ResinInstance resin)
  {
    init(resin);
  }

  public void init(ResinInstance resin)
  {
    _support = new ChangeSupport(this);
    //
    setLayout(new GridBagLayout());
    //download: button, choose resin combobox, go button
    GridBagConstraints constraints = new GridBagConstraints();
    constraints.anchor = GridBagConstraints.WEST;
    constraints.gridx = 0;
    constraints.gridy = 0;

    //-------
    //resin-home: label, resin home dir,
    constraints.gridx = 0;
    constraints.gridy++;
    constraints.insets.left = constraints.insets.right = 0;
    constraints.insets.top = constraints.insets.bottom = 0;
    constraints.fill = GridBagConstraints.NONE;
    constraints.weightx = 0;
    add(new JLabel("Resin Home"), constraints);

    _home = new JTextField(resin.getHome());
    _home.setEnabled(false);
    constraints.gridx = 1;
    constraints.fill = GridBagConstraints.HORIZONTAL;
    constraints.weightx = 1;
    add(_home, constraints);

    /*    JButton button = new JButton(new SelectResinHomeAction());
    constraints.gridx = 2;
    constraints.fill = GridBagConstraints.NONE;
    constraints.weightx = 0;
    add(button, constraints);
     */
    //-------
    //resin-root: label, editbox, button
    _rootLbl = new JLabel("Resin root");
    constraints.gridx = 0;
    constraints.gridy++;
    constraints.insets.left = 0;
    constraints.gridwidth = 1;
    constraints.fill = GridBagConstraints.NONE;
    constraints.weightx = 0;
    add(_rootLbl, constraints);

    _root = new JTextField(resin.getRoot());
    _root.setEnabled(false);
    constraints.gridx = 1;
    constraints.fill = GridBagConstraints.HORIZONTAL;
    constraints.weightx = 1;
    add(_root, constraints);

    /*
    _rootBtn = new JButton("...");
    _rootBtn.setEnabled(false);
    constraints.gridx = 2;
    constraints.insets.left = 0;
    constraints.fill = GridBagConstraints.NONE;
    constraints.weightx = 0;
    add(_rootBtn, constraints);
     */

    //-------
    //host name: label, editbox
    constraints.gridx = 0;
    constraints.gridy++;
    add(new JLabel("Resin's host name"), constraints);

    _hostName = new JTextField(resin.getHost());
    _hostName.setEnabled(false);
    constraints.gridx = 1;
    constraints.insets.right = 20;
    constraints.fill = GridBagConstraints.HORIZONTAL;
    constraints.weightx = 1;
    add(_hostName, constraints);
    //-------
    //address/ip: label, editbox
    constraints.gridx = 0;
    constraints.gridy++;
    constraints.insets.right = 0;
    constraints.fill = GridBagConstraints.NONE;
    constraints.weightx = 0;
    add(new JLabel("Address"), constraints);

    _address = new JTextField(resin.getAddress());
    _address.setEnabled(false);
    constraints.gridx = 1;
    constraints.insets.right = 20;
    constraints.fill = GridBagConstraints.HORIZONTAL;
    constraints.weightx = 1;
    add(_address, constraints);

    //-------
    //http-port: label,editbox
    constraints.gridx = 0;
    constraints.gridy++;
    constraints.insets.right = 0;
    constraints.fill = GridBagConstraints.HORIZONTAL;
    constraints.weightx = 0;
    add(new JLabel("Port"), constraints);

    _port = new JTextField(Integer.toString(resin.getPort()));
    _port.setEnabled(false);
    constraints.gridx = 1;
    constraints.insets.right = 20;
    constraints.fill = GridBagConstraints.HORIZONTAL;
    constraints.weightx = 1;
    add(_port, constraints);

    //-------
    //user: label,editbox
    constraints.gridx = 0;
    constraints.gridy++;
    constraints.insets.right = 0;
    constraints.fill = GridBagConstraints.HORIZONTAL;
    constraints.weightx = 0;
    add(new JLabel("User"), constraints);

    _user = new JTextField(resin.getUser());
    _user.setEnabled(false);
    constraints.gridx = 1;
    constraints.insets.right = 20;
    constraints.fill = GridBagConstraints.HORIZONTAL;
    constraints.weightx = 1;
    add(_user, constraints);

    //password: label,editbox
    constraints.gridx = 0;
    constraints.gridy++;
    constraints.insets.right = 0;
    constraints.fill = GridBagConstraints.HORIZONTAL;
    constraints.weightx = 0;
    add(new JLabel("Password"), constraints);

    _password = new JTextField(resin.getPassword());
    _password.setEnabled(false);
    constraints.gridx = 1;
    constraints.insets.right = 20;
    constraints.fill = GridBagConstraints.HORIZONTAL;
    constraints.weightx = 1;
    add(_password, constraints);

    //configuration: resin.xml
    JLabel confLbl = new JLabel("Configuration");
    constraints.gridy++;
    constraints.gridx = 0;
    constraints.gridwidth = 1;
    constraints.insets.top = 0;
    constraints.weightx = 0;
    add(confLbl, constraints);

    _conf = new JTextField(resin.getConf());
    _conf.setEnabled(false);
    constraints.gridx = 1;
    constraints.weightx = 1;
    add(_conf, constraints);

    //-------
    //label: "Select which resin configuration you want to use with this server
    //-------
    //radio: copy default configuration into the project
    //-------
    //use configuration in resin-home
    //
  }
}
