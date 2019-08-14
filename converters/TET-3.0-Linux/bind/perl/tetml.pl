#!/usr/bin/perl
# TET sample application for dumping PDF information in the XML language TETML
#
# $Id: tetml.pl,v 1.4 2008/12/23 17:31:11 rjs Exp $
#/

use tetlib_pl 3.0;
use strict;

# global option list */
my $globaloptlist = "searchpath={../data ../../../resource/cmap}";

# document-specific option list */
my $basedocoptlist = "";

# page-specific option list */
my $pageoptlist = "granularity=word";

# set this to 1 to generate TETML output in memory */
my $inmemory = 0;

eval  {
    my $tet;
    my $tetml;
    my $pageno;
    my $n_pages;
    my $docoptlist;
    my $doc;
    my $len;

    if ($#ARGV !=1) {
	die("usage: tetml <pdffilename> <xmlfilename>\n");
    }

    $tet = TET_new();

    TET_set_option($tet, $globaloptlist);

    if ($inmemory) {
	$docoptlist = sprintf("tetml={} %s", $basedocoptlist);
    } else {
	$docoptlist = sprintf("tetml={filename={%s}} %s",
	    $ARGV[1], $basedocoptlist);
    }

    $doc = TET_open_document($tet, $ARGV[0], $docoptlist);

    if ($doc == -1) {
	die(sprintf("Error %d in %s(): %s\n",
	    TET_get_errnum($tet), TET_get_apiname($tet), TET_get_errmsg($tet)));
    }

    $n_pages = TET_pcos_get_number($tet, $doc, "length:pages");

    # loop over pages in the document */
    for ($pageno = 1; $pageno <= $n_pages; ++$pageno) {
	TET_process_page($tet, $doc, $pageno, $pageoptlist);
    }

    # This could be combined with the last page-related call */
    TET_process_page($tet, $doc, 0, "tetml={trailer}");

    if ($inmemory) {
	my $fname = $ARGV[1];
	open(FP, ">$fname") || die(
	    sprintf("tetml: couldn't open output file '%s'\n", $fname));

	# Retrieve the generated TETML data from memory. Since we have
	# only a single call the result will contain the full TETML.
	#/

	$tetml = TET_get_xml_data($tet, $doc, "");
	if (!$tetml) {
	    die("tetml: couldn't retrieve XML data\n");
	}

	print FP $tetml;
	close(FP);
    }

    TET_close_document($tet, $doc);
};

if ($@) {
    printf("Error $@\n");
    exit;
}
