package demo.drupal.persistence;

import javax.persistence.Table;

// XXX: no primary key @Entity
@Table(name="permission")
public class Permission {
  /**
   * CREATE TABLE `permission` (
   *   `rid` int(10) unsigned NOT NULL default '0',
   *   `perm` longtext,
   *   `tid` int(10) unsigned NOT NULL default '0',
   *   KEY `rid` (`rid`)
   * );
   */

  private int rid;
  private String perm;
  private int tid;
}

