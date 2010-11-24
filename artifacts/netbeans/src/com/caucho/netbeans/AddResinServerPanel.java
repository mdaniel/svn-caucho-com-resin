/*
 * Copyright (c) 1998-2010 Caucho Technology -- all rights reserved
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

import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent;
import java.net.UnknownHostException;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
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
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import org.openide.WizardDescriptor;
import org.openide.WizardValidationException;
import org.openide.util.ChangeSupport;
import org.openide.util.Exceptions;
import org.openide.util.HelpCtx;

public class AddResinServerPanel extends JPanel {

  private final static Logger log = Logger.getLogger(AddResinServerPanel.class.getName());
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
  private java.util.List<String> _versions;
  private ChangeSupport _support;
  private WizardDescriptor.ValidatingPanel _panel;
  private WizardDescriptor _wd;

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
    //resin-home: label, resin home dir,
    constraints.gridx = 0;
    constraints.gridy++;
    constraints.insets.left = constraints.insets.right = 0;
    constraints.insets.top = constraints.insets.bottom = 0;
    constraints.fill = GridBagConstraints.NONE;
    constraints.weightx = 0;
    add(new JLabel("Resin Home"), constraints);

    _home = new JTextField();
    constraints.gridx = 1;
    constraints.fill = GridBagConstraints.HORIZONTAL;
    constraints.weightx = 1;
    add(_home, constraints);

    JButton button = new JButton(new SelectResinHomeAction());
    constraints.gridx = 2;
    constraints.fill = GridBagConstraints.NONE;
    constraints.weightx = 0;
    add(button, constraints);

    //-------
    //resin-root-checkbox (use home as root)
    _useHomeAsRootChk = new JCheckBox(new UseResinHomeAsRootAction());
    _useHomeAsRootChk.setSelected(true);
    constraints.gridx = 0;
    constraints.gridy++;
    constraints.gridwidth = 3;

    constraints.insets.left = 10;
    add(_useHomeAsRootChk, constraints);
    //-------
    //resin-root: label, editbox, button
    _rootLbl = new JLabel("Resin root");
    _rootLbl.setEnabled(false);
    constraints.gridx = 0;
    constraints.gridy++;
    constraints.insets.left = 0;
    constraints.gridwidth = 1;
    constraints.fill = GridBagConstraints.NONE;
    constraints.weightx = 0;
    add(_rootLbl, constraints);

    _root = new JTextField();
    _root.setEnabled(false);
    constraints.gridx = 1;
    constraints.fill = GridBagConstraints.HORIZONTAL;
    constraints.weightx = 1;
    add(_root, constraints);

    _rootBtn = new JButton("...");
    _rootBtn.setEnabled(false);
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
    //label: "Select which resin configuration you want to use with this server
    //-------
    //radio: copy default configuration into the project
    //-------
    //use configuration in resin-home
    //
  }

  public void setWizardDescriptor(WizardDescriptor wd) {
    _wd = wd;
  }

  public void checkInput() throws WizardValidationException {
    _wd.putProperty(WizardDescriptor.PROP_WARNING_MESSAGE, null);

    if (_home.getText() == null || _home.getText().isEmpty()) {
      _wd.putProperty(WizardDescriptor.PROP_WARNING_MESSAGE, "Supply Resin Home");

      throw new WizardValidationException(_home, "", "");
    }

    if (!ResinInstance.isResinHome(_home.getText())) {
      _wd.putProperty(WizardDescriptor.PROP_WARNING_MESSAGE, "Invalid Resin Home");

      throw new WizardValidationException(_home, "", "");
    }

    if (_root.getText() == null || _root.getText().isEmpty()) {
      _wd.putProperty(WizardDescriptor.PROP_WARNING_MESSAGE, "Invalid Resin Root");

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

  public void readSettings(Object data) {
  }

  public void storeSettings(Object data) {
  }

  public WizardDescriptor.ValidatingPanel getWizardDescriptorPanel() {
    if (_panel == null) {
      _panel = new WizardDescriptor.ValidatingPanel() {

        @Override
        public void validate() throws WizardValidationException {
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

  void downloadVersions() {
    _progressBar.setMinimum(0);
    _progressBar.setMaximum(10);
    _progressBar.setVisible(true);

    SwingWorker download = new SwingWorker() {

      @Override
      protected Object doInBackground() throws Exception {
        asyncDownloadVersions();

        return null;
      }
    };

    download.execute();
  }

  private void asyncDownloadVersions() {
    try {
      //monitor.beginTask("Download Resin Versions", 1);
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
            _versions.add(href.toString());

            SwingUtilities.invokeLater(new Runnable() {

              public void run() {
                _progressBar.setValue(_progressBar.getValue() + 1);
              }
            });
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

      Runnable uiTask = new Runnable() {

        public void run() {
          _versionsBox.removeAllItems();
          if (_versions.size() == 0) {
            _versionsBox.addItem("Please download manually.");
            _versionsBox.setSelectedIndex(0);
          } else {
            for (String version : _versions) {
              StringBuilder v = new StringBuilder();
              boolean pro = false;
              boolean snap = false;
              char[] chars = version.toCharArray();
              for (int i = 0; i < chars.length; i++) {
                char c = chars[i];
                if (Character.isDigit(c)) {
                  v.append(c);
                } else if ('_' == c || '.' == c) {
                  v.append('.');
                } else if (c == 'o' && chars[i - 2] == 'p') {
                  pro = true;
                } else if (c == 'p' && chars[i - 3] == 's') {
                  snap = true;
                }
              }

              String item = "Resin " + (pro ? "Pro " : " ")
                      + v.substring(0, v.length() - 1) + (snap ? " Snapshot" : "");
              _versionsBox.addItem(item);
            }

            _versionsBox.setSelectedIndex(0);
            _versionsBox.setEnabled(true);
          }

          _goBtn.setEnabled(true);

          //monitor.done();
        }
      };

      SwingUtilities.invokeLater(uiTask);
    } catch (final Exception e) {
      log.log(Level.WARNING, e.getMessage(), e);

      Runnable uiTask = new Runnable() {

        public void run() {
          JDialog dialog = new JDialog();
          dialog.add(new JLabel("Can not connect to Caucho.com"));
          dialog.setVisible(true);
        }
      };

      SwingUtilities.invokeLater(uiTask);
    }
  }

  private void downloadResin() {
    JFileChooser chooser = new JFileChooser(System.getProperty("user.home"));
    chooser.setDialogType(JFileChooser.OPEN_DIALOG);
    chooser.setDialogTitle("Select directory where Resin will be installed");
    chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
    chooser.setMultiSelectionEnabled(false);
    chooser.showOpenDialog(this);
    final File dest = chooser.getSelectedFile();

    if (dest == null) {
      return;
    }

    _goBtn.setEnabled(false);
    _progressBar.setValue(0);
    _progressBar.setMinimum(0);

    final String version = _versions.get(_versionsBox.getSelectedIndex());

    SwingWorker download = new SwingWorker() {

      @Override
      protected Object doInBackground() throws Exception {
        asyncDownloadResin(version, dest);

        return null;
      }
    };

    download.execute();
  }

  private void asyncDownloadResin(
          String version, File dest) {
    InputStream in = null;
    OutputStream out = null;
    JarFile jar = null;
    boolean success = false;
    File resinHome = null;
    try {
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
        x[0] = bytesRead;
        SwingUtilities.invokeLater(uiTask);
      }
      out.close();
      in.close();

      jar = new JarFile(jarFile);

      Enumeration<JarEntry> entries = jar.entries();
      while (entries.hasMoreElements()) {
        JarEntry entry = entries.nextElement();
        if (entry.isDirectory() && resinHome == null) {
          StringBuilder path = new StringBuilder();
          char[] name = entry.getName().toCharArray();
          for (char c : name) {
            if (c == '/' || c == '\\') {
              break;
            } else {
              path.append(c);
            }
          }

          resinHome = new File(dest, path.toString());

          continue;
        } else if (entry.isDirectory()) {
          continue;
        }

        String name = entry.getName();
        File file = new File(dest + File.separator + name);
        file.getParentFile().mkdirs();

        in = jar.getInputStream(entry);
        out = new FileOutputStream(file);
        while ((bytesRead = in.read(buffer)) > 0) {
          out.write(buffer, 0, bytesRead);
          out.flush();
        }

        out.close();
        in.close();

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

      final String resinHomeDir = resinHome.toString();
      SwingUtilities.invokeLater(new Runnable() {

        public void run() {
          _progressBar.setValue(_progressBar.getMaximum());
          if (resinHomeDir != null) {
            _home.setText(resinHomeDir);
            _root.setText(resinHomeDir);
          }
        }
      });
    }

    if (!success) {
      /*
      display.asyncExec(new Runnable() {

      public void run() {
      // fetch manually then
      int style = IWorkbenchBrowserSupport.AS_EXTERNAL
      | IWorkbenchBrowserSupport.LOCATION_BAR
      | IWorkbenchBrowserSupport.STATUS;
      IWebBrowser browser;
      try {
      browser = WorkbenchBrowserSupport.getInstance().createBrowser(
      style, "", "", "");
      browser.openURL(new URL("http://www.caucho.com/download/"));
      } catch (PartInitException e) {
      _log.log(new Status(Status.ERROR, resinPlugin, 0, e.getMessage(), e));
      } catch (MalformedURLException e) {
      _log.log(new Status(Status.ERROR, resinPlugin, 0, e.getMessage(), e));
      }
      return;
      }
      });
       */
    }
  }

  private class SelectResinHomeAction extends AbstractAction {

    public SelectResinHomeAction() {
      super("...");
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
      if (_useHomeAsRootChk.isSelected()) {
        _root.setText(home);
      }
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
      ((JToggleButton) ae.getSource()).setEnabled(false);
      downloadVersions();
    }
  }

  private class DownloadResinAction extends AbstractAction {

    public DownloadResinAction() {
      super("Go");
    }

    @Override
    public void actionPerformed(ActionEvent ae) {
      downloadResin();
    }
  }
}
