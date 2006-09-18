package demo.drupal.persistence;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.sql.Date;

@Entity
@Table(name="node")
public class Node {
  /**
   * CREATE TABLE `node` (
   *  `nid` int(10) unsigned NOT NULL auto_increment,
   *  `vid` int(10) unsigned NOT NULL default '0',
   *  `type` varchar(32) NOT NULL default '',
   *  `title` varchar(128) NOT NULL default '',
   *  `uid` int(10) NOT NULL default '0',
   *  `status` int(4) NOT NULL default '1',
   *  `created` int(11) NOT NULL default '0',
   *  `changed` int(11) NOT NULL default '0',
   *  `comment` int(2) NOT NULL default '0',
   *  `promote` int(2) NOT NULL default '0',
   *  `moderate` int(2) NOT NULL default '0',
   *  `sticky` int(2) NOT NULL default '0',
   *  PRIMARY KEY  (`nid`),
   *  KEY `node_type` (`type`(4)),
   *  KEY `node_title_type` (`title`,`type`(4)),
   *  KEY `status` (`status`),
   *  KEY `uid` (`uid`),
   *  KEY `vid` (`vid`),
   *  KEY `node_moderate` (`moderate`),
   *  KEY `node_promote_status` (`promote`,`status`),
   *  KEY `node_created` (`created`),
   *  KEY `node_changed` (`changed`),
   *  KEY `node_status_type` (`status`,`type`,`nid`)
   *);
   */

  @Id
  @Column(name="nid")
  private int id;

  private String type;
  private String title;
  private int status;
  private Date created;
  private Date changed;
  private int comment;
  private int promote;
  private int moderate;
  private int sticky;

  @ManyToOne
  @JoinColumn(name="uid")
  private User user;

  @ManyToOne
  @JoinColumn(name="vid")
  private Vocabulary vocabulary;

  public int getId()
  {
    return id;
  }

  public Date getChanged()
  {
    return changed;
  }

  public int getComment()
  {
    return comment;
  }

  public Date getCreated()
  {
    return created;
  }

  public int getModerate()
  {
    return moderate;
  }

  public int getPromote()
  {
    return promote;
  }

  public int getStatus()
  {
    return status;
  }

  public int getSticky()
  {
    return sticky;
  }

  public String getTitle()
  {
    return title;
  }

  public String getType()
  {
    return type;
  }

  public User getUser()
  {
    return user;
  }

  public Vocabulary getVocabulary()
  {
    return vocabulary;
  }
}
