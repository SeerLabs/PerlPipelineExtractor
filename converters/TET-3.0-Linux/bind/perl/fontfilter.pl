#!/usr/bin/perl
# Extract text from PDF and filter according to font name and size.
# This can be used to identify headings in the document and create a
# table of contents.
#
# $Id: fontfilter.pl,v 1.3 2008/12/23 17:31:11 rjs Exp $
#/

use tetlib_pl 3.0;
use strict;

# global option list */
my $globaloptlist = "searchpath={../data ../../../resource/cmap}";

# document-specific option list */
my $docoptlist = "";

# page-specific option list */
my $pageoptlist = "granularity=line";

# Search text with at least this size (use 0 to catch all sizes) */
my $fontsizetrigger = 10;

# Catch text where the font name contains this string
# (use empty string to catch all font names)
#/
my $fontnametrigger = "Bold";
my $pageno = 0;

eval  {
    my $tet;
    my $n_pages;
    my $doc;

    if ($#ARGV != 0) {
	die("usage: fontfilter <infilename>\n");
    }

    $tet = TET_new();

    TET_set_option($tet, $globaloptlist);

    $doc = TET_open_document($tet, $ARGV[0], $docoptlist);

    if ($doc == -1) {
	die(sprintf("Error %d in %s(): %s\n",
	    TET_get_errnum($tet), TET_get_apiname($tet), TET_get_errmsg($tet)));
    }

    # get number of pages in the document */
    $n_pages = TET_pcos_get_number($tet, $doc, "length:pages");

    # loop over pages in the document */
    for ($pageno = 1; $pageno <= $n_pages; ++$pageno) {
	my $text;
	my $page;

	$page = TET_open_page($tet, $doc, $pageno, $pageoptlist);

	if ($page == -1) {
	    printf("Error %d in %s() on page %d: %s\n",
		TET_get_errnum($tet), TET_get_apiname($tet), $pageno,
		TET_get_errmsg($tet));
	    next;                        # try next page */
	}

	# Retrieve all text fragments for the page */
	while ($text = TET_get_text($tet, $page)) {
	    my $ci;
	    my $fontname;

	    # Loop over all characters */
	    while ($ci = TET_get_char_info($tet, $page)) {
		# We need only the font name and size; the text 
		# position could be fetched from ci->x and ci->y.
		#/
		$fontname = TET_pcos_get_string($tet, $doc,
			    "fonts[" . $ci->{"fontid"} . "]/name");

		# Check whether we found a match */
		if ($ci->{"fontsize"} >= $fontsizetrigger &&
			($fontname =~ m/$fontnametrigger/)) {
		    # print the retrieved font name, size, and text */
		    printf("[%s %.2f] %s\n",
			$fontname, $ci->{"fontsize"}, $text);
		}

		# In this sample we check only the first character of
		# each fragment.
		#/
		last;
	    }
	}

	if (TET_get_errnum($tet) != 0) {
	    printf("Error %d in %s() on page %d: %s\n",
		TET_get_errnum($tet), TET_get_apiname($tet), $pageno,
		TET_get_errmsg($tet));
	}

	TET_close_page($tet, $page);
    }

    TET_close_document($tet, $doc);
};

if ($@) {
    printf("PDFlib Exception occurred:\n");
    if ($pageno == 0) {
	printf("Error $@\n");
    } else {
	printf("Error $@ on page $pageno\n");
    }
    exit;
}
