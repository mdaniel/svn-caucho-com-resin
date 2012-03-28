package com.caucho.quercus;

import com.caucho.quercus.function.AbstractFunction;

public interface ProfileQuercus
{
  public boolean isProfile();

  public void setProfileProbability(double probability);

  public int getProfileIndex(String name);
  
  public String getProfileName(int index);
  
  public AbstractFunction []getProfileFunctionMap();
}
