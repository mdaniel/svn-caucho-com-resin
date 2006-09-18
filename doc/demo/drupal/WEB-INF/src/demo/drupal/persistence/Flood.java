package demo.drupal.persistence;

import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name="flood")
public class Flood {
  /**
   * CREATE TABLE `flood` (
   *   `event` varchar(64) NOT NULL default '',
   *   `hostname` varchar(128) NOT NULL default '',
   *   `timestamp` int(11) NOT NULL default '0'
   * );
   */

  private String event;
  private String hostname;
  private int timestamp;
}
