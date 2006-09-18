package demo.drupal.persistence;

import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name="blocks")
public class Block {
  /**
   * CREATE TABLE `blocks` (
   *   `module` varchar(64) NOT NULL default '',
   *   `delta` varchar(32) NOT NULL default '0',
   *   `theme` varchar(255) NOT NULL default '',
   *   `status` tinyint(2) NOT NULL default '0',
   *   `weight` tinyint(1) NOT NULL default '0',
   *   `region` varchar(64) NOT NULL default 'left',
   *   `custom` tinyint(2) NOT NULL default '0',
   *   `throttle` tinyint(1) NOT NULL default '0',
   *   `visibility` tinyint(1) NOT NULL default '0',
   *   `pages` text NOT NULL
   * );
   */

  private String module;
  private String delta;
  private String theme;
  private int status;
  private int weight;
  private String region;
  private int custom;
  private int throttle;
  private int visibility;
  private String pages;
}
