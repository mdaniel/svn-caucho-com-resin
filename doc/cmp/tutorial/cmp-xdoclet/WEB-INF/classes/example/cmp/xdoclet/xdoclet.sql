DROP TABLE IF EXISTS xdoclet_courses;
CREATE TABLE xdoclet_courses (
  id VARCHAR(250) NOT NULL,
  instructor VARCHAR(250),

  PRIMARY KEY(id)
);

DROP TABLE IF EXISTS xdoclet_students;
CREATE TABLE xdoclet_students (
  name VARCHAR(250) NOT NULL,

  PRIMARY KEY(name)
);

DROP TABLE IF EXISTS xdoclet_enrollment;
CREATE TABLE xdoclet_enrollment (
  student VARCHAR(250) NOT NULL,
  course VARCHAR(250) NOT NULL
);

INSERT INTO xdoclet_courses VALUES('Defense Against the Dark Arts', 'Remus Lupin');
INSERT INTO xdoclet_courses VALUES('Divination', 'Sybil Trelawney');

INSERT INTO xdoclet_students VALUES('Harry Potter');
INSERT INTO xdoclet_students VALUES('Ron Weasley');
INSERT INTO xdoclet_students VALUES('Hermione Granger');

INSERT INTO xdoclet_enrollment VALUES('Hermione Granger', 'Defense Against the Dark Arts');
INSERT INTO xdoclet_enrollment VALUES('Harry Potter', 'Defense Against the Dark Arts');
INSERT INTO xdoclet_enrollment VALUES('Ron Weasley', 'Defense Against the Dark Arts');

INSERT INTO xdoclet_enrollment VALUES('Harry Potter', 'Divination');
INSERT INTO xdoclet_enrollment VALUES('Ron Weasley', 'Divination');
