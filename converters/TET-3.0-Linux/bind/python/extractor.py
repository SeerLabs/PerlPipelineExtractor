#!/usr/bin/python
# Simple Python text and image extractor based on PDFlib TET
#
# $Id: extractor.py,v 1.9 2009/01/21 20:26:16 rjs Exp $
#

import sys
import codecs
from tetlib_py import *

# global option list */
globaloptlist = "searchpath={../data ../../../resource/cmap}"

# document-specific option list */
docoptlist = ""

# page-specific option list */
pageoptlist = "granularity=page"

# separator to emit after each chunk of text. This depends on the
# application's needs; for granularity=word a space character may be useful.
#
separator = "\n"

# basic image extract options (more below) */
baseimageoptlist = "compression=auto format=auto"

# set this to 1 to generate image data in memory */
inmemory = 0

suffix = ".txt"

if len(sys.argv) < 2 or len(sys.argv) > 3:
    raise Exception("usage: extractor <infilename> [<outfilename>]\n")

try:
    try:

	tet = TET_new()

	if len(sys.argv) == 3:
	    outfilebase = sys.argv[2]
	else:
	    outfilebase = sys.argv[1]

	#fp = codecs.open(outfilebase + suffix, 'w', 'utf-8')
	fp = open(outfilebase + suffix, 'w')

	TET_set_option(tet, globaloptlist)

	doc = TET_open_document(tet, sys.argv[1], docoptlist)

	if (doc == -1):
	    raise Exception("Error " + TET_get_errnum(tet) + "in " \
		+ TET_get_apiname(tet) + "(): " + TET_get_errmsg(tet))

	# get number of pages in the document */
	n_pages = TET_pcos_get_number(tet, doc, "length:pages")

	# loop over pages in the document */
	for pageno in range(1, int(n_pages)+1):
	    imageno = -1

	    page = TET_open_page(tet, doc, pageno, pageoptlist)

	    if (page == -1):
		print "Error " + TET_get_errnum(tet) + "in " \
		    + TET_get_apiname(tet) + "(): " + TET_get_errmsg(tet)
		continue                        # try next page */

	    # Retrieve all text fragments; This is actually not required
	    # for granularity=page, but must be used for other granularities.
	    #
	    text = TET_get_text(tet, page)
	    while (text):
		fp.write(text)  # print the retrieved text */
		# print a separator between chunks of text */
		fp.write(separator)
		text = TET_get_text(tet, page)

	    # Retrieve all images on the page */
	    image = TET_get_image_info(tet, page)
	    while (image):

		imageno = imageno + 1

		# Print the following information for each image:
		# - page and image number
		# - pCOS id (required for indexing the images[] array)
		# - physical size of the placed image on the page
		# - pixel size of the underlying PDF image
		# - number of components, bits per component, and colorspace
		#
		width = TET_pcos_get_number(tet, doc, \
			    "images[" + repr(image["imageid"]) + "]/Width")
		height = TET_pcos_get_number(tet, doc, \
			    "images[" + repr(image["imageid"]) + "]/Height")
		bpc = TET_pcos_get_number(tet, doc, \
			    "images[" + repr(image["imageid"]) + "]/bpc")
		cs = TET_pcos_get_number(tet, doc, \
			    "images[" + repr(image["imageid"]) + "]/colorspaceid")

		txt = "page %d, image %d: id=%d, %.2fx%.2f point, " % \
		   (pageno, imageno, image["imageid"], image["width"], \
		    image["height"]) + "%dx" % width + "%d pixel, " % height
		if (cs != -1):
		    txt = txt + "%dx" % \
		       TET_pcos_get_number(tet, doc, \
			    "colorspaces[%d]/components" \
			       % cs) + "%d bit " % bpc + "%s" % \
		       TET_pcos_get_string(tet, doc, \
			    "colorspaces[%d]/name" % cs)
		else:
		    # cs==-1 may happen for some JPEG 2000 images. bpc,
		    # colorspace name and number of components are not
		    # available in this case.
		    txt = txt + "JPEG2000"

		print txt

		if (inmemory):
		    # Fetch the image data and store it in memory */
		    imageoptlist = baseimageoptlist

		    imagedata = TET_get_image_data(tet, doc, \
				    image["imageid"], imageoptlist)

		    if (imagedata == NULL):
			raise Exception ("\nError " + repr(TET_get_errnum(tet)) \
				+ "in " + TET_get_apiname(tet) + "() on page " + \
				repr(pageno) + ": " + TET_get_errmsg(tet) + "\n")
			continue                 # process next image */

		    # Client-specific image data consumption would go here
		    # We simply report the size of the data.
		    #
		    print("Page " + repr(pageno) + ": " + repr(length) \
			+ "d bytes of image data\n")
		else:
		    #
		    # Fetch the image data and write it to a disk file. The
		    # output filenames are generated from the input filename
		    # by appending page number and image number.
		    #
		    imageoptlist = "%s filename={%s_p%02d_%02d}" % \
			(baseimageoptlist, outfilebase, pageno, imageno)
		    
		    if (TET_write_image_file(tet, doc, image["imageid"], \
						imageoptlist) == -1):
			print("\nError " + repr(TET_get_errnum(tet)) + "in " + \
			    TET_get_apiname(tet) + "() on page " + repr(pageno) \
			    + ": " + TET_get_errmsg(tet) + "\n")
			continue                   # process next image */
		image = TET_get_image_info(tet, page)

	    if (TET_get_errnum(tet) != 0):
		raise Exception ("\nError " + repr(TET_get_errnum(tet)) \
		    + "in " + TET_get_apiname(tet) + "() on page " + \
		    repr(pageno) + ": " + TET_get_errmsg(tet) + "\n")

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

finally:
    TET_delete(tet)
