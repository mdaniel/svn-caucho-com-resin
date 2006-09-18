package demo.drupal.persistence;

import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name="files")
public class File {
  /**
   * CREATE TABLE `files` (
   *   `fid` int(10) unsigned NOT NULL default '0',
   *   `nid` int(10) unsigned NOT NULL default '0',
   *   `vid` int(10) unsigned NOT NULL default '0',
   *   `filename` varchar(255) NOT NULL default '',
   *   `description` varchar(255) NOT NULL default '',
   *   `filepath` varchar(255) NOT NULL default '',
   *   `filemime` varchar(255) NOT NULL default '',
   *   `filesize` int(10) unsigned NOT NULL default '0',
   *   `list` tinyint(1) unsigned NOT NULL default '0',
   *   KEY `vid` (`vid`),
   *   KEY `fid` (`fid`)
   * );
   */

  private int fid;
  private int nid;
  private int vid;
  private String filename;
  private String description;
  private String filepath;
  private String filemime;
  private int filesize;
  private int list;
}
