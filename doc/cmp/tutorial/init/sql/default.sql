DROP TABLE basic_courses;
CREATE TABLE basic_courses (
  id VARCHAR(250) NOT NULL,
  teacher VARCHAR(250),

  PRIMARY KEY(id)
);

INSERT INTO basic_courses VALUES('Potions', 'Severus Snape');
INSERT INTO basic_courses VALUES('Transfiguration', 'Minerva McGonagall');

DROP TABLE find_courses;
CREATE TABLE find_courses (
  course_id VARCHAR(250) NOT NULL,
  instructor VARCHAR(250),

  PRIMARY KEY(course_id)
);

INSERT INTO find_courses VALUES('Potions', 'Severus Snape');
INSERT INTO find_courses VALUES('Transfiguration', 'Minerva McGonagall');
INSERT INTO find_courses VALUES('Defense Against the Dark Arts', 'Remus Lupin');

DROP TABLE create_courses;
CREATE TABLE create_courses (
  course_id VARCHAR(250) NOT NULL,
  instructor VARCHAR(250),

  PRIMARY KEY(course_id)
);

INSERT INTO create_courses VALUES('Potions', 'Severus Snape');
INSERT INTO create_courses VALUES('Transfiguration', 'Minerva McGonagall');
INSERT INTO create_courses VALUES('Defense Against the Dark Arts', 'Remus Lupin');

DROP TABLE one2one_courses;
CREATE TABLE one2one_courses (
  name VARCHAR(250) NOT NULL,
  teacher VARCHAR(250),

  PRIMARY KEY(name)
);

DROP TABLE one2one_teachers;
CREATE TABLE one2one_teachers (
  name VARCHAR(250) NOT NULL,
  course VARCHAR(250),

  PRIMARY KEY(name)
);

INSERT INTO one2one_courses VALUES('Potions', 'Severus Snape');
INSERT INTO one2one_courses VALUES('Transfiguration', 'Minerva McGonagall');
INSERT INTO one2one_courses VALUES('Defense Against the Dark Arts', 'Remus Lupin');

INSERT INTO one2one_teachers VALUES('Severus Snape', 'Potions');
INSERT INTO one2one_teachers VALUES('Minerva McGonagall', 'Transfiguration');
INSERT INTO one2one_teachers VALUES('Remus Lupin', 'Defense Against the Dark Arts');


DROP TABLE one2many_students;
CREATE TABLE one2many_students (
  name VARCHAR(250) NOT NULL,
  house VARCHAR(250),

  PRIMARY KEY(name)
);

DROP TABLE one2many_houses;
CREATE TABLE one2many_houses (
  name VARCHAR(250) NOT NULL,

  PRIMARY KEY(name)
);

INSERT INTO one2many_students VALUES('Harry Potter', 'Gryffindor');
INSERT INTO one2many_students VALUES('Ron Weasley', 'Gryffindor');
INSERT INTO one2many_students VALUES('Hermione Granger', 'Gryffindor');

INSERT INTO one2many_students VALUES('Draco Malfoy', 'Slytherin');
INSERT INTO one2many_students VALUES('Millicent Bulstrode', 'Slytherin');

INSERT INTO one2many_students VALUES('Penelope Clearwater', 'Ravenclaw');
INSERT INTO one2many_students VALUES('Cho Chang', 'Ravenclaw');

INSERT INTO one2many_students VALUES('Cedric Diggory', 'Hufflepuff');
INSERT INTO one2many_students VALUES('Justin Finch-Fletchley', 'Hufflepuff');

INSERT INTO one2many_houses VALUES('Gryffindor');
INSERT INTO one2many_houses VALUES('Slytherin');
INSERT INTO one2many_houses VALUES('Hufflepuff');
INSERT INTO one2many_houses VALUES('Ravenclaw');

DROP TABLE many2many_courses;
CREATE TABLE many2many_courses (
  name VARCHAR(250) NOT NULL,
  instructor VARCHAR(250),

  PRIMARY KEY(name)
);

DROP TABLE many2many_students;
CREATE TABLE many2many_students (
  name VARCHAR(250) NOT NULL,

  PRIMARY KEY(name)
);

DROP TABLE many2many_student_course_mapping;
CREATE TABLE many2many_student_course_mapping (
  many2many_students VARCHAR(250) NOT NULL,
  many2many_courses VARCHAR(250) NOT NULL
);

INSERT INTO many2many_courses VALUES('Defense Against the Dark Arts', 'Remus Lupin');
INSERT INTO many2many_courses VALUES('Divination', 'Sybil Trelawney');

