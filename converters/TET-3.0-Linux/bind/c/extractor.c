/* Simple PDF text and image extractor based on PDFlib TET
 *
 * $Id: extractor.c,v 1.51 2009/01/21 19:40:25 rjs Exp $
 */

#include <stdio.h>
#include <string.h>
#include <stdlib.h>

#include "tetlib.h"

/* global option list */
static const char *globaloptlist =
    "searchpath={../data "
        "../../../resource/cmap}";

/* document-specific option list */
static const char *docoptlist = "";

/* page-specific option list */
static const char *pageoptlist = "granularity=page";

/* separator to emit after each chunk of text. This depends on the
 * application's needs; for granularity=word a space character may be useful.
 */
static const char *separator = "\n";

/* basic image extract options (more below) */
static const char *baseimageoptlist = "compression=auto format=auto";

/* set this to 1 to generate image data in memory */
static int inmemory = 0;

int main(int argc, char **argv)
{
    TET *tet;
    FILE *outfp;
    volatile int pageno = 0;
    static const char suffix[] = ".txt";
    char *outfilename;
    char *outfilebase;

    if (argc < 2 || argc > 3)
    {
        fprintf(stderr, "usage: extractor <infilename> [<outfilename>]\n");
        return(2);
    }

    if ((tet = TET_new()) == (TET *) 0)
    {
        fprintf(stderr, "extractor: out of memory\n");
        return(2);
    }

    if (argc == 2) {
	outfilebase = argv[1];
    } else {
	outfilebase = argv[2];
    }

    outfilename = malloc(strlen(outfilebase) + sizeof(suffix) + 1);
    if (!outfilename)
    {
	fprintf(stderr, "Out of memory");
	return(2);
    }
    sprintf(outfilename, "%s%s", outfilebase, suffix);

    if ((outfp = fopen(outfilename, "w")) == NULL)
    {
	fprintf(stderr, "Couldn't open output file '%s'\n", outfilename);
	TET_delete(tet);
	free(outfilename);
	return(2);
    }
    free(outfilename);

    TET_TRY (tet)
    {
        int n_pages;
        int doc;

        TET_set_option(tet, globaloptlist);

        doc = TET_open_document(tet, argv[1], 0, docoptlist);

        if (doc == -1)
        {
            fprintf(stderr, "Error %d in %s(): %s\n",
                TET_get_errnum(tet), TET_get_apiname(tet), TET_get_errmsg(tet));
            TET_EXIT_TRY(tet);
            TET_delete(tet);
            return(2);
        }

        /* get number of pages in the document */
        n_pages = (int) TET_pcos_get_number(tet, doc, "length:pages");

	/* loop over pages in the document */
        for (pageno = 1; pageno <= n_pages; ++pageno)
        {
            const char *text;
            int page;
            int len;
	    const TET_image_info *image;
	    int imageno = -1;

            page = TET_open_page(tet, doc, pageno, pageoptlist);

            if (page == -1)
            {
                fprintf(stderr, "Error %d in %s() on page %d: %s\n",
                    TET_get_errnum(tet), TET_get_apiname(tet), pageno,
                    TET_get_errmsg(tet));
                continue;                        /* try next page */
            }

            /* Retrieve all text fragments; This is actually not required
	     * for granularity=page, but must be used for other granularities.
	     */
            while ((text = TET_get_text(tet, page, &len)) != 0)
            {

		fprintf(outfp, "%s", text);  /* print the retrieved text */

		/* print a separator between chunks of text */
		fprintf(outfp, separator);
            }

            /* Retrieve all images on the page */
            while ((image = TET_get_image_info(tet, page)) != 0)
            {
		char imageoptlist[1024];
		int width, height, bpc, cs;

		imageno++;

		/* Print the following information for each image:
		 * - page and image number
		 * - pCOS id (required for indexing the images[] array)
		 * - physical size of the placed image on the page
		 * - pixel size of the underlying PDF image
		 * - number of components, bits per component, and colorspace
		 */
		width = (int) TET_pcos_get_number(tet, doc,
				    "images[%d]/Width", image->imageid);
		height = (int) TET_pcos_get_number(tet, doc,
				    "images[%d]/Height", image->imageid);
		bpc = (int) TET_pcos_get_number(tet, doc,
				    "images[%d]/bpc", image->imageid);
		cs = (int) TET_pcos_get_number(tet, doc,
				    "images[%d]/colorspaceid", image->imageid);

		printf("page %d, image %d: id=%d, %.2fx%.2f point, ",
			pageno, imageno, image->imageid,
			image->width, image->height);
		printf("%dx%d pixel, ", width, height);

		if (cs != -1)
		{
		    printf("%dx%d bit %s\n",
			    (int) TET_pcos_get_number(tet, doc,
				    "colorspaces[%d]/components", cs),
			    bpc,
			    TET_pcos_get_string(tet, doc,
				    "colorspaces[%d]/name", cs));
		}
		else
		{
		    /* cs==-1 may happen for some JPEG 2000 images. bpc,
		     * colorspace name and number of components are not
		     * available in this case.
		     */
		    printf("JPEG2000\n");
		}

		if (inmemory)
		{
		    const char *imagedata;
		    size_t length;

		    /* Fetch the image data and store it in memory */
		    sprintf(imageoptlist, "%s", baseimageoptlist);

		    imagedata = TET_get_image_data(tet, doc, &length,
				    image->imageid, imageoptlist);

		    if (imagedata == NULL)
		    {
			    printf("\nError %d in %s() on page %d: %s\n",
			    TET_get_errnum(tet), TET_get_apiname(tet), pageno,
			    TET_get_errmsg(tet));
			    continue;                 /* process next image */
		    }

		    /* Client-specific image data consumption would go here
		     * We simply report the size of the data.
		     */
		    printf("Page %d: %ld bytes of image data\n",
		    	pageno, length);

		} else {

		    /*
                     * Fetch the image data and write it to a disk file. The
                     * output filenames are generated from the input filename
                     * by appending page number and image number.
                     */
		    sprintf(imageoptlist, "%s filename={%s_p%02d_%02d}",
			baseimageoptlist, outfilebase, pageno, imageno);

		    if (TET_write_image_file(tet, doc, image->imageid,
						imageoptlist) == -1)
		    {
			printf("\nError %d in %s() on page %d: %s\n",
			    TET_get_errnum(tet), TET_get_apiname(tet), pageno,
			    TET_get_errmsg(tet));
			continue;                  /* process next image */
		    }
		}
            }

            if (TET_get_errnum(tet) != 0)
            {
                fprintf(stderr, "Error %d in %s() on page %d: %s\n",
                    TET_get_errnum(tet), TET_get_apiname(tet), pageno,
                    TET_get_errmsg(tet));
            }

            TET_close_page(tet, page);
        }

        TET_close_document(tet, doc);
    }

    TET_CATCH (tet)
    {
        if (pageno == 0)
            fprintf(stderr, "Error %d in %s(): %s\n",
                TET_get_errnum(tet), TET_get_apiname(tet), TET_get_errmsg(tet));
        else
            fprintf(stderr, "Error %d in %s() on page %d: %s\n",
                TET_get_errnum(tet), TET_get_apiname(tet), pageno,
                TET_get_errmsg(tet));
    }

    TET_delete(tet);
    fclose(outfp);

    return 0;
}
