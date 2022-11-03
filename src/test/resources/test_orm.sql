DROP TABLE IF EXISTS test_orm;
CREATE TABLE test_orm  (
  id int AUTO_INCREMENT PRIMARY KEY,
  code varchar(255) ,
  name varchar(255) ,
  noup varchar(255) ,
  noins varchar(255) ,
  noselect varchar(255) ,
  createTime datetime,
  ref_id int ,
  ref_id2 int ,
  type varchar(255) ,
  int_val int ,
  type2 varchar(255)
) ;

DROP TABLE IF EXISTS test_orm_child_ref;
CREATE TABLE test_orm_child_ref  (
  id int AUTO_INCREMENT PRIMARY KEY,
  name varchar(255) 
) ;

DROP TABLE IF EXISTS test_orm_ref;
CREATE TABLE test_orm_ref  (
  id int AUTO_INCREMENT PRIMARY KEY,
  name varchar(255) ,
  ref_id int
) ;

