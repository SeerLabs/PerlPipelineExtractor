#!/usr/bin/perl
# $Id: extractor.pl,v 1.12 2009/01/21 19:40:28 rjs Exp $
#
# Simple PDF text extractor based on PDFlib TET
#

use tetlib_pl 3.0;
use strict;


# global option list */
my $globaloptlist = "searchpath={../data ../../../resource/cmap}";

# document-specific option list */
my $docoptlist = "";

# page-specific option list */
my $pageoptlist = "granularity=page";

# separator to emit after each chunk of text. This depends on the
# application's needs; for granularity=word a space character may be useful.
#/
my $separator = "\n";

# basic image extract options (more below) */
my $baseimageoptlist = "compression=auto format=auto";

# set this to 1 to generate image data in memory */
my $inmemory = 0;
my $pageno = 0;

eval  {
    my $tet;
    my $suffix = ".txt";
    my $outfilename;
    my $outfilebase;

    if ($#ARGV < 0 || $#ARGV > 1) {
	die("usage: extractor.pl <infilename> [<outfilename>]\n");
    }


    $tet = TET_new();

    if ($#ARGV == 0) {
	$outfilebase = $ARGV[0];
    } else {
	$outfilebase = $ARGV[1];
    }
    $outfilename = $outfilebase . $suffix;
    open(OUTFP, "> $outfilename") ||
	die("Couldn't open output file '" . $outfilename . "'\n");

    my $n_pages;
    my $doc;

    TET_set_option($tet, $globaloptlist);

    $doc = TET_open_document($tet, $ARGV[0], $docoptlist);

    if ($doc == -1) {
	die("Error ". TET_get_errnum($tet) . " in " . TET_get_apiname($tet)
	    . "(): " . TET_get_errmsg($tet) . "\n");
    }

    # get number of pages in the document */
    $n_pages = TET_pcos_get_number($tet, $doc, "length:pages");

    # loop over pages in the document */
    for ($pageno = 1; $pageno <= $n_pages; ++$pageno)
    {
	my $text;
	my $page;
	my $len;
	my $image;
	my $imageno = -1;

	$page = TET_open_page($tet, $doc, $pageno, $pageoptlist);

	if ($page == -1) {
	    print("Error ". TET_get_errnum($tet) ." in ". TET_get_apiname($tet)
		. "(): " . TET_get_errmsg($tet) . "\n");
	    next;                        # try next page */
	}

	# Retrieve all text fragments; This is actually not required
	# for granularity=page, but must be used for other granularities.
	#/
	while ($text = TET_get_text($tet, $page)) {

	    print OUTFP $text;  # print the retrieved text */

	    # print a separator between chunks of text */
	    print OUTFP $separator;
	}

	# Retrieve all images on the page */
	while ($image = TET_get_image_info($tet, $page)) {
	    my $imageoptlist;
	    my ($width, $height, $bpc, $cs);

	    $imageno++;

	    # Print the following information for each image:
	    # - page and image number
	    # - pCOS id (required for indexing the images[] array)
	    # - physical size of the placed image on the page
	    # - pixel size of the underlying PDF image
	    # - number of components, bits per component, and colorspace
	    #/
	    $width = TET_pcos_get_number($tet, $doc,
			    "images[" . $image->{"imageid"} . "]/Width");
	    $height = TET_pcos_get_number($tet, $doc,
			    "images[" . $image->{"imageid"} . "]/Height");
	    $bpc = TET_pcos_get_number($tet, $doc,
			    "images[" . $image->{"imageid"} . "]/bpc");
	    $cs = TET_pcos_get_number($tet, $doc,
			    "images[" . $image->{"imageid"} . "]/colorspaceid");

	    printf("page %d, image %d: id=%d, %.2fx%.2f point, ",
		    $pageno, $imageno,
		    $image->{"imageid"}, $image->{"width"}, $image->{"height"});

	    printf("%dx%d pixel, ",
		    $width, $height);

	    if ($cs != -1) {
		printf("%dx%d bit %s\n",
		    TET_pcos_get_number($tet, $doc,
			    "colorspaces[$cs]/components"),
		    $bpc,
		    TET_pcos_get_string($tet, $doc, "colorspaces[$cs]/name"));
	    } else {
		# cs==-1 may happen for some JPEG 2000 images. bpc,
		# colorspace name and number of components are not
		# available in this case.
		#
		printf("JPEG2000\n");
	    }

	    if ($inmemory) {
		my $imagedata;

		# Fetch the image data and store it in memory */
		$imageoptlist = sprintf("%s", $baseimageoptlist);

		$imagedata = TET_get_image_data($tet, $doc,
				$image->{"imageid"}, $imageoptlist);

		if (!$imagedata) {
			print("Error ". TET_get_errnum($tet) . " in " . 
				TET_get_apiname($tet) . "(): on page $pageno" 
				. TET_get_errmsg($tet) . "\n");
			next;                 # process next image */
		}

		# Client-specific image data consumption would go here
		# We simply report the size of the data.
		#/
		printf("Page %d: %ld bytes of image data\n",
		    $pageno, length($imagedata));
	    } else {
		#
		# Fetch the image data and write it to a disk file. The
		# output filenames are generated from the input filename
		# by appending page number and image number.
		#/
		$imageoptlist = sprintf("%s filename={%s_p%02d_%02d}",
		    $baseimageoptlist, $outfilebase, $pageno, $imageno);
		if (TET_write_image_file($tet, $doc, $image->{"imageid"},
					    $imageoptlist) == -1) {
		    print("Error ". TET_get_errnum($tet) . " in " . 
			    TET_get_apiname($tet) . "(): on page $pageno" 
			    . TET_get_errmsg($tet) . "\n");
		    next;                  # process next image */
		}
	    }
	}

	if (TET_get_errnum($tet) != 0) {
	    print("Error ". TET_get_errnum($tet) . " in " . 
		    TET_get_apiname($tet) . "(): on page $pageno" 
		    . TET_get_errmsg($tet) . "\n");
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