INSERT INTO many2many_students VALUES('Harry Potter');
INSERT INTO many2many_students VALUES('Ron Weasley');
INSERT INTO many2many_students VALUES('Hermione Granger');

INSERT INTO many2many_student_course_mapping VALUES('Hermione Granger', 'Defense Against the Dark Arts');
INSERT INTO many2many_student_course_mapping VALUES('Harry Potter', 'Defense Against the Dark Arts');
INSERT INTO many2many_student_course_mapping VALUES('Ron Weasley', 'Defense Against the Dark Arts');

INSERT INTO many2many_student_course_mapping VALUES('Harry Potter', 'Divination');
INSERT INTO many2many_student_course_mapping VALUES('Ron Weasley', 'Divination');

DROP TABLE ejbql_teacher;
CREATE TABLE ejbql_teacher (
  name VARCHAR(250) NOT NULL,
  course VARCHAR(250) NOT NULL,
	
  PRIMARY KEY(name)
);

DROP TABLE ejbql_student;
CREATE TABLE ejbql_student (
  name VARCHAR(250) NOT NULL,
  house VARCHAR(250) NOT NULL,
  gender VARCHAR(6) NOT NULL,

  PRIMARY KEY(name)
);

DROP TABLE ejbql_student_course_mapping;
CREATE TABLE ejbql_student_course_mapping (
  ejbql_student VARCHAR(250) NOT NULL,
  ejbql_course VARCHAR(250) NOT NULL
);

DROP TABLE ejbql_course;
CREATE TABLE ejbql_course( 
  name VARCHAR(250) NOT NULL,
  room VARCHAR(250) NOT NULL,
  teacher VARCHAR(250) NOT NULL,

  PRIMARY KEY(name)
);

DROP TABLE ejbql_house;
CREATE TABLE ejbql_house (
  name VARCHAR(250) NOT NULL,

  PRIMARY KEY(name)
);

INSERT INTO ejbql_course VALUES('Potions', 'Black Dungeon', 'Severus Snape');
INSERT INTO ejbql_course VALUES('Transfiguration', 'West Tower', 'Minerva McGonagall');

INSERT INTO ejbql_student VALUES('Harry Potter', 'Gryffindor', 'Male');
INSERT INTO ejbql_student VALUES('Ron Weasley', 'Gryffindor', 'Male');
INSERT INTO ejbql_student VALUES('Penelope Clearwater', 'Ravenclaw', 'Female');
INSERT INTO ejbql_student VALUES('Draco Malfoy', 'Slytherin', 'Male');

INSERT INTO ejbql_student_course_mapping VALUES('Harry Potter', 'Potions');
INSERT INTO ejbql_student_course_mapping VALUES('Harry Potter', 'Transfiguration');
INSERT INTO ejbql_student_course_mapping VALUES('Draco Malfoy', 'Potions');
INSERT INTO ejbql_student_course_mapping VALUES('Ron Weasley', 'Transfiguration');

INSERT INTO ejbql_teacher VALUES('Severus Snape', 'Potions');
INSERT INTO ejbql_teacher VALUES('Minerva McGonagall', 'Transfiguration');

INSERT INTO ejbql_house VALUES('Gryffindor');
INSERT INTO ejbql_house VALUES('Slytherin');
INSERT INTO ejbql_house VALUES('Ravenclaw');
INSERT INTO ejbql_house VALUES('Hufflepuff');

DROP TABLE select_student;
CREATE TABLE select_student (
  name VARCHAR(250) NOT NULL,
  house VARCHAR(250) NOT NULL,
  gender VARCHAR(6) NOT NULL,

  PRIMARY KEY(name)
);

DROP TABLE select_house;
CREATE TABLE select_house (
  name VARCHAR(250) NOT NULL,

  PRIMARY KEY(name)
);

INSERT INTO select_student VALUES('Harry Potter', 'Gryffindor', 'Boy');
INSERT INTO select_student VALUES('Ron Weasley', 'Gryffindor', 'Boy');
INSERT INTO select_student VALUES('Hermione Granger', 'Gryffindor', 'Girl');
INSERT INTO select_student VALUES('Draco Malfoy', 'Slytherin', 'Boy');
INSERT INTO select_student VALUES('Penelope Clearwater', 'Ravenclaw', 'Girl');
INSERT INTO select_student VALUES('Millicent Bulstrode', 'Slytherin', 'Girl');

INSERT INTO select_house VALUES('Gryffindor');
INSERT INTO select_house VALUES('Slytherin');
INSERT INTO select_house VALUES('Ravenclaw');
INSERT INTO select_house VALUES('Hufflepuff');

