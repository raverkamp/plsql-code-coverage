create table AAA_COVERAGE
(
  ID              INTEGER not null,
  PACKAGE_NAME    VARCHAR2(30) not null,
  ORIGINAL_body_SOURCE CLOB,
  original_spec_source clob,
  IS_COVERED      number(1) not null,
  START_DATE      DATE,
  END_DATE        DATE,
  hash_of_covered_code raw(256) -- just make it large enough for maybe other hashes
);

alter table AAA_COVERAGE
  add primary key (ID);

alter table AAA_COVERAGE
  add unique (PACKAGE_NAME);

alter table aaa_coverage 
  add check(is_covered in (0,1));

alter table aaa_coverage 
  add check(not (is_covered = 1) or (end_date is null));

create table AAA_COVERAGE_STATEMENTS
(
  CVR_ID    INTEGER not null,
  STM_NO    INTEGER not null,
  LINE_NO   INTEGER not null,
  TXT_START VARCHAR2(2000) not null,
  TXT_END   VARCHAR2(2000) not null,
  HIT       INTEGER not null,
  START_    INTEGER not null,
  END_      INTEGER not null
);

alter table AAA_COVERAGE_STATEMENTS
  add primary key (cvr_id,stm_no);

alter table AAA_COVERAGE_STATEMENTS
  add foreign key (CVR_ID)
  references AAA_COVERAGE (ID);

create sequence aaa_coverage_seq;
