create or replace package body cc_test1 as
procedure p1 is
function f1(x varchar2) return varchar2 is
begin
  return x||'x';
end;
begin
 null;
 dbms_output.put_line(f1('bla'));
 null;
 null;
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

procedure bla(x in varchar2) is
begin
  if x is null then
    dbms_output.put_line('null');
  else
    case x 
      when 'a' then dbms_output.put_line('A');
      when 'b' then dbms_output.put_line('B');
      else dbms_output.put_line('neither A or B'); 
    end case;
  end if;
end;

procedure a2(x integer) is
  procedure b(y integer) is
  begin
    null;
  end;
begin
  null;
  declare
    procedure c(z integer) is
    begin
      null;
    end;
   begin
     c(2);
     null;
   end;
   null;
   b(7);
end;

end;
/