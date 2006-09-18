package demo.drupal.persistence;

import javax.persistence.Id;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name="term_data")
public class TermData {
  /**
   * CREATE TABLE `term_data` (
   *   `tid` int(10) unsigned NOT NULL auto_increment,
   *   `vid` int(10) unsigned NOT NULL default '0',
   *   `name` varchar(255) NOT NULL default '',
   *   `description` longtext,
   *   `weight` tinyint(4) NOT NULL default '0',
   *   PRIMARY KEY  (`tid`),
   *   KEY `vid` (`vid`)
   * );
   */

  @Id
  private int tid;
  private int vid;
  private String name;
  private String description;
  private int weight;
}
/**
DROP TABLE IF EXISTS `term_hierarchy`;
CREATE TABLE `term_hierarchy` (
  `tid` int(10) unsigned NOT NULL default '0',
  `parent` int(10) unsigned NOT NULL default '0',
  KEY `tid` (`tid`),
  KEY `parent` (`parent`)
);
*/

/**
DROP TABLE IF EXISTS `term_node`;
CREATE TABLE `term_node` (
  `nid` int(10) unsigned NOT NULL default '0',
  `tid` int(10) unsigned NOT NULL default '0',
  PRIMARY KEY  (`tid`,`nid`),
  KEY `nid` (`nid`),
  KEY `tid` (`tid`)
);
*/

/**
DROP TABLE IF EXISTS `term_relation`;
CREATE TABLE `term_relation` (
  `tid1` int(10) unsigned NOT NULL default '0',
  `tid2` int(10) unsigned NOT NULL default '0',
  KEY `tid1` (`tid1`),
  KEY `tid2` (`tid2`)
);
*/

/**
DROP TABLE IF EXISTS `term_synonym`;
CREATE TABLE `term_synonym` (
  `tid` int(10) unsigned NOT NULL default '0',
  `name` varchar(255) NOT NULL default '',
  KEY `tid` (`tid`),
  KEY `name` (`name`(3))
);
*/

