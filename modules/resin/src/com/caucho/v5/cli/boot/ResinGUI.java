/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * This file is part of Resin(R)
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

package com.caucho.v5.cli.boot;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.HeadlessException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;

import com.caucho.v5.server.config.ServerConfigBoot;
import com.caucho.v5.util.Version;

@SuppressWarnings("serial")
public class ResinGUI extends JFrame implements WindowListener, ActionListener {

  private final ServerConfigBoot _client;
  private StartCommandGui _command;
  private final ExecutorService _exec = Executors.newSingleThreadExecutor();

  private JRadioButton _start;
  private JRadioButton _stop;
  private JButton _quit;

  public ResinGUI(StartCommandGui command, 
                  ServerConfigBoot client)
    throws HeadlessException, IOException
  {
    super(Version.getFullVersion());

    _client = client;
    _command = command;

    // command.startConsole(client);
    if (true) {
      throw new UnsupportedOperationException(getClass().getName());
    }

    init();
    pack();

    double titleWidth = this.getFontMetrics(this.getFont())
      .getStringBounds(Version.getFullVersion(),
                       this.getGraphics())
      .getWidth();

    final Dimension size = this.getSize();
    int width = (int) titleWidth + 96;

    if (width < size.getWidth())
      width = (int) size.getWidth();

    Dimension dim = new Dimension(width, (int) size.getHeight());

    this.setMinimumSize(dim);
    this.setSize(dim);
  }

  private void init()
  {
    String id = _client.getId();
    
    if (id == null || id.isEmpty())
      id = "default";

    this.setLayout(new BorderLayout());

    Box box = new Box(BoxLayout.Y_AXIS);

    Border border
      = BorderFactory.createCompoundBorder(new EmptyBorder(5, 5, 5, 5),
                                           new TitledBorder("Server: " + id));

    box.setBorder(border);

    ButtonGroup group = new ButtonGroup();
    _start = new JRadioButton("Start");
    _start.setActionCommand("start");
    _start.addActionListener(this);
    _start.setSelected(true);

    _stop = new JRadioButton("Stop");
    _stop.setActionCommand("stop");
    _stop.addActionListener(this);

    group.add(_start);
    group.add(_stop);

    box.add(_start);
    box.add(_stop);

    this.add(box, BorderLayout.CENTER);

    _quit = new JButton("Quit");
    _quit.setActionCommand("quit");
    _quit.addActionListener(this);
    JPanel panel = new JPanel();
    panel.add(_quit);

    this.add(panel, BorderLayout.SOUTH);

    this.addWindowListener(this);
  }

  @Override
  public void actionPerformed(ActionEvent e)
  {
    setUiEnabled(false);
    _exec.execute(new ActionRunnable(e.getActionCommand()));
  }

  private void setUiEnabled(boolean enabled)
  {
    _start.setEnabled(enabled);
    _stop.setEnabled(enabled);
    _quit.setEnabled(enabled);
  }

  @Override
  public void windowDeactivated(WindowEvent e)
  {

  }

  @Override
  public void windowActivated(WindowEvent e)
  {

  }

  @Override
  public void windowDeiconified(WindowEvent e)
  {

  }

  @Override
  public void windowIconified(WindowEvent e)
  {

  }

  @Override
  public void windowClosed(WindowEvent e)
  {
      synchronized (_command) {
        _command.notifyAll();
      }
  }

  @Override
  public void windowClosing(WindowEvent e)
  {
    synchronized (_command) {
      _command.notifyAll();
    }
  }

  @Override
  public void windowOpened(WindowEvent e)
  {

  }

  public class ActionRunnable implements Runnable {
    private String _action;

    public ActionRunnable(String action)
    {
      _action = action;
    }

    @Override
    public void run()
    {
      if ("start".equals(_action)) {
        // _client.startConsole();
        /*
        try {
        } catch (IOException e) {
          e.printStackTrace();
        }
        */
      }
      else {
       // _client.stopConsole();
      }

      SwingUtilities.invokeLater(new Runnable() {
        @Override
        public void run()
        {
          if ("quit".equals(_action)) {
            setVisible(false);
            dispose();
          }
          else
            setUiEnabled(true);
        }
      });
    }
  }
}
