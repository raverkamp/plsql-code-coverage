create or replace package body cannotparse as

procedure p1 is
  subtype int01 is binary_integer range 0..1;
  a int01;
begin
  a:= 1;
  null;
end;

end;
/