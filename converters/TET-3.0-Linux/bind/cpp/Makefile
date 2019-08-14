# cvsid: $Id: Makefile.sample,v 1.7 2008/12/15 20:26:53 rjs Exp $
#
# sample Makefile for TET applications
# This may have to be changed for your build environment


RM = rm
CPPFLAGS= -I../c/include
LDFLAGS	= -L../c/lib
LDLIBS	= tet.o -ltet -lm

# use the following for Mac OS X...
#LDLIBS	= tet.o -ltet -lm -framework ApplicationServices

# ...and make sure to set the search path for dynamic libraries, e.g.
# (in the shell) unless you already installed TET in the system:

#DYLD_LIBRARY_PATH=./lib make test

PROGS	= \
	dumper \
	extractor \
	fontfilter \
	tetml

all: tet.o $(PROGS)

test: all
	./dumper FontReporter.pdf
	./extractor FontReporter.pdf FontReporter
	./fontfilter FontReporter.pdf
	./tetml FontReporter.pdf FontReporter.tetml

clean:
	$(RM) $(PROGS) *.o FontReporter.tetml FontReporter.txt FontReporter*.tif
