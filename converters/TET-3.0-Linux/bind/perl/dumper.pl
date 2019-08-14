# TET sample application for dumping PDF information with pCOS
#
# $Id: dumper.pl,v 1.3 2008/12/15 20:26:53 rjs Exp $
#/

use tetlib_pl 3.0;
use strict;

eval  {
    my $tet;

    if ($#ARGV != 0) {
	die("usage: dumper <filename>\n");
    }

    $tet = TET_new();

    my ($count, $pcosmode, $plainmetadata);
    my ($objtype, $i, $doc);
    my $docoptlist = "requiredmode=minimum";
    my $globaloptlist = "";
    # This is where input files live. Adjust as necessary.
    my $searchpath = "../data";
    my $optlist;

    TET_set_option($tet, $globaloptlist);

    $optlist = sprintf("searchpath={%s}", $searchpath);
    TET_set_option($tet, $optlist);


    if (($doc = TET_open_document($tet, $ARGV[0], $docoptlist)) == -1) {
	die(sprintf("ERROR: %s\n", TET_get_errmsg($tet)));
    }

    # --------- general information (always available) */

    $pcosmode = TET_pcos_get_number($tet, $doc, "pcosmode");

    printf("   File name: %s\n", TET_pcos_get_string($tet, $doc, "filename"));

    printf(" PDF version: %s\n",
	TET_pcos_get_string($tet, $doc, "pdfversionstring"));

    printf("  Encryption: %s\n",
	TET_pcos_get_string($tet, $doc, "encrypt/description"));

    printf("   Master pw: %s\n",
	TET_pcos_get_number($tet, $doc, "encrypt/master") ? "yes" : "no");

    printf("     User pw: %s\n",
	TET_pcos_get_number($tet, $doc, "encrypt/user") ? "yes" : "no");

    printf("Text copying: %s\n",
	TET_pcos_get_number($tet, $doc, "encrypt/nocopy") ? "no" : "yes");

    printf("  Linearized: %s\n",
	TET_pcos_get_number($tet, $doc, "linearized") ? "yes" : "no");

    if ($pcosmode == 0) {
	printf("Minimum mode: no more information available\n\n");
	TET_delete($tet);
	return 0;
    }

    # --------- more details (requires at least user password) */

    printf("PDF/X status: %s\n", TET_pcos_get_string($tet, $doc, "pdfx"));

    printf("PDF/A status: %s\n", TET_pcos_get_string($tet, $doc, "pdfa"));

    printf("  Tagged PDF: %s\n\n",
	TET_pcos_get_number($tet, $doc, "tagged") ? "yes" : "no");

    printf("No. of pages: %d\n",
	TET_pcos_get_number($tet, $doc, "length:pages"));

    printf(" Page 1 size: width=%g, height=%g\n",
	TET_pcos_get_number($tet, $doc, "pages[0]/width"),
	TET_pcos_get_number($tet, $doc, "pages[0]/height"));

    $count = TET_pcos_get_number($tet, $doc, "length:fonts");
    printf("No. of fonts: %d\n", $count);

    for ($i=0; $i < $count; $i++) {
	if (TET_pcos_get_number($tet, $doc, "fonts[$i]/embedded")) {
	    printf("embedded ");
	} else {
	    printf("unembedded ");
	}

	printf("%s font ",
	    TET_pcos_get_string($tet, $doc, "fonts[$i]/type"));
	printf("%s\n",
	    TET_pcos_get_string($tet, $doc, "fonts[$i]/name"));
    }

    printf("\n");

    $plainmetadata =
	TET_pcos_get_number($tet, $doc, "encrypt/plainmetadata");

    if ($pcosmode == 1 && !$plainmetadata &&
	    (TET_pcos_get_number($tet, $doc, "encrypt/nocopy"))) {
	printf("Restricted mode: no more information available\n\n");
	TET_delete($tet);
	return 0;
    }

    # --------- document info keys and XMP metadata (requires master pw
    # or plaintext metadata)
    #/

    $count = TET_pcos_get_number($tet, $doc, "length:/Info");

    for ($i=0; $i < $count; $i++) {
	$objtype = TET_pcos_get_string($tet, $doc, "type:/Info[$i]");
	printf("%12s: ", TET_pcos_get_string($tet, $doc, "/Info[$i].key"));

	# Info entries can be stored as string or name objects */
	if ($objtype eq "string" || $objtype eq "name") {
	    printf("'%10s'\n",
		TET_pcos_get_string($tet, $doc, "/Info[$i]"));
	} else {
	    printf("(%s object)\n",
		TET_pcos_get_string($tet, $doc, "type:/Info[$i]"));
	}
    }

    printf("\nXMP meta data: ");

    $objtype = TET_pcos_get_string($tet, $doc, "type:/Root/Metadata");
    if ($objtype eq "stream") {
	my $contents;

	$contents =
	    TET_pcos_get_stream($tet, $doc, "", "/Root/Metadata");
	printf("%d bytes ", length($contents));

	# This demonstrates Unicode conversion */
	my $dummy = TET_utf8_to_utf16($tet, $contents, "utf16");
	printf("(%d Unicode characters)\n\n", length($dummy)/2);
    } else {
	printf("not present\n\n");
    }

    TET_close_document($tet, $doc);
};

if ($@) {
    printf("TET exception occurred in dumper:\n");
    printf("$@\n");
    exit;
}
