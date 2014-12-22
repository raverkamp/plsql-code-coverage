create or replace package body csv is

  g_p integer;

  k_clob    integer := 1;
  k_varchar integer := 2;

  g_what integer;

  g_clob clob;

  g_quote_char varchar2(1);
  g_delim_char varchar2(1);

  g_varchar2 varchar2(32000);

procedure p(x varchar2) is
  begin
    null;
    dbms_output.put_line(x);
  end;

  function instr2(y varchar2, p integer) return integer is
  begin
    if g_what = k_clob then
      return dbms_lob.instr(lob_loc => g_clob, pattern => y, offset => p);
    elsif g_what = k_varchar then
      return instr(g_varchar2, y, p);
    end if;
  end;

 function substrclob(i_from integer, i_to1 integer) return varchar2 is
    begin
     if i_from+4000>=i_to1 then
       return dbms_lob.substr(lob_loc => g_clob,
                             amount  => i_to1 - i_from,
                             offset  => i_from);
      else
       declare a integer:= trunc((i_to1 +i_from)/2);
       begin
         return substrclob(i_from,a);
        end;
       end if;
      end;


  function substr2(i_from integer, i_to1 integer) return varchar2 is
  begin
   if g_what = k_clob then
      return substrclob(i_from,i_to1);
    elsif g_what = k_varchar then
      return substr(g_varchar2, i_from, i_to1 - i_from);
    end if;
  end;


  function len2 return integer is
  begin
   if g_what = k_clob then
      return nvl(dbms_lob.getlength(g_clob), 0);
    elsif g_what = k_varchar then
      return nvl(length(g_varchar2), 0);
    end if;
  end;

  procedure clob_init(i_clob in clob, i_delim varchar2, i_quote varchar2) is
  begin
    g_what := k_clob;
    g_clob := i_clob;
    g_p    := 1;
    set_chars(i_delim, i_quote);
  end;

  procedure varchar_init(i_varchar2 in varchar2,
                         i_delim    varchar2,
                         i_quote    varchar2) is
  begin
    g_what := k_varchar;
    g_p    := 1;
    g_varchar2 := i_varchar2;
    set_chars(i_delim, i_quote);
  end;

  procedure set_chars(i_delim varchar2, i_quote varchar2) is
  begin
    g_delim_char := i_delim;
    g_quote_char := i_quote;
  end;

  procedure reset(p integer) is
  begin
    g_p := p;
    --   if p + g_p - 1 > nvl(length(g_buff), 0) + 1 then
    --     raise_application_error(-20001, 'bug');
    --    end if;
    --   g_buff := substr(g_buff, p + g_p - 1); --, nvl(length(g_buff), 0) - p + 1);
    --   g_p    := 1;
  end;

  function past_end(x integer) return boolean is
  begin
    return x > len2;
  end;

  procedure skip_to_end(i_pos integer, o_state out varchar2) is
    -- i_pos is one after end of quotet field
    -- skip to pos after end of line

    posd  integer;
    posnl integer;
  begin
    posd  := instr2(g_delim_char, i_pos);
    posnl := instr2(chr(10), i_pos);
    if posd > 0 or posnl > 0 then
      -- an end was found !
      if (posd > 0 and (posnl = 0 or posnl > posd)) then
        -- separator found
        reset(posd + 1);
        o_state := k_found_delim;
        return;
      else
        -- end of line found
        reset(posnl + 1);
        o_state := k_found_nl;
        return;
      end if;
    else
      o_state := k_found_eof;
      reset(len2 + 1);
    end if;
  end;

  procedure next_quoted_field(o_field out varchar2, o_state out integer) is
    x     integer;
    q_pos integer;
  begin
    p('q '||g_p);
    -- first char is quote char => v is not null!
    x:= g_p+1;
    loop
      --  if past_end(x) then
      --    -- end of file
      --    -- quoted field is not closed  ... play nice
      --     o_field := replace(substr2(2, len2 + 1),
      --                        g_quote_char || g_quote_char,
      --                        g_quote_char);
      --    o_state := k_found_nl;
      --     reset(x); -- := length(v)+1;
      --     return;
      --   end if;
      q_pos := instr2(g_quote_char, x);
      if q_pos = 0 then
        -- not closed
        o_field := replace(substr(g_p + 1, len2 + 1),
                           g_quote_char || g_quote_char,
                           g_quote_char);
        o_state := k_found_eof;
        return;
      end if;
      -- found quote char
      -- we need the next char ...
      if past_end(q_pos + 1) then
        -- no next char
        o_field := replace(substr2(g_p + 1, q_pos),
                           g_quote_char || g_quote_char,
                           g_quote_char);
        o_state := k_found_eof;
        reset(q_pos + 1);
        return;
      end if;
      if substr2(q_pos + 1, q_pos + 2) != g_quote_char then
        -- at the end of field
        p('---'||q_pos);
        o_field := replace(substr2(g_p + 1, q_pos),
                           g_quote_char || g_quote_char,
                           g_quote_char);
        skip_to_end(q_pos + 1, o_state);
        return;
      else
        x := q_pos + 2;
      end if;
    end loop;
  end;

  -- buffer is string
  -- start position is integer
  -- on next field both is set
  -- at end if
  -- g_p> length(buffer)
  -- g_buff and g_p can change with calls to getchars
  -- getchars always works, returns if chars where found
  -- reset set
  procedure next_field(o_field out varchar2, o_state out integer) is
    res   varchar2(32000);
    posd  integer;
    posnl integer;
    --  posq integer;
  begin
    p('NF:' ||g_p);
    if past_end(g_p) then
      o_field := null;
      o_state := k_found_eof;
      return;
    end if;

    -- if the first char is quote it is a quotet field,
    -- may be less conditions
    if substr2(g_p, g_p + 1) = g_quote_char then
      next_quoted_field(o_field, o_state);
      return;
    end if;
    posd  := instr2(g_delim_char, g_p);
    posnl := instr2(chr(10), g_p);
    p('pos: '||posd||' '||posnl);
    if posd = 0 and posnl = 0 then
      o_field := substr2(g_p, len2 + 1);
      o_state := k_found_eof;
      reset(len2+1);
      return;
    end if;
    if posd != 0 and (posd < posnl or posnl = 0) then
      -- found sep and later no or later NL
      res     := substr2(g_p, posd);
      o_field := res;
      o_state := k_found_delim;
      reset(posd + 1);
      return;
    else
      res     := substr2(g_p, posnl);
      o_field := res;
      reset(posnl + 1);
      o_state := k_found_nl;
      return;
    end if;
  end;

end csv;
/