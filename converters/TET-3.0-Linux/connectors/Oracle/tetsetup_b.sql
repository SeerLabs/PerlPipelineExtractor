Rem SQL script for testing TET as a filter for PDF documents in Oracle Text,
Rem example B
Rem $Id: tetsetup_b.sql,v 1.1 2008/12/01 10:52:15 stm Exp $
create table pdftable_b(pk number primary key, title varchar(200),
	pagecount number, pdffile blob);
commit;

execute ctx_ddl.create_preference ('pdf_filter_b', 'user_filter')
execute ctx_ddl.set_attribute ('pdf_filter_b', 'command', 'tetfilter.sh')

create index tetindex_b on pdftable_b (pdffile) indextype is ctxsys.context
parameters ('datastore CTXSYS.DEFAULT_DATASTORE filter pdf_filter_b');
commit;
