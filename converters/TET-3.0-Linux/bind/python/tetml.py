# TET sample application for dumping PDF information in the XML language TETML
#
# $Id: tetml.py,v 1.4 2008/12/23 17:31:12 rjs Exp $
#/

import sys
from tetlib_py import *

# global option list */
globaloptlist = "searchpath={../data ../../../resource/cmap}"

# document-specific option list */
basedocoptlist = ""

# page-specific option list */
pageoptlist = "granularity=word"

# set this to 1 to generate TETML output in memory */
inmemory = 0


if len(sys.argv) != 3:
    raise Exception("usage: tetml <pdffilename> <xmlfilename>\n")

try:
    try:
	pageno = 0
	tet = TET_new()

        TET_set_option(tet, globaloptlist)

	if (inmemory):
	    docoptlist =  "tetml={} %s" % basedocoptlist
	else:
	    docoptlist =  "tetml={filename={%s}} %s" % \
				(sys.argv[2], basedocoptlist)

        doc = TET_open_document(tet, sys.argv[1], docoptlist)

        if (doc == -1):
            raise Exception("Error %d in %s(): %s" % \
               (TET_get_errnum(tet), TET_get_apiname(tet), TET_get_errmsg(tet)))

        n_pages = int(TET_pcos_get_number(tet, doc, "length:pages"))

	# loop over pages in the document */
	for pageno in range(1, n_pages+1):
            TET_process_page(tet, doc, pageno, pageoptlist)

	# This could be combined with the last page-related call */
	TET_process_page(tet, doc, 0, "tetml={trailer}")

	if (inmemory):
            fp = open(sys.argv[2], "wb")

	    # Retrieve the generated TETML data from memory. Since we have
	    # only a single call the result will contain the full TETML.
	    #/

	    tetml = TET_get_xml_data(tet, doc, "")
	    if (not tetml):
		raise Exception("tetml: couldn't retrieve XML data")

	    fp.fwrite(tetml)
	    fp.close()

        TET_close_document(tet, doc)

    except TETlibException:
	print("\nError " + repr(TET_get_errnum(tet)) + "in " + \
		TET_get_apiname(tet) + "() on page " + repr(pageno) \
		+ ": " + TET_get_errmsg(tet) + "\n")

    except Exception, e:
	print "Exception occurred:"
	print e

finally:
    TET_delete(tet)
