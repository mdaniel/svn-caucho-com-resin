package demo.drupal.persistence;

import javax.persistence.Id;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name="node_access")
public class NodeAccess {
  /**
   * CREATE TABLE `node_access` (
   *   `nid` int(10) unsigned NOT NULL default '0',
   *   `gid` int(10) unsigned NOT NULL default '0',
   *   `realm` varchar(255) NOT NULL default '',
   *   `grant_view` tinyint(1) unsigned NOT NULL default '0',
   *   `grant_update` tinyint(1) unsigned NOT NULL default '0',
   *   `grant_delete` tinyint(1) unsigned NOT NULL default '0',
   *   PRIMARY KEY  (`nid`,`gid`,`realm`)
   * );
   */

  @Id
  private int nid;
  @Id
  private int gid;
  @Id
  private String realm;
  private int grant_view;
  private int grant_update;
  private int grant_delete;
}
