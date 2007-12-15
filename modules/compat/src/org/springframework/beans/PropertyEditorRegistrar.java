package org.springframework.beans;

import org.springframework.core.*;

public interface PropertyEditorRegistrar {
  public void registerCustomEditors(PropertyEditorRegistry registry);
}
