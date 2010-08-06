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

package com.caucho.boot;

import com.caucho.Version;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.IOException;

/**
 * Created by IntelliJ IDEA.
 * User: alex
 * Date: Aug 6, 2010
 * Time: 1:22:40 PM
 * To change this template use File | Settings | File Templates.
 */
public class ResinGUI extends JFrame implements WindowListener, ActionListener {

  private final WatchdogClient _client;
  private final ResinBoot _boot;

  public ResinGUI(ResinBoot boot, WatchdogClient client)
    throws HeadlessException, IOException
  {
    super(Version.FULL_VERSION);
   
    _boot = boot;
    _client = client;

    try {
      _client.startConsole();
    } catch (IOException e) {
      throw e;
    }

    init();
    pack();

    double titleWidth = this.getFontMetrics(this.getFont())
      .getStringBounds(Version.FULL_VERSION,
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
    if (id != null || id.isEmpty())
      id = "default";

    this.setLayout(new BorderLayout());

    Box box = new Box(BoxLayout.Y_AXIS);

    Border border
      = BorderFactory.createCompoundBorder(new EmptyBorder(5, 5, 5, 5),
                                           new TitledBorder("Server: " + id));

    box.setBorder(border);

    ButtonGroup group = new ButtonGroup();
    JRadioButton start = new JRadioButton("Start");
    start.setActionCommand("start");
    start.addActionListener(this);
    start.setSelected(true);

    JRadioButton stop = new JRadioButton("Stop");
    stop.setActionCommand("stop");
    stop.addActionListener(this);

    group.add(start);
    group.add(stop);

    box.add(start);
    box.add(stop);

    this.add(box, BorderLayout.CENTER);

    JButton button = new JButton("Quit");
    button.setActionCommand("quit");
    button.addActionListener(this);
    JPanel panel = new JPanel();
    panel.add(button);

    this.add(panel, BorderLayout.SOUTH);

    this.addWindowListener(this);
  }

  @Override
  public void actionPerformed(ActionEvent e)
  {
    if ("start".equals(e.getActionCommand())) {
      start();
    }
    else if ("stop".equals(e.getActionCommand())) {
      stop();
    }
    else {
      quit();
    }
  }

  private void stop()
  {
    _client.stopConsole();
  }

  private void start()
  {
    try {
      _client.startConsole();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private void quit()
  {
    _client.stopConsole();
    setVisible(false);
    dispose();
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
  }

  @Override
  public void windowClosing(WindowEvent e)
  {
    synchronized (_boot) {
      _boot.notifyAll();
    }
  }

  @Override
  public void windowOpened(WindowEvent e)
  {

  }
}
