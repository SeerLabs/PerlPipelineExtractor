<?php
/*
 * TET sample application for dumping PDF information in the XML language TETML
 *
 * $Id: tetml.php,v 1.5 2008/12/30 13:17:52 rp Exp $
 */

/* global option list */
$globaloptlist = "searchpath={../data ../../data ../../../resource/cmap}";

/* document-specific option list */
$basedocoptlist = "";

/* page-specific option list */
$pageoptlist = "granularity=word";

/* set this to 1 to generate TETML output in memory */
$inmemory = 0;


try {
    if ($argc !=3) {
	die("usage: tetml <pdffilename> <xmlfilename>\n");
    }

    $tet = new TET();

    $tet->set_option($globaloptlist);

    if ($inmemory) {
	$docoptlist = sprintf("tetml={} %s", $basedocoptlist);
    } else {
	$docoptlist = sprintf("tetml={filename={%s}} %s",
	    $argv[2], $basedocoptlist);
    }

    $doc = $tet->open_document($argv[1], $docoptlist);

    if ($doc == -1) {
	die(sprintf("Error %d in %s(): %s\n",
	    $tet->get_errnum(), $tet->get_apiname(), $tet->get_errmsg()));
    }

    $n_pages = $tet->pcos_get_number($doc, "length:pages");

    /* loop over pages in the document */
    for ($pageno = 1; $pageno <= $n_pages; ++$pageno) {
	$tet->process_page($doc, $pageno, $pageoptlist);
    }

    /* This could be combined with the last page-related call */
    $tet->process_page($doc, 0, "tetml={trailer}");

    if ($inmemory) {
	$fname = $argv[2];
	if (!$fp = fopen($fname, "wb")) {
	    die(sprintf("tetml: couldn't open output file '%s'\n", $fname));
	}

	/* Retrieve the generated TETML data from memory. Since we have
	 * only a single call the result will contain the full TETML.
	 */

	$tetml = $tet->get_xml_data($doc, "");
	if (!$tetml) {
	    die("tetml: couldn't retrieve XML data\n");
	}

	fwrite($fp, $tetml);
	fclose($fp);
    }

    $tet->close_document($doc);
}
catch (TETException $e) {
    die("TET exception occurred in tetml sample:\n" .
	    "[" . $e->get_errnum() . "] " . $e->get_apiname() . ": " .
	    $e->get_errmsg() . "\n");
}
catch (Exception $e) {
    die($e);
}

$tet = 0;
?>