DROP TABLE transaction_student;
CREATE TABLE transaction_student (
  id INTEGER NOT NULL,
  name VARCHAR(250) NOT NULL,
  password VARCHAR(250),
  gender VARCHAR(1) NOT NULL,

  PRIMARY KEY(name)
);

DROP TABLE transaction_student_course_mapping;
CREATE TABLE transaction_student_course_mapping (
  transaction_student INTEGER NOT NULL,
  transaction_course INTEGER NOT NULL
);

DROP TABLE transaction_course;
CREATE TABLE transaction_course( 
  id INTEGER NOT NULL,
  name VARCHAR(250) NOT NULL,
  teacher VARCHAR(250) NOT NULL,
  max_student_amount INTEGER,

  PRIMARY KEY(id)
);

INSERT INTO transaction_student VALUES(1, 'Harry Potter', 'quidditch', 'M');
INSERT INTO transaction_student VALUES(2, 'Ron Weasley', 'chudleycannons', 'M');
INSERT INTO transaction_student VALUES(3, 'Hermione Granger', 'o45m8i19', 'F');


INSERT INTO transaction_course VALUES(101, 'Potions', 'Severus Snape', 0);
INSERT INTO transaction_course VALUES(102, 'Transfiguration', 'Minerva McGonagall', 20);
INSERT INTO transaction_course VALUES(103, 'History of Magic', 'Ghost Binns', 20);

DROP TABLE id_students;
CREATE TABLE id_students (
  name VARCHAR(250) NOT NULL,

  PRIMARY KEY(name)
);

DROP TABLE id_quidditch;
CREATE TABLE id_quidditch (
  student VARCHAR(250) NOT NULL REFERENCES id_students(name),

  pos VARCHAR(250),
  points INTEGER,

  PRIMARY KEY(student)
);

INSERT INTO id_students VALUES('Harry Potter');
INSERT INTO id_students VALUES('Ron Weasley');

INSERT INTO id_students VALUES('Alicia Spinnet');
INSERT INTO id_students VALUES('Draco Malfoy');

INSERT INTO id_quidditch VALUES('Harry Potter', 'seeker', 300);
INSERT INTO id_quidditch VALUES('Alicia Spinnet', 'chaser', 60);
INSERT INTO id_quidditch VALUES('Fred Weasley', 'beater', 10);
INSERT INTO id_quidditch VALUES('George Weasley', 'beater', 20);
INSERT INTO id_quidditch VALUES('Draco Malfoy', 'seeker', 0);
INSERT INTO id_quidditch VALUES('Oliver Wood', 'keeper', 10);
DROP TABLE map_students;
CREATE TABLE map_students (
  name VARCHAR(250) NOT NULL,

  PRIMARY KEY(name)
);

DROP TABLE map_courses;
CREATE TABLE map_courses (
  name VARCHAR(250) NOT NULL,

  PRIMARY KEY(name)
);

DROP TABLE map_grades;
CREATE TABLE map_grades (
  student VARCHAR(250) NOT NULL REFERENCES map_students(name),
  course VARCHAR(250) NOT NULL REFERENCES map_courses(name),

  grade VARCHAR(10),

  PRIMARY KEY(student, course)
);

INSERT INTO map_students VALUES('Harry Potter');
INSERT INTO map_students VALUES('Ron Weasley');
INSERT INTO map_students VALUES('Hermione Granger');

INSERT INTO map_courses VALUES('Defence Against the Dark Arts');
INSERT INTO map_courses VALUES('Potions');
INSERT INTO map_courses VALUES('Transfiguration');

INSERT INTO map_grades VALUES('Harry Potter', 'Defence Against the Dark Arts', 'A');
INSERT INTO map_grades VALUES('Ron Weasley', 'Defence Against the Dark Arts', 'A-');
INSERT INTO map_grades VALUES('Hermione Granger', 'Defence Against the Dark Arts', 'A+');

INSERT INTO map_grades VALUES('Harry Potter', 'Transfiguration', 'B+');
INSERT INTO map_grades VALUES('Ron Weasley', 'Transfiguration', 'B');
INSERT INTO map_grades VALUES('Hermione Granger', 'Transfiguration', 'A+');

INSERT INTO map_grades VALUES('Harry Potter', 'Potions', 'C-');
INSERT INTO map_grades VALUES('Ron Weasley', 'Potions', 'C+');
INSERT INTO map_grades VALUES('Hermione Granger', 'Potions', 'A');

