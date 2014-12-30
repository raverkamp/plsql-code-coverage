create or replace package cc_test1 as

procedure p1;

function f1(x varchar2) return varchar2;
pragma restrict_references(f1,'WNDS');

function f2(x varchar2) return varchar2 deterministic;

function f3(x varchar2) return varchar2 deterministic PARALLEL_ENABLE;

procedure bla(x in varchar2);

procedure w30wwwwwwwwwwwwwwwwwwwwwwwwwww;

end;
/