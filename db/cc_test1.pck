create or replace package cc_test1 as

procedure p1;

function f1(x varchar2) return varchar2;
pragma restrict_references(f1,'WNDS');

function f2(x varchar2) return varchar2 deterministic;

function f3(x varchar2) return varchar2 deterministic PARALLEL_ENABLE;

end;
/
show errors

create or replace package body cc_test1 as
procedure p1 is
function f1(x varchar2) return varchar2 is
begin
  return x||'x';
end;
begin
 null;
 dbms_output.put_line(f1('bla'));
end;

function f1(x varchar2) return varchar2 is
begin
return x||x;
end;

function f2(x varchar2) return varchar2 deterministic is
begin
  return 'd'||x;
end;

function f3(x varchar2) return varchar2 deterministic PARALLEL_ENABLE is
begin
 return 'e'||x;
end;

end;
/
show errors