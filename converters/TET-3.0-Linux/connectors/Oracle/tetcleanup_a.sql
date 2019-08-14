Rem SQL script for cleaning up the TET test data, example A
Rem $Id: tetcleanup.sql,v 1.2 2008/11/26 10:15:38 stm Exp $
drop index tetindex_a;
execute ctx_ddl.drop_preference('pdf_datastore_a')
execute ctx_ddl.drop_preference('pdf_filter_a')
drop table pdftable_a;
commit;
