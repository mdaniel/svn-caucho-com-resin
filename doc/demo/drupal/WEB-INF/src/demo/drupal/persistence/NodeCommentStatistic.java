package demo.drupal.persistence;

import javax.persistence.Id;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name="node_comment_statistics")
public class NodeCommentStatistic {
  /**
   * CREATE TABLE `node_comment_statistics` (
   *   `nid` int(10) unsigned NOT NULL auto_increment,
   *   `last_comment_timestamp` int(11) NOT NULL default '0',
   *   `last_comment_name` varchar(60) default NULL,
   *   `last_comment_uid` int(10) NOT NULL default '0',
   *   `comment_count` int(10) unsigned NOT NULL default '0',
   *   PRIMARY KEY  (`nid`),
   *   KEY `node_comment_timestamp` (`last_comment_timestamp`)
   * );
   */

  @Id
  private int nid;
  private int last_comment_timestamp;
  private String last_comment_name;
  private int last_comment_uid;
  private int comment_count;
}
