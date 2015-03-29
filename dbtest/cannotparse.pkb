create or replace package body cannotparse as

zero CONSTANT pls_integer := 0;

procedure p1 is
  subtype int01 is binary_integer range -1 .. 1 ;
  a int01;
begin
  a:= 1;
  null;
  $if $$PLSQL_DEBUG  $then 
    null; 
  $end
  dbms_output.put_line(a);
end;

end;
/