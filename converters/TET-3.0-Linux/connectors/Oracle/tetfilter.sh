#!/bin/sh
#
# Wrapper script for using the TET command line tool as a filter for
# PDF documents in Oracle Text.
#
# Put this file into $ORACLE_HOME/ctx/bin.
#
# $Id: tetfilter.sh,v 1.3 2008/11/27 15:34:10 stm Exp $

# Change TETDIR to the installation directory of TET:
TETDIR="/home/user/TET-3.0-Linux"

# Change PDFLIBLICENSEFILE to point to your PDFlib license file containing the
# TET license:
PDFLIBLICENSEFILE="$HOME/license.txt"
export PDFLIBLICENSEFILE

# Option list for TET_open_document():
TETOPT=""
# For specifying the license key directly and not via a license file add it
# to TETOPT:
# TETOPT="license aaaaaaa-bbbbbb-cccccc-dddddd-eeeeee"

# Option list for TET_open_document():
DOCOPT=""

# Option list for TET_open_page() or TET_process_page():
PAGEOPT=""

$TETDIR/bin/tet -v 0 --searchpath "$TETDIR/resource/cmap" \
	--searchpath "$TETDIR/resource" \
	--tetopt "$TETOPT" \
	--docopt "$DOCOPT" \
	--pageopt "$PAGEOPT" \
	-o "$2" "$1"

exit $?
