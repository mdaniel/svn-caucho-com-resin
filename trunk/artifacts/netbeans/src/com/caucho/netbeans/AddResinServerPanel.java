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

import java.awt.event.FocusEvent;
import org.openide.WizardDescriptor;
import org.openide.WizardValidationException;
import org.openide.util.ChangeSupport;
import org.openide.util.HelpCtx;

import javax.swing.*;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.FocusListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AddResinServerPanel extends JPanel {

  private final static Logger log = Logger.getLogger(AddResinServerPanel.class.getName());
  //
  private JComboBox _versionsBox;
  private JButton _downloadButton;
  private JProgressBar _progressBar;
  private JTextField _home;
  private JCheckBox _useHomeAsRootChk;
  private JLabel _rootLbl;
  private JTextField _root;
  private JButton _rootBtn;
  private JTextField _hostName;
  private JTextField _address;
  private JTextField _port;
  private JTextField _webapps;
  private JTextField _user;
  private JTextField _password;
  private JCheckBox _useDefaultConf;
  private JLabel _pluginConf;
  private JLabel _confLbl;
  private JTextField _conf;
  private JButton _confSelect;
  private java.util.List<String> _versions;
  private ChangeSupport _support;
  private WizardDescriptor.ValidatingPanel _panel;
  private WizardDescriptor _wd;
  private String _pluginConfName;

  AddResinServerPanel() {
    init();
  }

  public void init() {
    _support = new ChangeSupport(this);
    //
    setLayout(new GridBagLayout());
    //download: button, choose resin combobox, go button
    GridBagConstraints constraints = new GridBagConstraints();
    constraints.anchor = GridBagConstraints.WEST;
    constraints.gridx = 0;
    constraints.gridy = 0;

    /*
    add(new JToggleButton(new DownloadVersionsAction()), constraints);

    _versionsBox = new JComboBox();
    _versionsBox.setEnabled(false);
    constraints.gridx++;
    constraints.fill = GridBagConstraints.HORIZONTAL;
    constraints.weightx = 1.0;
    add(_versionsBox, constraints);

    _goBtn = new JButton(new DownloadResinAction());
    _goBtn.setEnabled(false);
    constraints.gridx++;
    constraints.weightx = 0;
    constraints.fill = GridBagConstraints.NONE;
    add(_goBtn, constraints);
     */

    //-------
    //resin-home: label, resin home dir,
    constraints.gridx = 0;
    constraints.gridy++;
    constraints.insets.left = constraints.insets.right = 0;
    constraints.insets.top = constraints.insets.bottom = 0;
    constraints.fill = GridBagConstraints.NONE;
    constraints.weightx = 0;
    constraints.anchor = GridBagConstraints.WEST;
    add(new JLabel("Resin Home"), constraints);

    _home = new JTextField();
    constraints.gridx = 1;
    constraints.fill = GridBagConstraints.HORIZONTAL;
    constraints.weightx = 1;
    add(_home, constraints);
    _home.addFocusListener(new FocusListener() {

      @Override
      public void focusGained(FocusEvent fe) {
      }

      @Override
      public void focusLost(FocusEvent fe) {
        if (_root.getText() == null || _root.getText().trim().isEmpty()) {
          _root.setText(_home.getText().trim());
        }
      }
    });

    JButton button = new JButton(new SelectResinHomeAction());
    constraints.gridx = 2;
    constraints.fill = GridBagConstraints.NONE;
    constraints.weightx = 0;
    add(button, constraints);

    //-------
    //download progress bar

    _progressBar = new JProgressBar(JProgressBar.HORIZONTAL);
    _progressBar.setPreferredSize(new Dimension(0, 10));

    constraints.gridx = 1;
    constraints.gridy++;
    constraints.insets.left = constraints.insets.right = 5;
    constraints.insets.top = 5;
    constraints.insets.bottom = 10;
    constraints.fill = GridBagConstraints.HORIZONTAL;
    constraints.weightx = 1;

    add(_progressBar, constraints);
    //-------
    // download button
    _downloadButton = new JButton(new DownloadResinAction());
    constraints.gridx = 1;
    constraints.gridy++;
    constraints.weightx = 0;
    constraints.anchor = GridBagConstraints.EAST;
    add(_downloadButton, constraints);

    //-------
    //resin-root: label, editbox, button
    _rootLbl = new JLabel("Resin root");
    constraints.gridx = 0;
    constraints.gridy++;
    constraints.insets.left = 0;
    constraints.gridwidth = 1;
    constraints.fill = GridBagConstraints.NONE;
    constraints.weightx = 0;
    constraints.anchor = GridBagConstraints.WEST;
    add(_rootLbl, constraints);

    _root = new JTextField();
    constraints.gridx = 1;
    constraints.fill = GridBagConstraints.HORIZONTAL;
    constraints.weightx = 1;
    add(_root, constraints);

    _rootBtn = new JButton("Browse...");
    constraints.gridx = 2;
    constraints.insets.left = 0;
    constraints.fill = GridBagConstraints.NONE;
    constraints.weightx = 0;
    add(_rootBtn, constraints);
    //-------
    //host name: label, editbox
    constraints.gridx = 0;
    constraints.gridy++;
    add(new JLabel("Resin's host name"), constraints);

    _hostName = new JTextField("localhost");
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

    _address = new JTextField("127.0.0.1");
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

    _port = new JTextField("8080");
    constraints.gridx = 1;
    constraints.insets.right = 20;
    constraints.fill = GridBagConstraints.HORIZONTAL;
    constraints.weightx = 1;
    add(_port, constraints);

    //-------
    //http-port: label,editbox
    constraints.gridx = 0;
    constraints.gridy++;
    constraints.insets.right = 0;
    constraints.fill = GridBagConstraints.HORIZONTAL;
    constraints.weightx = 0;
    add(new JLabel("Deploy Directory"), constraints);

    _webapps = new JTextField("webapps");
    constraints.gridx = 1;
    constraints.insets.right = 20;
    constraints.fill = GridBagConstraints.HORIZONTAL;
    constraints.weightx = 1;
    add(_webapps, constraints);


    button = new JButton("Browse...");
    constraints.gridx = 2;
    constraints.insets.left = 0;
    constraints.fill = GridBagConstraints.NONE;
    constraints.weightx = 0;
    add(button, constraints);


    //-------
    //user: label,editbox
/*
    constraints.gridx = 0;
    constraints.gridy++;
    constraints.insets.right = 0;
    constraints.fill = GridBagConstraints.HORIZONTAL;
    constraints.weightx = 0;
    add(new JLabel("User"), constraints);

    _user = new JTextField("admin");
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

    _password = new JTextField("password");
    constraints.gridx = 1;
    constraints.insets.right = 20;
    constraints.fill = GridBagConstraints.HORIZONTAL;
    constraints.weightx = 1;
    add(_password, constraints);
     *
     */

    //-------
    //label: "Select which resin configuration you want to use with this server
 /*
    JLabel label = new JLabel(
    "Select configuration you want to use with this server");
    constraints.gridx = 0;
    constraints.gridy++;
    constraints.insets.right = 0;
    constraints.insets.top = 10;
    constraints.gridwidth = 3;
    constraints.weightx = 1;
    add(label, constraints);
    //-------
    //radio: copy default configuration into the project
    _useDefaultConf = new JCheckBox();
    _useDefaultConf.setSelected(true);
    constraints.gridy++;
    constraints.insets.right = 0;
    constraints.insets.top = 5;
    constraints.gridwidth = 3;
    constraints.weightx = 1;
    add(_useDefaultConf, constraints);

    _pluginConf = new JLabel(" ");
    constraints.gridy++;
    add(_pluginConf, constraints);
    //-------
    //configuration: resin.xml
    _confLbl = new JLabel("Configuration");
    _confLbl.setEnabled(false);
    constraints.gridy++;
    constraints.gridwidth = 1;
    constraints.insets.top = 0;
    constraints.weightx = 0;
    add(_confLbl, constraints);

    _conf = new JTextField();
    _conf.setEnabled(false);
    constraints.gridx = 1;
    constraints.weightx = 1;
    add(_conf, constraints);

    _confSelect = new JButton(new AbstractAction("Select") {

    @Override
    public void actionPerformed(ActionEvent ae) {
    selectConfiguration();
    }
    });
    _confSelect.setEnabled(false);
    constraints.gridx = 2;
    constraints.weightx = 0;
    add(_confSelect, constraints);
    //
    _useDefaultConf.setAction(new AbstractAction(
    "Copy plugin supplied configuration file into Resin Home") {

    @Override
    public void actionPerformed(ActionEvent ae) {
    copyPluginSuppliedConfigrationFile();
    }
    });
     *
     */

    JComponent push = new JPanel();
    constraints.gridx = 1;
    constraints.gridy++;
    constraints.weighty = 1.0;
    constraints.fill = GridBagConstraints.BOTH;

    add(push, constraints);

    //
  }

  public void copyPluginSuppliedConfigrationFile() {
    if (_useDefaultConf.isSelected()) {
      _confLbl.setEnabled(false);
      _conf.setEnabled(false);
      _confSelect.setEnabled(false);
      initPluginConfFileName();
    } else {
      _confLbl.setEnabled(true);
      _conf.setEnabled(true);
      _confSelect.setEnabled(true);
      _pluginConf.setText("Please select configuration file below");
    }
  }

  public void selectConfiguration() {
    String home = _home.getText();
    String confDir = null;

    if (ResinInstance.isResinHome(home)) {
      confDir = home + "/conf";
    }

    JFileChooser chooser;

    if (confDir == null) {
      chooser = new JFileChooser();
    } else {
      chooser = new JFileChooser(confDir);
    }

    chooser.setFileFilter(new FileFilter() {

      @Override
      public String getDescription() {
        return "Accepts files ending with .xml and .conf extensions";
      }

      @Override
      public boolean accept(File file) {
        String path = file.getPath();

        if (path.endsWith(".xml") || path.endsWith(".conf")) {
          return true;
        } else {
          return false;
        }
      }
    });
    chooser.setDialogType(JFileChooser.OPEN_DIALOG);
    chooser.setDialogTitle("Select directory where Resin will be installed");
    chooser.setMultiSelectionEnabled(false);
    chooser.showOpenDialog(this);
    final File dest = chooser.getSelectedFile();

    if (dest != null) {
      _conf.setText(dest.getPath());
    }
  }

  public void initPluginConfFileName() {
    String displayName = _wd.getProperty("ServInstWizard_displayName").toString();
    String confName = ResinInstance.makeConfName(displayName);
    _pluginConfName = confName;
    _pluginConf.setText("Configuration is set to $RESIN_HOME/conf/" + confName);
  }

  public void setWizardDescriptor(WizardDescriptor wd) {
    _wd = wd;
  }

  public void checkInput()
          throws WizardValidationException {
    _wd.putProperty(WizardDescriptor.PROP_WARNING_MESSAGE, null);

    if (_home.getText() == null || _home.getText().isEmpty()) {
      _wd.putProperty(WizardDescriptor.PROP_WARNING_MESSAGE,
              "Supply Resin Home");

      throw new WizardValidationException(_home, "", "");
    }

    if (!ResinInstance.isResinHome(_home.getText())) {
      _wd.putProperty(WizardDescriptor.PROP_WARNING_MESSAGE,
              "Invalid Resin Home");
      throw new WizardValidationException(_home, "", "");
    }

    if (_root.getText() == null || _root.getText().isEmpty()) {
      _wd.putProperty(WizardDescriptor.PROP_WARNING_MESSAGE,
              "Invalid Resin Root");

      throw new WizardValidationException(_root, "", "");
    }

    String address = _address.getText();
    if (address == null || address.isEmpty()) {
      _wd.putProperty(WizardDescriptor.PROP_WARNING_MESSAGE, "Supply Address");

      throw new WizardValidationException(_address, "", "");
    }

    address = address.trim();

    if (address.trim() != "*" && !isValidAddress(address)) {
      _wd.putProperty(WizardDescriptor.PROP_WARNING_MESSAGE, "Invalid Address");

      throw new WizardValidationException(_address, "", "");
    }

    String port = _port.getText();
    try {
      Integer.parseInt(port);
    } catch (Exception e) {
      _wd.putProperty(WizardDescriptor.PROP_WARNING_MESSAGE, "Invalid Port");

      throw new WizardValidationException(_port, "", "");
    }
  }

  private boolean isValidAddress(String address) {
    try {
      InetAddress inetAddress = InetAddress.getByName(address);

      if (inetAddress.isLoopbackAddress()) {
        return true;
      }

      if (inetAddress.isSiteLocalAddress()) {
        return true;
      }

      if (inetAddress.isAnyLocalAddress()) {
        return true;
      }

      if (inetAddress.isLinkLocalAddress()) {
        return true;
      }
    } catch (UnknownHostException ex) {
      log.severe(ex.getMessage());
    }

    return false;
  }

  public String getHome() {
    return _home.getText().trim();
  }

  public String getRoot() {
    return _root.getText().trim();
  }

  public String getHost() {
    return _hostName.getText().trim();
  }

  public String getAddress() {
    return _address.getText().trim();
  }

  public int getPort() {
    return Integer.parseInt(_port.getText().trim());
  }

  public String getUser() {
    return _user.getText().trim();
  }

  public String getPassword() {
    return _password.getText().trim();
  }

  public String getConf() {
    if (_useDefaultConf.isSelected()) {
      return getHome() + "/conf/" + _pluginConfName;
    } else {
      return _conf.getText().trim();
    }
  }

  public String getWebapps() {
    return _webapps.getText();
  }

  public boolean isUsingPluginConfiguration() {
    return _useDefaultConf.isSelected();
  }

  public void readSettings(Object data) {
  }

  public void storeSettings(Object data) {
  }

  public WizardDescriptor.ValidatingPanel getWizardDescriptorPanel() {
    if (_panel == null) {
      _panel = new WizardDescriptor.ValidatingPanel() {

        @Override
        public void validate()
                throws WizardValidationException {
          checkInput();
        }

        @Override
        public void addChangeListener(ChangeListener cl) {
          _support.addChangeListener(cl);
        }

        @Override
        public Component getComponent() {
          return AddResinServerPanel.this;
        }

        @Override
        public HelpCtx getHelp() {
          return HelpCtx.DEFAULT_HELP;
        }

        @Override
        public boolean isValid() {
          return true;
        }

        @Override
        public void readSettings(Object data) {
          AddResinServerPanel.this.readSettings(data);
        }

        @Override
        public void removeChangeListener(ChangeListener cl) {
          _support.removeChangeListener(cl);
        }

        @Override
        public void storeSettings(Object data) {
          AddResinServerPanel.this.storeSettings(data);
        }
      };
    }

    return _panel;
  }

  private String getLatestVersion() throws IOException {
    try {
      URL url = new URL("http://www.caucho.com/download/");
      HttpURLConnection connection = (HttpURLConnection) url.openConnection();

      BufferedReader reader = new BufferedReader(new InputStreamReader(
              connection.getInputStream(), "UTF-8"));

      int c;
      _versions = new ArrayList<String>();
      StringBuilder href = new StringBuilder();

      while ((c = reader.read()) > 0) {
        if ('\"' == c) {
          if (href.length() > 9 && href.charAt(0) == 'r'
                  && href.charAt(1) == 'e' && href.charAt(2) == 's'
                  && href.charAt(3) == 'i' && href.charAt(4) == 'n'
                  && href.charAt(5) == '-'
                  && href.lastIndexOf(".zip") == href.length() - 4
                  && href.charAt(href.length() - 5) != 'c') {
            return href.toString();
          } else {
            href = new StringBuilder();
          }
        } else if (' ' == c || '<' == c || '=' == c || '>' == c || '\n' == c
                || '\r' == c || '\t' == c) {
          href = new StringBuilder();
        } else {
          href.append((char) c);
        }
      }

      throw new RuntimeException("can't retrieve versions");
    } catch (IOException e) {
      log.log(Level.WARNING, e.getMessage(), e);

      throw e;
    }
  }

  private void downloadResin() {
    _downloadButton.setEnabled(false);
    _progressBar.setValue(0);
    _progressBar.setMinimum(0);

    final File file = new File(_home.getText());
    if (!file.isAbsolute()) {
      _wd.putProperty(WizardDescriptor.PROP_WARNING_MESSAGE,
              "Resin Home must be absolute path");

      return;
    }
    if (!file.exists() && !file.mkdirs()) {
      _wd.putProperty(WizardDescriptor.PROP_WARNING_MESSAGE,
              "Can't create directory `" + file + "'");

      return;
    }

    SwingWorker download = new SwingWorker() {

      @Override
      protected Object doInBackground()
              throws Exception {
        asyncDownloadResin(file);

        return null;
      }
    };

    download.execute();
  }

  private void asyncDownloadResin(File dest) {
    InputStream in = null;
    OutputStream out = null;
    JarFile jar = null;

    boolean success = false;
    File resinHome = null;

    try {
      String version = getLatestVersion();
      URL url = new URL("http://www.caucho.com/download/" + version);
      String temp = System.getProperty("java.io.tmpdir");
      HttpURLConnection connection = (HttpURLConnection) url.openConnection();
      final int len = connection.getHeaderFieldInt("Content-Length", 20000000);
      SwingUtilities.invokeLater(new Runnable() {

        public void run() {
          _progressBar.setMaximum(len * 2);
        }
      });
      in = connection.getInputStream();

      String jarFile = temp + File.separatorChar + version;

      out = new FileOutputStream(jarFile);

      byte[] buffer = new byte[65536];

      int bytesRead;
      final int[] x = new int[1];
      Runnable uiTask = new Runnable() {

        public void run() {
          _progressBar.setValue(_progressBar.getValue() + x[0]);
        }
      };

      while ((bytesRead = in.read(buffer)) > 0) {
        out.write(buffer, 0, bytesRead);
        out.flush();
        x[

          0] = bytesRead;
        SwingUtilities.invokeLater(uiTask);
      }
      out.close();
      in.close();

      jar = new JarFile(jarFile);

      Enumeration<JarEntry> entries = jar.entries();

      while (entries.hasMoreElements()) {
        JarEntry entry = entries.nextElement();
        String name = entry.getName();

        name = name.substring(name.indexOf('/') + 1);

        if (entry.isDirectory()) {
          final File file = new File(dest + File.separator + name);
          file.mkdirs();
        } else {
          final File file = new File(dest + File.separator + name);
          file.getParentFile().mkdirs();

          in = jar.getInputStream(entry);
          out = new FileOutputStream(file);
          while ((bytesRead = in.read(buffer)) > 0) {
            out.write(buffer, 0, bytesRead);
            out.flush();
          }

          out.close();
          in.close();
        }

        x[0] = (int) entry.getCompressedSize();
        SwingUtilities.invokeLater(uiTask);
      }

      success = true;
    } catch (Exception e) {
      log.log(Level.WARNING, e.getMessage(), e);
    } finally {
      try {
        if (in != null) {
          in.close();
        }
      } catch (IOException e) {
      }

      try {
        if (out != null) {
          out.close();
        }
      } catch (IOException e) {
      }

      try {
        if (jar != null) {
          jar.close();
        }
      } catch (IOException e) {
        log.log(Level.WARNING, e.getMessage(), e);
      }

      SwingUtilities.invokeLater(new Runnable() {

        public void run() {
          _progressBar.setValue(_progressBar.getMaximum());
          _downloadButton.setEnabled(true);
        }
      });
    }
  }

  private class SelectResinHomeAction extends AbstractAction {

    public SelectResinHomeAction() {
      super("Browse...");
    }

    @Override
    public void actionPerformed(ActionEvent ae) {
      String home = _home.getText();
      if (home == null) {
        home = System.getProperty("user.home");
      }
      JFileChooser chooser = new JFileChooser(home);

      chooser.setDialogType(JFileChooser.OPEN_DIALOG);
      chooser.setDialogTitle("Select directory where Resin will be installed");
      chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
      chooser.setMultiSelectionEnabled(false);
      chooser.showOpenDialog(AddResinServerPanel.this);
      final File dest = chooser.getSelectedFile();

      if (dest == null) {
        return;
      }

      home = dest.getPath();

      _home.setText(home);
      _root.setText(home);
    }
  }

  private class UseResinHomeAsRootAction extends AbstractAction {

    public UseResinHomeAsRootAction() {
      super("Use Resin home as Resin root");
    }

    @Override
    public void actionPerformed(ActionEvent ae) {
      if (_useHomeAsRootChk.isSelected()) {
        _rootLbl.setEnabled(false);
        _root.setText(_home.getText());
        _root.setEnabled(false);
        _rootBtn.setEnabled(false);
      } else {
        _rootLbl.setEnabled(true);
        _root.setEnabled(true);
        _rootBtn.setEnabled(true);
      }
    }
  }

  private class DownloadVersionsAction extends AbstractAction {

    public DownloadVersionsAction() {
      super("Download");
    }

    @Override
    public void actionPerformed(ActionEvent ae) {
    }
  }

  private class DownloadResinAction extends AbstractAction {

    public DownloadResinAction() {
      super("Download");
    }

    @Override
    public void actionPerformed(ActionEvent ae) {
      downloadResin();
    }
  }

  private class SelectWebappsDirectoryAction extends AbstractAction {

    public SelectWebappsDirectoryAction() {
      super("Browse...");
    }

    @Override
    public void actionPerformed(ActionEvent ae) {
      File root = new File(_root.getText());
      File webapps = new File(_webapps.getText());
      File dir = null;
      if (webapps.isAbsolute()) {
        dir = webapps;
      } else {
        dir = root;
      }

      if (!dir.exists()) {
        dir = new File(System.getProperty("user.home"));
      }

      JFileChooser chooser = new JFileChooser(dir);

      chooser.setDialogType(JFileChooser.OPEN_DIALOG);
      chooser.setDialogTitle("Select directory where Resin will deploy webapps");
      chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
      chooser.setMultiSelectionEnabled(false);
      chooser.showOpenDialog(AddResinServerPanel.this);
      final File dest = chooser.getSelectedFile();

      if (dest == null) {
        return;
      }

      String deployDir = dest.getPath();

      _webapps.setText(deployDir);
    }
  }
}
