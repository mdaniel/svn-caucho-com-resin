package demo.drupal.persistence;

import javax.persistence.Id;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name="moderation_filters")
public class ModerationFilter {
  /**
   * CREATE TABLE `moderation_filters` (
   *   `fid` int(10) unsigned NOT NULL auto_increment,
   *   `filter` varchar(255) NOT NULL default '',
   *   `minimum` smallint(6) NOT NULL default '0',
   *   PRIMARY KEY  (`fid`)
   * );
   */

  @Id
  private int fid;
  private String filter;
  private int minimum;
}
