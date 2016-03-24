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


procedure w30wwwwwwwwwwwwwwwwwwwwwwwwwww is
begin
  null;
end;

procedure publik(x varchar2) is
begin
  null;
end;

procedure publik(y varchar2) is
begin
  null;
end;

procedure syntax is
x integer;
begin
  x:= extract(year from sysdate);
end;

procedure intervaltest is
a interval day to second;
begin
 a:= interval '1' hour;
 a:= interval '2' minute;
 a := interval '3' second;
end;

procedure numbers is
  v_num number;
  v_val number;
begin
  v_num:= 1234 + (to_number(v_val) - 1.4) / 0.1;
end;

procedure sqlrowcount is
a integer;
b integer;
v varchar2(200);
begin
  dbms_output.put_line('#######################');
  a:= sql%rowcount;
  update atable set y=upper(y);
  a:= sql%rowcount;
  if a!=4 then
    raise_application_error(-20000,'fail 1');
  end if;
  update atable set y=upper(y) where 1=2;
  execute immediate 'update atable set y=upper(y)';
  a:= sql%rowcount;
  if a!=4 then
    raise_application_error(-20000,'fail 2');
  end if;
  
  for r in (select * from atable )loop 
   a:= sql%rowcount;
  dbms_output.put_line('l '||a);
  exit;
  end loop;
   a:= sql%rowcount;
   if a!=4 then
    raise_application_error(-20000,'fail 3');
   end if;
  execute immediate 'begin dbms_output.put_Line(''aaaa'');end;';
  a:= sql%rowcount;
  if nvl(a,-1)!=1 then
      raise_application_error(-20000,'fail 4');
  end if;
  update atable set y =upper(y) where 1=2;
  select dummy into v from dual;
  a:= sql%rowcount;
  if nvl(a,-1)!=1 then
    raise_application_error(-20000,'fail 5');
  end if;
   select dummy into v from dual;
   if to_number(to_char(sql%rowcount)) != 1 then
     raise_application_error(-20000,'fail 6');
   end if;
end;

-- luckily sql is a keyword in
procedure abc is
  cursor c is select * from dual;
  a varchar2(200);
  cursor "SQL" is select * from dual;
begin
  open c;
  fetch c into a;
  if abc.c%found then
    dbms_output.put_line('found');
  end if;
  open "SQL";
  fetch "SQL" into a;
  if abc."SQL"%found then
    dbms_output.put_line('found');
  end if;
end;

procedure test_forall_ind is
type tab_tp is table of integer index by binary_integer;
tab tab_tp;
x integer;
begin
for i in 1.. 10 loop
  tab(i*4) := i;
end loop;
forall j in INDICES of tab
   update atable
   set y = tab(j)
   where x = tab(j);
end;

procedure test_forall_val is
type tab_tp is table of binary_integer index by binary_integer;
tab tab_tp;
x integer;
begin
for i in 1.. 10 loop
  tab(i*4) := i;
end loop;
forall j in values of tab
   update atable
   set y = tab(j)
   where x = tab(j);
end;



end;
/