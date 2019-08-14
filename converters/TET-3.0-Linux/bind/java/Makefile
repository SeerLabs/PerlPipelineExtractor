# Sample Makefile for TET's Java binding
# $Id: Makefile.sample,v 1.10 2008/12/30 14:12:25 rp Exp $


TESTPROGS	= \
	dumper.class \
	extractor.class \
	fontfilter.class \
	get_attachments.class \
	tetml.class

.SUFFIXES:	.java .class

.java.class:
	-CLASSPATH=$$CLASSPATH:TET.jar:. javac $<


all: $(TESTPROGS)

test: all
	-LIBPATH=$LIBPATH:. CLASSPATH=$$CLASSPATH:TET.jar:.  java dumper FontReporter.pdf 
	-LIBPATH=$LIBPATH:. CLASSPATH=$$CLASSPATH:TET.jar:.  java extractor FontReporter.pdf FontReporter
	-LIBPATH=$LIBPATH:. CLASSPATH=$$CLASSPATH:TET.jar:.  java fontfilter FontReporter.pdf 
	-LIBPATH=$LIBPATH:. CLASSPATH=$$CLASSPATH:TET.jar:.  java tetml FontReporter.pdf FontReporter.tetml
	-LIBPATH=$LIBPATH:. CLASSPATH=$$CLASSPATH:TET.jar:.  java get_attachments FontReporter.pdf attachments.txt

clean:
	$(RM) *.class
	$(RM) FontReporter.pdf dumper.txt FontReporter.txt fontfilter.txt
	$(RM) FontReporter.tetml FontReporter*.tif attachments.txt m.out
