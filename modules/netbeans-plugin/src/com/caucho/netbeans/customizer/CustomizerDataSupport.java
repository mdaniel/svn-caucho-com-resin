/*
 * Copyright (c) 1998-2007 Caucho Technology -- all rights reserved
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
 * @author Sam
 */


package com.caucho.netbeans.customizer;

import com.caucho.netbeans.util.ResinProperties;

import org.netbeans.api.java.platform.JavaPlatform;
import org.netbeans.api.java.platform.JavaPlatformManager;
import org.netbeans.api.java.platform.Specification;
import org.openide.ErrorManager;

import javax.swing.*;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.PlainDocument;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Arrays;


/**
 * Customizer data support keeps models for all the customizer components,
 * initializes them, tracks model changes and performs save.
 */
public final class CustomizerDataSupport
{

  // models
  private DefaultComboBoxModel jvmModel;
  private Document javaOptsModel;
  private Document resinRootModel;
  private Document resinBaseModel;
  private PathModel sourceModel;
  private PathModel classModel;
  private PathModel javadocModel;
  private SpinnerNumberModel serverPortModel;
  private ButtonModel proxyModel;
  private ButtonModel autoloadModel;

  // model dirty flags
  private boolean jvmModelFlag;
  private boolean javaOptsModelFlag;
  private boolean sourceModelFlag;
  private boolean javadocModelFlag;
  private boolean serverPortModelFlag;
  private boolean proxyModelFlag;
  private boolean autoloadModelFlag;

  private ResinProperties properties;

  /**
   * Creates a new instance of CustomizerDataSupport
   */
  public CustomizerDataSupport(ResinProperties properties)
  {
    this.properties = properties;
    init();
  }

  /**
   * Initialize the customizer models.
   */
  private void init()
  {

    // jvmModel
    jvmModel = new DefaultComboBoxModel();
    loadJvmModel();
    jvmModel.addListDataListener(new ListDataListener()
    {
      public void contentsChanged(ListDataEvent e)
      {
        jvmModelFlag = true;
        store(); // This is just temporary until the server manager has OK and Cancel buttons
      }

      public void intervalAdded(ListDataEvent e)
      {
      }

      public void intervalRemoved(ListDataEvent e)
      {
      }
    });

    // javaOptions
    javaOptsModel = createDocument(properties.getJavaOpts());
    javaOptsModel.addDocumentListener(new ModelChangeAdapter()
    {
      public void modelChanged()
      {
        javaOptsModelFlag = true;
        store(); // This is just temporary until the server manager has OK and Cancel buttons
      }
    });

    // resinRootModel
    resinRootModel
      = createDocument(properties.getResinHome().getAbsolutePath());

    // resinBaseModel
    resinBaseModel
      = createDocument(properties.getResinConf().getAbsolutePath());

    // classModel
    classModel = new PathModel(properties.getClasses());

    // sourceModel
    sourceModel = new PathModel(properties.getSources());
    sourceModel.addListDataListener(new ModelChangeAdapter()
    {
      public void modelChanged()
      {
        sourceModelFlag = true;
        store(); // This is just temporary until the server manager has OK and Cancel buttons
      }
    });

    // javadocModel
    javadocModel = new PathModel(properties.getJavadocs());
    javadocModel.addListDataListener(new ModelChangeAdapter()
    {
      public void modelChanged()
      {
        javadocModelFlag = true;
        store(); // This is just temporary until the server manager has OK and Cancel buttons
      }
    });

    // serverPortModel
    // TODO manager.getServerPort
    serverPortModel = new SpinnerNumberModel(properties.getServerPort(),
                                             0,
                                             65535,
                                             1);
    serverPortModel.addChangeListener(new ModelChangeAdapter()
    {
      public void modelChanged()
      {
        serverPortModelFlag = true;
        store(); // This is just temporary until the server manager has OK and Cancel buttons
      }
    });

    // proxyModel
    proxyModel = createToggleButtonModel(properties.getProxyEnabled());
    proxyModel.addItemListener(new ModelChangeAdapter()
    {
      public void modelChanged()
      {
        proxyModelFlag = true;
        store(); // This is just temporary until the server manager has OK and Cancel buttons
      }
    });

    // autoloadModel
    autoloadModel = createToggleButtonModel(properties.getAutoloadEnabled());
    autoloadModel.addItemListener(new ModelChangeAdapter()
    {
      public void modelChanged()
      {
        autoloadModelFlag = true;
        store(); // This is just temporary until the server manager has OK and Cancel buttons
      }
    });
  }

  /**
   * Update the jvm model
   */
  public void loadJvmModel()
  {
    JavaPlatformManager jpm = JavaPlatformManager.getDefault();
    JavaPlatformAdapter curJvm
      = (JavaPlatformAdapter) jvmModel.getSelectedItem();
    String curPlatformName = null;
    if (curJvm != null) {
      curPlatformName = curJvm.getName();
    }
    else {
      curPlatformName = (String) properties.getJavaPlatform()
        .getProperties()
        .get(ResinProperties.PLAT_PROP_ANT_NAME);
    }

    jvmModel.removeAllElements();

    // feed the combo with sorted platform list
    JavaPlatform[] j2sePlatforms = jpm.getPlatforms(null,
                                                    new Specification("J2SE",
                                                                      null)); // NOI18N
    JavaPlatformAdapter[] platformAdapters
      = new JavaPlatformAdapter[j2sePlatforms.length];
    for (int i = 0; i < platformAdapters.length; i++) {
      platformAdapters[i] = new JavaPlatformAdapter(j2sePlatforms[i]);
    }
    Arrays.sort(platformAdapters);
    for (int i = 0; i < platformAdapters.length; i++) {
      JavaPlatformAdapter platformAdapter = platformAdapters[i];
      jvmModel.addElement(platformAdapter);
      // try to set selected item
      if (curPlatformName != null) {
        if (curPlatformName.equals(platformAdapter.getName())) {
          jvmModel.setSelectedItem(platformAdapter);
        }
      }
    }
  }

