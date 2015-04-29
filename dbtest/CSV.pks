create or replace package csv is

  k_found_delim integer := 1;
  k_found_nl    integer := 2;
  k_found_eof   integer := 3;

  -- lines are separated by chr(10)
  --   what about chr(13) at the end of field, cut it off?
  --

  -- the whole thing can be parsed,

  procedure set_chars(i_delim varchar2, i_quote varchar2);

  procedure clob_init(i_clob in clob, i_delim varchar2, i_quote varchar2);
  procedure varchar_init(i_varchar2 in varchar2,
                         i_delim    varchar2,
                         i_quote    varchar2);

  -- return the next field and a code for what ended the field
  --   1 : found a delimiter
  --   2 : end of line
  --   3 : end of file and no value in o_field
  --  what do we do if this is called after the end of file?
  --  what about transactions?
  procedure next_field(o_field out varchar2, o_state out integer);

end csv;
/