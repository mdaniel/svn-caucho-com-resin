<?php
/**
 * General include file for admin.
 *
 * @author Sam
 */

class MBeanServer
{
  protected $delegate;

  public function MBeanServer($id = null)
  {
    if (isset($id))
      $this->delegate = java("com.caucho.quercus.lib.resin.MBeanServer", $id);
    else
      $this->delegate = java("com.caucho.quercus.lib.resin.MBeanServer");
  }

  public function getServer()
  {
    return $this->lookup("resin:type=Server");
  }

  public function getResin()
  {
    return $this->lookup("resin:type=Resin");
  }

  public function getStatService()
  {
    return $this->lookup("resin:type=StatService");
  }

  public function getLogService()
  {
    return $this->lookup("resin:type=LogService");
  }

  public function query($query)
  {
    try {
      return $this->delegate->query($query);
    } catch (Exception $e) {
      return null;
    }
  }

  function lookup($type)
  {
    try {
      return $this->delegate->lookup($type);
    } catch (Exception $e) {
      return null;
    }
  }

  function isConnected()
  {
    try {
      $this->delegate->lookup("resin:type=Server");

      return true;
    } catch (Exception $e) {
      return false;
    }
  }

  function toString()
  {
    return "PHPMBeanServer[" . $this->$delegate . "]";
  }
}

?>
