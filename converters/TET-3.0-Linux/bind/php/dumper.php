<?php
/* TET sample application for dumping PDF information with pCOS
 *
 * $Id: dumper.php,v 1.3 2008/12/15 20:26:54 rjs Exp $
 */

try {
    if ($argc != 2) {
	die("usage: dumper <filename>\n");
    }

    $tet = new TET();

    $docoptlist = "requiredmode=minimum";
    $globaloptlist = "";
    /* This is where input files live. Adjust as necessary. */
    $searchpath = "../data ../../data";

    $tet->set_option($globaloptlist);

    $optlist = sprintf("searchpath={%s}", $searchpath);
    $tet->set_option($optlist);

    if (($doc = $tet->open_document($argv[1], $docoptlist)) == -1) {
	die(sprintf("ERROR: %s\n", $tet->get_errmsg()));
    }

    /* --------- general information (always available) */

    $pcosmode = $tet->pcos_get_number($doc, "pcosmode");

    printf("   File name: %s\n", $tet->pcos_get_string($doc, "filename"));

    printf(" PDF version: %.1f\n",
	$tet->pcos_get_string($doc, "pdfversionstring"));

    printf("  Encryption: %s\n",
	$tet->pcos_get_string($doc, "encrypt/description"));

    printf("   Master pw: %s\n",
	$tet->pcos_get_number($doc, "encrypt/master") ? "yes" : "no");

    printf("     User pw: %s\n",
	$tet->pcos_get_number($doc, "encrypt/user") ? "yes" : "no");

    printf("Text copying: %s\n",
	$tet->pcos_get_number($doc, "encrypt/nocopy") ? "no" : "yes");

    printf("  Linearized: %s\n",
	$tet->pcos_get_number($doc, "linearized") ? "yes" : "no");

    if ($pcosmode == 0) {
	printf("Minimum mode: no more information available\n\n");
	$tet->delete();
	return 0;
    }

    /* --------- more details (requires at least user password) */

    printf("PDF/X status: %s\n", $tet->pcos_get_string($doc, "pdfx"));

    printf("PDF/A status: %s\n", $tet->pcos_get_string($doc, "pdfa"));

    printf("  Tagged PDF: %s\n\n",
	$tet->pcos_get_number($doc, "tagged") ? "yes" : "no");

    printf("No. of pages: %d\n",
	$tet->pcos_get_number($doc, "length:pages"));

    printf(" Page 1 size: width=%d, height=%d\n",
	$tet->pcos_get_number($doc, "pages[0]/width"),
	$tet->pcos_get_number($doc, "pages[0]/height"));

    $count = $tet->pcos_get_number($doc, "length:fonts");
    printf("No. of fonts: %d\n", $count);

    for ($i=0; $i < $count; $i++) {
	if ($tet->pcos_get_number($doc, "fonts[$i]/embedded")) {
	    printf("embedded ");
	} else {
	    printf("unembedded ");
	}

	printf("%s font ",
	    $tet->pcos_get_string($doc, "fonts[$i]/type"));
	printf("%s\n",
	    $tet->pcos_get_string($doc, "fonts[$i]/name"));
    }

    printf("\n");

    $plainmetadata =
	$tet->pcos_get_number($doc, "encrypt/plainmetadata");

    if ($pcosmode == 1 && !$plainmetadata &&
	    ($tet->pcos_get_number($doc, "encrypt/nocopy"))) {
	printf("Restricted mode: no more information available\n\n");
	$tet->delete();
	return 0;
    }

    /* --------- document info keys and XMP metadata (requires master pw
     * or plaintext metadata)
     */

    $count = $tet->pcos_get_number($doc, "length:/Info");

    for ($i=0; $i < $count; $i++) {
	$objtype = $tet->pcos_get_string($doc, "type:/Info[$i]");
	printf("%12s: ", $tet->pcos_get_string($doc, "/Info[$i].key"));

	/* Info entries can be stored as string or name objects */
	if ($objtype == "string" || $objtype == "name") {
	    printf("'%10s'\n",
		$tet->pcos_get_string($doc, "/Info[$i]"));
	} else {
	    printf("(%s object)\n",
		$tet->pcos_get_string($doc, "type:/Info[$i]"));
	}
    }

    printf("\nXMP meta data: ");

    $objtype = $tet->pcos_get_string($doc, "type:/Root/Metadata");
    if ($objtype == "stream") {
	$contents;

	$contents =
	    $tet->pcos_get_stream($doc, "", "/Root/Metadata");
	printf("%d bytes ", strlen($contents));

	/* This demonstrates Unicode conversion */
	$dummy = $tet->utf8_to_utf16($contents, "utf16");
	printf("(%d Unicode characters)\n\n", strlen($dummy)/2);
    } else {
	printf("not present\n\n");
    }

    $tet->close_document($doc);
}
catch (TETException $e) {
    die("TET exception occurred in extractor sample:\n" .
	    "[" . $e->get_errnum() . "] " . $e->get_apiname() . ": " .
	    $e->get_errmsg() . "\n");
}
catch (Exception $e) {
    die($e);
}

$tet = 0;
?>
