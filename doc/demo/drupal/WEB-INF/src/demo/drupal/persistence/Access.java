package demo.drupal.persistence;

import javax.persistence.Id;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name="access")
public class Access {
  /**
   * CREATE TABLE `access` (
   *   `aid` tinyint(10) NOT NULL auto_increment,
   *   `mask` varchar(255) NOT NULL default '',
   *   `type` varchar(255) NOT NULL default '',
   *   `status` tinyint(2) NOT NULL default '0',
   *   PRIMARY KEY  (`aid`)
   * );
   */

  @Id
  private int aid;
  private String mask;
  private String type;
  private int status;
}
