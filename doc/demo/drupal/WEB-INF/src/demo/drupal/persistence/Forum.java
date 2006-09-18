package demo.drupal.persistence;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name="forum")
public class Forum {
  /**
   * CREATE TABLE `forum` (
   *   `nid` int(10) unsigned NOT NULL default '0',
   *   `vid` int(10) unsigned NOT NULL default '0',
   *   `tid` int(10) unsigned NOT NULL default '0',
   *   PRIMARY KEY  (`nid`),
   *   KEY `vid` (`vid`),
   *   KEY `tid` (`tid`)
   * );
   */

  @Id
  private int nid;

  // XXX: relationship
  private int vid;

  // XXX: relationship
  private int tid;
}
