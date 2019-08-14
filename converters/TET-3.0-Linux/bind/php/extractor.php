<?php
/* $Id: extractor.php,v 1.11 2009/01/21 19:40:28 rjs Exp $
 *
 * Simple PDF text extractor based on PDFlib TET
 */


/* global option list */
$globaloptlist = "searchpath={../data ../../data ../../../resource/cmap}";

/* document-specific option list */
$docoptlist = "";

/* page-specific option list */
$pageoptlist = "granularity=page";

/* separator to emit after each chunk of text. This depends on the
 * application's needs; for granularity=word a space character may be useful
 */
$separator = "\n";

/* basic image extract options (more below) */
$baseimageoptlist = "compression=auto format=auto";

/* set this to 1 to generate image data in memory */
$inmemory = 0;

$pageno = 0;
$suffix = ".txt";

try {
    if ($argc < 2 || $argc > 3) {
	die("usage: extractor.pl <infilename> [<outfilename>]\n");
    }


    $tet = new TET();

    if ($argc == 2) {
	$outfilebase = $argv[1];
    } else {
	$outfilebase = $argv[2];
    }
    $outfilename = $outfilebase . $suffix;

    if (!$outfp = fopen($outfilename, "wb")) {
	die("Couldn't open output file '" . $outfilename . "'\n");
    }


    $tet->set_option($globaloptlist);

    $doc = $tet->open_document($argv[1], $docoptlist);

    if ($doc == -1) {
	die("Error ". $tet->get_errnum() . " in " . $tet->get_apiname()
	    . "(): " . $tet->get_errmsg() . "\n");
    }

    /* get number of pages in the document */
    $n_pages = $tet->pcos_get_number($doc, "length:pages");

    /* loop over pages in the document */
    for ($pageno = 1; $pageno <= $n_pages; ++$pageno) {
	$imageno = -1;

	$page = $tet->open_page($doc, $pageno, $pageoptlist);

	if ($page == -1) {
	    print("Error ". $tet->get_errnum() ." in ". $tet->get_apiname()
		. "(): " . $tet->get_errmsg() . "\n");
	    next;                        /* try next page */
	}

	/* Retrieve all text fragments; This is actually not required
	 * for granularity=page, but must be used for other granularities.
	 */
	while ($text = $tet->get_text($page)) {

	    fwrite($outfp, $text);  /* print the retrieved text */

	    /* print a separator between chunks of text */
	    fwrite($outfp, $separator);
	}

	/* Retrieve all images on the page */
	while ($image = $tet->get_image_info($page)) {

	    $imageno++;

	    /* Print the following information for each image:
	     * - page and image number
	     * - pCOS id (required for indexing the images[] array)
	     * - physical size of the placed image on the page
	     * - pixel size of the underlying PDF image
	     * - number of components, bits per component, and colorspace
	     */
	    $width = $tet->pcos_get_number($doc,
			    "images[" . $image->imageid . "]/Width");
	    $height = $tet->pcos_get_number($doc,
			    "images[" . $image->imageid . "]/Height");
	    $bpc = $tet->pcos_get_number($doc,
			    "images[" . $image->imageid . "]/bpc");
	    $cs = $tet->pcos_get_number($doc,
			    "images[" . $image->imageid . "]/colorspaceid");

	    printf("page %d, image %d: id=%d, %.2fx%.2f point, ",
		    $pageno, $imageno,
		    $image->imageid, $image->width, $image->height);

	    printf("%dx%d pixel, ",
		    $width, $height);

	    if ($cs != -1) {
		printf("%dx%d bit %s\n",
		    $tet->pcos_get_number($doc,
			    "colorspaces[$cs]/components"),
		    $bpc,
		    $tet->pcos_get_string($doc, "colorspaces[$cs]/name"));
	    } else {
		/* cs==-1 may happen for some JPEG 2000 images. bpc,
		 * colorspace name and number of components are not
		 * available in this case.
		 */
		printf("JPEG2000\n");
	    }

	    if ($inmemory) {

		/* Fetch the image data and store it in memory */
		$imageoptlist = sprintf("%s", $baseimageoptlist);

		$imagedata = $tet->get_image_data($doc,
				$image->imageid, $imageoptlist);

		if (!$imagedata) {
			print("Error ". $tet->get_errnum() . " in " . 
				$tet->get_apiname() . "(): on page $pageno " 
				. $tet->get_errmsg() . "\n");
			next;                 /* process next image */
		}

		/* Client-specific image data consumption would go here
		 * We simply report the size of the data.
		 */
		printf("Page %d: %ld bytes of image data\n",
		    $pageno, $length);
	    } else {
		/*
		 * Fetch the image data and write it to a disk file. The
		 * output filenames are generated from the input filename
		 * by appending page number and image number.
		 */
		$imageoptlist = sprintf("%s filename={%s_p%02d_%02d}",
		    $baseimageoptlist, $outfilebase, $pageno, $imageno);

		if ($tet->write_image_file($doc, $image->imageid,
					    $imageoptlist) == -1) {
		    print("Error ". $tet->get_errnum() . " in " . 
			    $tet->get_apiname() . "() on page $pageno: " 
			    . $tet->get_errmsg() . "\n");
		    next;                  /* process next image */
		}
	    }
	}


	if ($tet->get_errnum() != 0) {
	    print("Error ". $tet->get_errnum() . " in " . 
		    $tet->get_apiname() . "(): on page $pageno" 
		    . $tet->get_errmsg() . "\n");
	}

	$tet->close_page($page);
    }

    fclose($outfp);

    $tet->close_document($doc);
}
catch (TETException $e) {
    if ($pageno == 0) {
	die("TET exception occurred in extractor sample:\n" .
	    "[" . $e->get_errnum() . "] " . $e->get_apiname() . ": " .
	    $e->get_errmsg() . "\n");
    } else {
	die("TET exception occurred in extractor sample:\n" .
	    "[" . $e->get_errnum() . "] " . $e->get_apiname() .
	    "on page $pageno: " .  $e->get_errmsg() . "\n");
    }
}
catch (Exception $e) {
    die($e);
}

$tet = 0;
?>
