#!/usr/bin/python
# Extract text from PDF and filter according to font name and size.
# This can be used to identify headings in the document and create a
# table of contents.
#
# $Id: fontfilter.py,v 1.3 2008/12/23 17:31:12 rjs Exp $
#/

import sys
from tetlib_py import *


# global option list */
globaloptlist = "searchpath={../data ../../../resource/cmap}"

# document-specific option list */
docoptlist = ""

# page-specific option list */
pageoptlist = "granularity=line"

# Search text with at least this size (use 0 to catch all sizes) */
fontsizetrigger = 10

# Catch text where the font name contains this string
# (use empty string to catch all font names)
#/
fontnametrigger = "Bold"

if len(sys.argv) != 2:
    raise Exception("usage: fontfilter <infilename>\n")

try:
    try:
	pageno = 0

	tet = TET_new()

	TET_set_option(tet, globaloptlist)

	doc = TET_open_document(tet, sys.argv[1], docoptlist)

	if (doc == -1):
	    raise Exception("Error %d in %s(): %s\n" % \
		(TET_get_errnum(tet), TET_get_apiname(tet), \
		TET_get_errmsg(tet)))

	# get number of pages in the document */
	n_pages = int(TET_pcos_get_number(tet, doc, "length:pages"))

	# loop over pages in the document */
	for pageno in range(1, n_pages+1):
	    page = TET_open_page(tet, doc, pageno, pageoptlist)

	    if (page == -1):
		print ("Error %d in %s() on page %d: %s\n" % \
		    (TET_get_errnum(tet), TET_get_apiname(tet), pageno, \
		     TET_get_errmsg(tet)))
		continue                        # try next page */

	    # Retrieve all text fragments for the page */
	    text = TET_get_text(tet, page)
	    while (text):
		# Loop over all characters */
		ci = TET_get_char_info(tet, page)
		while (ci):
		    # We need only the font name and size the text 
		    # position could be fetched from ci->x and ci->y.
		    #/
		    fontname = TET_pcos_get_string(tet, doc, \
				"fonts[%d]/name" % ci["fontid"])

		    # Check whether we found a match */
		    # C only: some versions of strstr don't allow empty
		    # strings, so we better check */
		    if (ci["fontsize"] >= fontsizetrigger and \
				fontname.find(fontnametrigger) != -1):
			# print the retrieved font name, size, and text */
			print ("[%s %.2f] %s" % \
			    (fontname, ci["fontsize"], text))
		    ci = TET_get_char_info(tet, page)
		    # In this sample we check only the first character of
		    # each fragment.
		    #/
		    break
		text = TET_get_text(tet, page)
	    if (TET_get_errnum(tet) != 0):
		raise Exception("Error %d in %s() on page %d: %s\n" % \
		    (TET_get_errnum(tet), TET_get_apiname(tet), pageno, \
		     TET_get_errmsg(tet)))

	    TET_close_page(tet, page)

	TET_close_document(tet, doc)


    except TETlibException:
	if (pageno == 0):
	    print("\nError " + repr(TET_get_errnum(tet)) + "in " + \
		TET_get_apiname(tet) + ": " + TET_get_errmsg(tet) + "\n")
	else:
	    print("\nError " + repr(TET_get_errnum(tet)) + "in " + \
		TET_get_apiname(tet) + "() on page " + repr(pageno) \
		+ ": " + TET_get_errmsg(tet) + "\n")

    except Exception, e:
	print "Exception occurred:"
	print e

finally:
    TET_delete(tet)
