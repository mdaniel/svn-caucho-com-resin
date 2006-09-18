package demo.drupal.persistence;

import javax.persistence.Id;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name="vocabulary")
public class Vocabulary {
  /**
   * CREATE TABLE `vocabulary` (
   *   `vid` int(10) unsigned NOT NULL auto_increment,
   *   `name` varchar(255) NOT NULL default '',
   *   `description` longtext,
   *   `help` varchar(255) NOT NULL default '',
   *   `relations` tinyint(3) unsigned NOT NULL default '0',
   *   `hierarchy` tinyint(3) unsigned NOT NULL default '0',
   *   `multiple` tinyint(3) unsigned NOT NULL default '0',
   *   `required` tinyint(3) unsigned NOT NULL default '0',
   *   `tags` tinyint(3) unsigned NOT NULL default '0',
   *   `module` varchar(255) NOT NULL default '',
   *   `weight` tinyint(4) NOT NULL default '0',
   *   PRIMARY KEY  (`vid`)
   * );
   */

  @Id
  private int vid;
  private String name;
  private String description;
  private String help;
  private int relations;
  private int hierarchy;
  private int multiple;
  private int required;
  private int tags;
  private String module;
  private int weight;
  
  public String toString()
  {
    return getClass().getSimpleName() + "[" + vid + "," + name + "]";
  }
}

/**
DROP TABLE IF EXISTS `vocabulary_node_types`;
CREATE TABLE `vocabulary_node_types` (
  `vid` int(10) unsigned NOT NULL default '0',
  `type` varchar(16) NOT NULL default '',
  PRIMARY KEY  (`vid`,`type`)
);
*/

