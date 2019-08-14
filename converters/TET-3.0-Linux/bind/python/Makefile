# Sample Makefile for TET's Python binding
# $Id: Makefile.sample,v 1.6 2008/12/15 20:26:54 rjs Exp $


all: test

test::
	python dumper.py FontReporter.pdf
	python extractor.py FontReporter.pdf FontReporter
	python fontfilter.py FontReporter.pdf
	python tetml.py FontReporter.pdf FontReporter.tetml

clean::
	$(RM) FontReporter.pdf dumper.txt FontReporter.txt fontfilter.txt
	$(RM) FontReporter.tetml FontReporter*.tif attachments.txt m.out
