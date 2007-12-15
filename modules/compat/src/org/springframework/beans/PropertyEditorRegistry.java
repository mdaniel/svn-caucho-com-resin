package org.springframework.beans;

import java.beans.*;

import org.springframework.core.*;

public interface PropertyEditorRegistry {
  public PropertyEditor findCustomEditor(Class requiredType,
					 String propertyPath);

  public void registerCustomEditor(Class requiredType,
				   PropertyEditor propertyEditor);

  public void registerCustomEditor(Class requiredType,
				   String propertyPath,
				   PropertyEditor propertyEditor);
}
