# cvsid: $Id: Makefile.sample,v 1.9 2008/12/15 20:26:52 rjs Exp $
#
# sample Makefile for PDFlib applications
# This may have to be changed for your build environment


RM = rm
CFLAGS	= -I./include
LDFLAGS	= -L./lib
# use the following for AIX
#LDFLAGS	= -L./lib -ltet -lm

LDLIBS	= -ltet -lm
# use the following for Mac OS X:
#LDLIBS	= -ltet -lm -framework ApplicationServices

PROGS	= \
	dumper \
	extractor \
	fontfilter \
	get_attachments \
	tetml

all: $(PROGS)

test: $(PROGS)
	./dumper FontReporter.pdf
	./extractor FontReporter.pdf FontReporter
	./fontfilter FontReporter.pdf
	./tetml FontReporter.pdf FontReporter.tetml
	./get_attachments FontReporter.pdf attachments.txt


clean:
	$(RM) $(PROGS) *.pdf *.o
