Rem SQL script for cleaning up the TET test data, example B
Rem $Id: tetcleanup_b.sql,v 1.1 2008/12/01 10:52:15 stm Exp $
drop index tetindex_b;
execute ctx_ddl.drop_preference('pdf_filter_b')
drop table pdftable_b;
commit;
