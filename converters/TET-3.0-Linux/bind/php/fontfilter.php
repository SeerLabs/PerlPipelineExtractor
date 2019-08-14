<?php
/*
* Extract text from PDF and filter according to font name and size.
* This can be used to identify headings in the document and create a
* table of contents.
*
* $Id: fontfilter.php,v 1.4 2008/12/30 13:17:52 rp Exp $
*/


/* global option list */
$globaloptlist = "searchpath={../data ../../data ../../../resource/cmap}";

/* document-specific option list */
$docoptlist = "";

/* page-specific option list */
$pageoptlist = "granularity=line";

/* Search text with at least this size (use 0 to catch all sizes) */
$fontsizetrigger = 10;

/* Catch text where the font name contains this string
 * (use empty string to catch all font names)
 */
$fontnametrigger = "Bold";
$pageno = 0;

try {
    if ($argc != 2) {
	die("usage: fontfilter <infilename>\n");
    }

    $tet = new TET();

    $tet->set_option($globaloptlist);

    $doc = $tet->open_document($argv[1], $docoptlist);

    if ($doc == -1) {
	die(sprintf("Error %d in %s(): %s\n",
	    $tet->get_errnum(), $tet->get_apiname(), $tet->get_errmsg()));
    }

    /* get number of pages in the document */
    $n_pages = $tet->pcos_get_number($doc, "length:pages");

    /* loop over pages in the document */
    for ($pageno = 1; $pageno <= $n_pages; ++$pageno) {
	$page = $tet->open_page($doc, $pageno, $pageoptlist);

	if ($page == -1) {
	    printf("Error %d in %s() on page %d: %s\n",
		$tet->get_errnum(), $tet->get_apiname(), $pageno,
		$tet->get_errmsg());
	    next;                        /* try next page */
	}

	/* Retrieve all text fragments for the page */
	while ($text = $tet->get_text($page)) {
	    /* Loop over all characters */
	    while ($ci = $tet->get_char_info($page)) {
		/* We need only the font name and size; the text 
		 * position could be fetched from ci->x and ci->y.
		 */
		$fontname = $tet->pcos_get_string($doc,
			    "fonts[" . $ci->fontid . "]/name");

		/* Check whether we found a match */
		if ($ci->fontsize >= $fontsizetrigger &&
			(strpos($fontname, $fontnametrigger) !== false)) {
		    /* print the retrieved font name, size, and text */
		    printf("[%s %.2f] %s\n", $fontname, $ci->fontsize, $text);
		}

		/* In this sample we check only the first character of
		 * each fragment.
		 */
		break;
	    }
	}

	if ($tet->get_errnum() != 0) {
	    printf("Error %d in %s() on page %d: %s\n",
		$tet->get_errnum(), $tet->get_apiname(), $pageno,
		$tet->get_errmsg());
	}

	$tet->close_page($page);
    }

    $tet->close_document($doc);
}
catch (TETException $e) {
    if ($pageno == 0) {
	die("TET exception occurred in fontfilter sample:\n" .
	    "[" . $e->get_errnum() . "] " . $e->get_apiname() . ": " .
	    $e->get_errmsg() . "\n");
    } else {
	die("TET exception occurred in fontfilter sample:\n" .
	    "[" . $e->get_errnum() . "] " . $e->get_apiname() .
	    " on page $pageno: " .  $e->get_errmsg() . "\n");
    }
}
catch (Exception $e) {
    die($e);
}

$tet = 0;
?>
