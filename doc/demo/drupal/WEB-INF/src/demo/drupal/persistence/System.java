package demo.drupal.persistence;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name="system")
public class System {
  /**
   * CREATE TABLE `system` (
   *   `filename` varchar(255) NOT NULL default '',
   *   `name` varchar(255) NOT NULL default '',
   *   `type` varchar(255) NOT NULL default '',
   *   `description` varchar(255) NOT NULL default '',
   *   `status` int(2) NOT NULL default '0',
   *   `throttle` tinyint(1) NOT NULL default '0',
   *   `bootstrap` int(2) NOT NULL default '0',
   *   `schema_version` smallint(2) unsigned NOT NULL default '0',
   *   PRIMARY KEY  (`filename`)
   * );
   */

  @Id
  private String filename;
  private String name;
  private String type;
  private String description;
  private int status;
  private int throttle;
  private int bootstrap;
  private int schema_version;
}
