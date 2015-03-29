create or replace package cannotparsespec as
 $if $$PLSQL_DEBUG  $then 
    x integer;
  $end
end;
/