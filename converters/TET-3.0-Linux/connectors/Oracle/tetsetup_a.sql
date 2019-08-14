Rem SQL script for testing TET as a filter for PDF documents in Oracle Text,
Rem example A
Rem $Id: tetsetup.sql,v 1.2 2008/11/26 10:15:38 stm Exp $

Rem Change tetpath to the directory where the TET package was extracted
define tetpath = '/home/user/TET-3.0-Linux'

Rem Create table for PDF documents. The documents are stored on disk,
Rem column "pdffile" contains the full pathname to the document.
create table pdftable_a (pk number primary key, pdffile varchar2(2000));
insert into pdftable_a values(1, '&tetpath/bind/data/FontReporter.pdf');
insert into pdftable_a values(2, '&tetpath/bind/data/Whitepaper-XMP-metadata-in-PDFlib-products-J.pdf');
insert into pdftable_a values(3, '&tetpath/bind/data/Whitepaper-XMP-metadata-in-PDFlib-products.pdf');
insert into pdftable_a values(4, '&tetpath/bind/data/Whitepaper-PDFA-with-PDFlib-products-J.pdf');
insert into pdftable_a values(5, '&tetpath/bind/data/Whitepaper-PDFA-with-PDFlib-products.pdf');
insert into pdftable_a values(6, '&tetpath/bind/data/PDFlib-datasheet.pdf');
insert into pdftable_a values(7, '&tetpath/bind/data/TET-PDF-IFilter-datasheet.pdf');
commit;

Rem Create preferences for specifying the datastore and the user filter
Rem command 
execute ctx_ddl.create_preference ('pdf_datastore_a', 'file_datastore')
execute ctx_ddl.create_preference ('pdf_filter_a', 'user_filter')
execute ctx_ddl.set_attribute ('pdf_filter_a', 'command', 'tetfilter.sh')

Rem Create the full-text index with the file datastore and the user filter
create index tetindex_a on pdftable_a (pdffile) indextype is ctxsys.context
parameters ('datastore pdf_datastore_a filter pdf_filter_a');

commit;