  // model getters ----------------------------------------------------------

  public DefaultComboBoxModel getJvmModel()
  {
    return jvmModel;
  }

  public Document getJavaOptsModel()
  {
    return javaOptsModel;
  }

  public Document getResinRootModel()
  {
    return resinRootModel;
  }

  public Document getResinBaseModel()
  {
    return resinBaseModel;
  }

  public PathModel getClassModel()
  {
    return classModel;
  }

  public PathModel getSourceModel()
  {
    return sourceModel;
  }

  public PathModel getJavadocsModel()
  {
    return javadocModel;
  }

  public SpinnerNumberModel getServerPortModel()
  {
    return serverPortModel;
  }

  public ButtonModel getProxyModel()
  {
    return proxyModel;
  }

  public ButtonModel getAutoloadModel()
  {
    return autoloadModel;
  }

  // private helper methods -------------------------------------------------

  /**
   * Save all changes
   */
  private void store()
  {

    if (jvmModelFlag) {
      JavaPlatformAdapter platformAdapter
        = (JavaPlatformAdapter) jvmModel.getSelectedItem();
      properties.setJavaPlatform(platformAdapter.getJavaPlatform());
      jvmModelFlag = false;
    }

    if (javaOptsModelFlag) {
      properties.setJavaOpts(getText(javaOptsModel));
      javaOptsModelFlag = false;
    }

    if (sourceModelFlag) {
      properties.setSources(sourceModel.getData());
      sourceModelFlag = false;
    }

    if (javadocModelFlag) {
      properties.setJavadocs(javadocModel.getData());
      javadocModelFlag = false;
    }

    if (serverPortModelFlag) {
      properties.setServerPort(((Integer) serverPortModel.getValue()).intValue());
      serverPortModelFlag = false;
    }

    if (proxyModelFlag) {
      properties.setProxyEnabled(proxyModel.isSelected());
      proxyModelFlag = false;
    }

    if (autoloadModelFlag) {
      properties.setAutoloadEnabled(autoloadModel.isSelected());
      autoloadModelFlag = false;
    }
  }

  /**
   * Create a Document initialized by the specified text parameter, which may
   * be null
   */
  private Document createDocument(String text)
  {
    PlainDocument doc = new PlainDocument();
    if (text != null) {
      try {
        doc.insertString(0, text, null);
      }
      catch (BadLocationException e) {
        ErrorManager.getDefault().notify(e);
      }
    }
    return doc;
  }

  /**
   * Create a ToggleButtonModel inilialized by the specified selected
   * parameter.
   */
  private JToggleButton.ToggleButtonModel createToggleButtonModel(boolean selected)
  {
    JToggleButton.ToggleButtonModel model
      = new JToggleButton.ToggleButtonModel();
    model.setSelected(selected);
    return model;
  }

  /**
   * Get the text value from the document
   */
  private String getText(Document doc)
  {
    try {
      return doc.getText(0, doc.getLength());
    }
    catch (BadLocationException e) {
      ErrorManager.getDefault().notify(e);
      return null;
    }
  }

  // private helper class ---------------------------------------------------

  /**
   * Adapter that implements several listeners, which is useful for dirty
   * model monitoring.
   */
  private abstract class ModelChangeAdapter
    implements ListDataListener, DocumentListener, ItemListener, ChangeListener
  {

    public abstract void modelChanged();

    public void contentsChanged(ListDataEvent e)
    {
      modelChanged();
    }

    public void intervalAdded(ListDataEvent e)
    {
      modelChanged();
    }

    public void intervalRemoved(ListDataEvent e)
    {
      modelChanged();
    }

    public void changedUpdate(DocumentEvent e)
    {
      modelChanged();
    }

    public void removeUpdate(DocumentEvent e)
    {
      modelChanged();
    }

    public void insertUpdate(DocumentEvent e)
    {
      modelChanged();
    }

    public void itemStateChanged(ItemEvent e)
    {
      modelChanged();
    }

    public void stateChanged(javax.swing.event.ChangeEvent e)
    {
      modelChanged();
    }
  }

  /**
   * Java platform combo box model helper
   */
  private static class JavaPlatformAdapter
    implements Comparable
  {
    private JavaPlatform platform;

    public JavaPlatformAdapter(JavaPlatform platform)
    {
      this.platform = platform;
    }

    public JavaPlatform getJavaPlatform()
    {
      return platform;
    }

    public String getName()
    {
      return (String) platform.getProperties()
        .get(ResinProperties.PLAT_PROP_ANT_NAME);
    }

    public String toString()
    {
      return platform.getDisplayName();
    }

    public int compareTo(Object o)
    {
      return toString().compareTo(o.toString());
    }
  }
}
