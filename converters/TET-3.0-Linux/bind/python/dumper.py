# TET sample application for dumping PDF information with pCOS
#
# $Id: dumper.py,v 1.6 2008/12/15 20:26:54 rjs Exp $
#

import sys
from tetlib_py import *

def yesno(arg):
    if (arg != 0):
	return "yes"
    return "no"

if len(sys.argv) != 2:
    raise Exception("usage: dumper <filename>")

try:
    try:
	tet = TET_new()

	searchpath = "searchpath={../data}"
	docoptlist = "requiredmode=minimum"
	globaloptlist = ""

	TET_set_option(tet, searchpath)
	TET_set_option(tet, globaloptlist)

	doc = TET_open_document(tet, sys.argv[1], docoptlist)
	if doc  == -1:
	    raise Exception("ERROR: %s\n" % TET_get_errmsg(tet))

	# --------- general information (always available) */
	pcosmode = int(TET_pcos_get_number(tet, doc, "pcosmode"))

	print("   File name: %s" % TET_pcos_get_string(tet, doc, "filename"))

	print(" PDF version: %s" % \
	    TET_pcos_get_string(tet, doc, "pdfversionstring"))

	print("  Encryption: %s" % \
	    TET_pcos_get_string(tet, doc, "encrypt/description"))

	print("   Master pw: %s" % \
	    yesno(TET_pcos_get_number(tet, doc, "encrypt/master")))

	print("     User pw: %s" % \
	    yesno(TET_pcos_get_number(tet, doc, "encrypt/user")))

	print("Text copying: %s" % \
	    yesno(not TET_pcos_get_number(tet, doc, "encrypt/nocopy")))

	print("  Linearized: %s" % \
	    yesno(TET_pcos_get_number(tet, doc, "linearized")))

	if (pcosmode == 0):
	    print("Minimum mode: no more information available\n")
	    TET_close_document(tet, doc)
	    exit(0)

	# --------- more details (requires at least user password) */

	print("PDF/X status: %s" % TET_pcos_get_string(tet, doc, "pdfx"))

	print("PDF/A status: %s" % TET_pcos_get_string(tet, doc, "pdfa"))

	print("  Tagged PDF: %s\n" % \
	    yesno(TET_pcos_get_number(tet, doc, "tagged")))

	print("No. of pages: %d" % \
	    int(TET_pcos_get_number(tet, doc, "length:pages")))

	print(" Page 1 size: width=%g, height=%g" % \
	    (TET_pcos_get_number(tet, doc, "pages[%d]/width" % 0),
	     TET_pcos_get_number(tet, doc, "pages[%d]/height" % 0)))

	count = int(TET_pcos_get_number(tet, doc, "length:fonts"))
	print("No. of fonts: %d" % count)

	for i in range(count):
	    if (TET_pcos_get_number(tet, doc, "fonts[%d]/embedded" % i)):
		print "embedded",
	    else:
		print "unembedded",

	    print("%s font" % \
		TET_pcos_get_string(tet, doc, "fonts[%d]/type" % i)),
	    print("%s" % \
		TET_pcos_get_string(tet, doc, "fonts[%d]/name" % i))

	print

	plainmetadata = \
		int(TET_pcos_get_number(tet, doc, "encrypt/plainmetadata"))

	if (pcosmode == 1 and not plainmetadata and \
		int(TET_pcos_get_number(tet, doc, "encrypt/nocopy"))):
	    print("Restricted mode: no more information available\n")
	    TET_close_document(tet, doc)
	    exit(0)

	# --------- document info keys and XMP metadata (requires master pw
	# or plaintext metadata)
	#

	count = int(TET_pcos_get_number(tet, doc, "length:/Info"))

	for i in range(0, count):
	    objtype = int(TET_pcos_get_number(tet, doc, "type:/Info[%d]" % i))
	    print("%12s:" % TET_pcos_get_string(tet,doc, "/Info[%d].key" % i)),

	    # Info entries can be stored as string or name objects */
	    # pcos_to_sting == 4; pcos_ot_name == 3
	    if (objtype == 4 or objtype == 3):
		print("'%10s'" % \
		    TET_pcos_get_string(tet, doc, "/Info[%d]" % i))
	    else:
		print("(%s object)" % \
		    TET_pcos_get_string(tet, doc, "type:/Info[%d]" % i))

	print("\nXMP meta data:"),

	objtype = int(TET_pcos_get_number(tet, doc, "type:/Root/Metadata"))
	# pcos_ot_stream == 7
	if (objtype == 7):
	    contents = TET_pcos_get_stream(tet, doc, "", "/Root/Metadata")
	    print("%d bytes" % len(contents)),

	    ustring = unicode(contents, "UTF-8")
	    print("(%d Unicode characters)\n" % len(ustring))
	else:
	    print("not present\n")

	TET_close_document(tet, doc)

    except TETlibException:
	print("\nError " + repr(TET_get_errnum(tet)) + "in " + \
		TET_get_apiname(tet) + ": " + TET_get_errmsg(tet) + "\n")

    except Exception, e:
	print "Exception occurred:"
	print e

finally:
    TET_delete(tet)
