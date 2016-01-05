package com.caucho.quercus.env.xdebug;

import com.caucho.quercus.module.AbstractQuercusModule;
import com.caucho.quercus.module.IniDefinition;
import com.caucho.quercus.module.IniDefinitions;

public class XdebugModule extends AbstractQuercusModule
{
  private static final IniDefinitions _iniDefinitions = new IniDefinitions();

  static final IniDefinition INI_REMOTE_PORT = _iniDefinitions
      .add("xdebug.remote_port", 9000, PHP_INI_ALL);

  static final IniDefinition INI_REMOTE_HOST = _iniDefinitions
      .add("xdebug.remote_host", "127.0.0.1", PHP_INI_ALL);

  static final IniDefinition INI_REMOTE_MODE = _iniDefinitions
      .add("xdebug.remote_mode", "req", PHP_INI_ALL);

  static final IniDefinition INI_REMOTE_ENABLE = _iniDefinitions
      .add("xdebug.remote_enable", false, PHP_INI_ALL);

  static final IniDefinition INI_IDEKEY = _iniDefinitions
      .add("xdebug.idekey", "quercus", PHP_INI_ALL);

  @Override
  public String[] getLoadedExtensions() {
    return new String[] { "xdebug" };
  }

  @Override
  public IniDefinitions getIniDefinitions() {
    return _iniDefinitions;
  }
}
