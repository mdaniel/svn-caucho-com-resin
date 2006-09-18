package demo.drupal.persistence;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name="node_counter")
public class NodeCounter {
  /**
   * CREATE TABLE `node_counter` (
   *   `nid` int(11) NOT NULL default '0',
   *   `totalcount` bigint(20) unsigned NOT NULL default '0',
   *   `daycount` mediumint(8) unsigned NOT NULL default '0',
   *   `timestamp` int(11) unsigned NOT NULL default '0',
   *   PRIMARY KEY  (`nid`),
   *   KEY `totalcount` (`totalcount`),
   *   KEY `daycount` (`daycount`),
   *   KEY `timestamp` (`timestamp`)
   * );
   */

  @Id
  private int nid;
  private long totalcount;
  private int daycount;
  private int timestamp;
}
