package demo.drupal.persistence;

import javax.persistence.Id;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name="poll")
public class Poll {
  /**
   * CREATE TABLE `poll` (
   *   `nid` int(10) unsigned NOT NULL default '0',
   *   `runtime` int(10) NOT NULL default '0',
   *   `polled` longtext NOT NULL,
   *   `active` int(2) unsigned NOT NULL default '0',
   *   PRIMARY KEY  (`nid`)
   * );
   */

  @Id
  private int nid;
  private int runtime;
  private String polled;
  private int active;
}
