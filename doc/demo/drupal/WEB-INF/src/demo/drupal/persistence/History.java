package demo.drupal.persistence;

import javax.persistence.Id;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name="history")
public class History {
  /**
   * CREATE TABLE `history` (
   *   `uid` int(10) NOT NULL default '0',
   *   `nid` int(10) NOT NULL default '0',
   *   `timestamp` int(11) NOT NULL default '0',
   *   PRIMARY KEY  (`uid`,`nid`)
   * );
   */

  @Id
  private int uid;
  @Id
  private int nid;

  private int timestamp;
}
