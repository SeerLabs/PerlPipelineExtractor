# Sample Makefile for TET's Perl binding
# $Id: Makefile.sample,v 1.7 2008/12/15 20:26:53 rjs Exp $


PERLBIN = perl -I. -Iperl58

all: test

test::
	$(PERLBIN) dumper.pl FontReporter.pdf
	$(PERLBIN) extractor.pl FontReporter.pdf FontReporter
	$(PERLBIN) fontfilter.pl FontReporter.pdf
	$(PERLBIN) tetml.pl FontReporter.pdf FontReporter.tetml

clean::
	$(RM) FontReporter.pdf dumper.txt FontReporter.txt fontfilter.txt
	$(RM) FontReporter.tetml FontReporter*.tif attachments.txt
