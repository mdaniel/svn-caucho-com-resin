package example;

import javax.persistence.*;
import java.sql.Date;

@Entity
@Table(name="recipes")
public class Recipe {
  @Id
  @GeneratedValue
  private int id;

  @ManyToOne(optional = false)
  @JoinColumn(name = "category_id")
  private Category category;

  @Column(unique = true, nullable = false)
  private String title;

  private String description;

  @Column(nullable = false)
  private Date date;

  @Lob
  private String instructions;

  public Category getCategory()
  {
    return category;
  }

  public void setCategory(Category category)
  {
    this.category = category;
  }

  public Date getDate()
  {
    return date;
  }

  public void setDate(Date date)
  {
    this.date = date;
  }

  public String getDescription()
  {
    return description;
  }

  public void setDescription(String description)
  {
    this.description = description;
  }

  public int getId()
  {
    return id;
  }

  public void setId(int id)
  {
    this.id = id;
  }

  public String getInstructions()
  {
    return instructions;
  }

  public void setInstructions(String instructions)
  {
    this.instructions = instructions;
  }

  public String getTitle()
  {
    return title;
  }

  public void setTitle(String title)
  {
    this.title = title;
  }

  public String toString()
  {
    return getClass().getSimpleName() + "[id=" + id + " title=" + title + "]";
  }
}
