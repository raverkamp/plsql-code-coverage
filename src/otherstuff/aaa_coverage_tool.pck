create or replace package aaa_coverage_tool is

 procedure start_coverage(pck_name varchar2, id out integer);
 procedure add_line(cvr_id    integer,
                     stm_no    integer,
                     start_    integer,
                     end_      integer,
                     txt_start varchar2,
                     txt_end   varchar2);

  procedure end_coverage(id integer);

  procedure abort_coverage(id integer);
  procedure end_coverage(pck_name varchar2);

  procedure clean_up;

  type bool_tab is table of boolean;
  function get_statement_tab(id integer) return bool_tab;
end ;
/
create or replace package body aaa_coverage_tool is

  procedure fetch_source(object_name varchar2,object_type varchar2,source out clob) is
    tmpsrc clob;
  begin
   DBMS_LOB.CREATETEMPORARY (tmpsrc,true,DBMS_LOB.CALL);
    for r in (select *
                from user_source s
               where s.name = object_name
                 and s.TYPE = object_type
               order by s.line) loop
       DBMS_LOB.WRITEAPPEND(tmpsrc ,length(r.text), r.text);
    end loop;
    source := tmpsrc;
  end;

  procedure start_coverage(pck_name varchar2, id out integer) is
    body_source   clob;
    spec_source   clob;
    source2  clob;
    t         varchar2(200);
    iscovered integer;
    id_temp integer;
  begin
    select object_type
      into t
      from user_objects u
     where u.object_type = 'PACKAGE BODY'
       and u.object_name = pck_name;
    select count(*)
      into iscovered
      from aaa_coverage c
     where c.package_name = start_coverage.pck_name
       and c.is_covered = 1;
    if iscovered > 0 then
      raise_application_error(-20001,
                              'package ' || pck_name || ' is still covered');
    end if;

    delete from aaa_coverage_statements s
     where s.cvr_id in
           (select id from aaa_coverage c where c.package_name = pck_name);
    delete from aaa_coverage c where c.package_name = pck_name ;
    select aaa_coverage_seq.nextval into id_temp from dual;
    start_coverage.id := id_temp;
    insert into aaa_coverage
      (id, package_name, start_date, ORIGINAL_body_SOURCE,ORIGINAL_spec_SOURCE,is_covered)
    values
      (id_temp,pck_name, sysdate, EMPTY_cLOB(),empty_clob(),1)
    returning ORIGINAL_body_SOURCE,original_spec_source into body_source,spec_source;
   
    fetch_source(pck_name,'PACKAGE BODY',source2);
    dbms_lob.append(body_source,source2);
    fetch_source(pck_name,'PACKAGE',source2);
    dbms_lob.append(spec_source,source2);
  end;

  procedure end_coverage(pck_name varchar2) is
    size_  integer;
    cov_id integer;
  begin
    select count(*), max(c.id)
      into size_, cov_id
      from aaa_coverage c
     where c.package_name = end_coverage.pck_name
       and c.end_date is null;
    if size_ = 0 then
      raise_application_error(-20001, 'no coverage open for package: '|| pck_name);
    elsif size_ > 1 then
      raise_application_error(-20001,
                              'more than one coverage open for ' ||
                              pck_name);
    else
      end_coverage(cov_id);
    end if;
  end;

procedure end_coverage(id integer) is
    r          aaa_coverage%rowtype;
    sql_cursor integer;
  begin
    select * into r from aaa_coverage where id = end_coverage.id;
    update aaa_coverage c
       set c.end_date = sysdate,
           c.is_covered = 0
     where c.id = end_coverage.id;
  end;

  procedure abort_coverage(id integer) is
  begin
    update aaa_coverage c
       set c.end_date = null,
           c.start_date = null,
           c.is_covered = 0
     where c.id = abort_coverage.id;
  end;

  procedure add_line(cvr_id    integer,
                     stm_no    integer,
                     start_    integer,
                     end_      integer,
                     txt_start varchar2,
                     txt_end   varchar2) is
  begin
    insert into aaa_coverage_statements
      (id, cvr_id, stm_no, line_no, start_, END_, txt_start, txt_end, hit)
    values
      (aaa_coverage_seq.nextval,
       cvr_id,
       add_line.stm_no,
       -1,
       add_line.start_,
       add_line.end_,
       add_line.txt_start,
       add_line.txt_end,
       0);

  end;
 

  procedure clean_up is
  begin
    delete from aaa_coverage_statements;
    delete from aaa_coverage;
  end;

   function get_statement_tab(id integer) return bool_tab is
    res bool_tab := bool_tab();
    max_stm_no integer;
  begin
    select max(stm_no) into max_stm_no from aaa_coverage_statements s where s.cvr_id = get_statement_tab.id;
    if nvl(max_stm_no,0)=0 then
      return res;
    end if;
    res.extend(max_stm_no);
    for r in (select stm_no from aaa_coverage_statements s where s.cvr_id = get_statement_tab.id and hit=1) loop
      res(r.stm_no):=true;
    end loop;
    return res;
  end;
end;
/
