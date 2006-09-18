package demo.drupal.persistence;

import javax.persistence.Id;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name="moderation_votes")
public class ModerationVote {
  /**
   * CREATE TABLE `moderation_votes` (
   *   `mid` int(10) unsigned NOT NULL auto_increment,
   *   `vote` varchar(255) default NULL,
   *   `weight` tinyint(4) NOT NULL default '0',
   *   PRIMARY KEY  (`mid`)
   * );
   */

  @Id
  private int mid;
  private String vote;
  private int weight;
}
