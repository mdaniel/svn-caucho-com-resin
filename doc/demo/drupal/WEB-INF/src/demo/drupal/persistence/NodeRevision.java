package demo.drupal.persistence;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name="node_revisions")
public class NodeRevision {
  /**
   * CREATE TABLE `node_revisions` (
   *   `nid` int(10) unsigned NOT NULL default '0',
   *   `vid` int(10) unsigned NOT NULL default '0',
   *   `uid` int(10) NOT NULL default '0',
   *   `title` varchar(128) NOT NULL default '',
   *   `body` longtext NOT NULL,
   *   `teaser` longtext NOT NULL,
   *   `log` longtext NOT NULL,
   *   `timestamp` int(11) NOT NULL default '0',
   *   `format` int(4) NOT NULL default '0',
   *   PRIMARY KEY  (`nid`,`vid`),
   *   KEY `uid` (`uid`)
   * );
   */

  @Id
  private int nid;
  @Id
  private int vid;
  private int uid;
  private String title;
  private String body;
  private String teaser;
  private String log;
  private int timestamp;
  private int format;
}
