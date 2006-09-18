package demo.drupal.persistence;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name="aggregator_feed")
public class AggregatorFeed {
  /**
   * CREATE TABLE `aggregator_feed` (
   *   `fid` int(10) NOT NULL auto_increment,
   *   `title` varchar(255) NOT NULL default '',
   *   `url` varchar(255) NOT NULL default '',
   *   `refresh` int(10) NOT NULL default '0',
   *   `checked` int(10) NOT NULL default '0',
   *   `link` varchar(255) NOT NULL default '',
   *   `description` longtext NOT NULL,
   *   `image` longtext NOT NULL,
   *   `etag` varchar(255) NOT NULL default '',
   *   `modified` int(10) NOT NULL default '0',
   *   `block` tinyint(2) NOT NULL default '0',
   *   PRIMARY KEY  (`fid`),
   *   UNIQUE KEY `link` (`url`),
   *   UNIQUE KEY `title` (`title`)
   * );
   */

  @Id
  private int fid;
  private String title;
  private String url;
  private int refresh;
  private int checked;
  private String link;
  private String description;
  private byte[] image;
  private String etag;
  private int modified;
  private int block;
}
