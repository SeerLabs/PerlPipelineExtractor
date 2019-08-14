/* PDF text extractor based on PDFlib TET
*
 * $Id: extractor.cpp,v 1.27 2009/01/21 19:40:26 rjs Exp $
 */

#include <iostream>
#include <fstream>

#include "tet.hpp"

/*  global option list */
const string globaloptlist = "searchpath={../data "
		 "../../../resource/cmap}";

/* document-specific  option list */
const string docoptlist = "";

/* page-specific option list */
const string pageoptlist = "granularity=page";

/* separator to emit after each chunk of text. This depends on the
 * applications needs; for granularity=word a space character may be useful.
 */
static const char *utf8_separator = "\n";

/* Basic image extract options (more below) */
const string baseimageoptlist = "compression=auto format=auto";

/* Set inmemory to true to generate the image in memory. */
static bool inmemory = false;


int main(int argc, char **argv)
{
    TET tet;
    ofstream out;
    int pageno = 0;
    const string suffix = ".txt";
    string outfilebase;
    string outfilename;
    
    try
    {
	/* get separator in native byte ordering */
	string utf16_separator(tet.utf8_to_utf16(utf8_separator, ""));

	if (argc < 2 || argc > 3)
	{
	    cerr << "usage: extractor <infilename> [<outfilename>]" << suffix;
	    return(2);
	}

	outfilebase = argc == 2 ? argv[1] : argv[2];
	outfilename = outfilebase + ".txt";

	out.open(outfilename.c_str(), ios::binary);
	if (!out.is_open())
	{
	    cerr << "Couldn't open output file " << outfilebase << suffix
		    << endl;
	    return(2);
	}

        tet.set_option(globaloptlist);

        int doc = tet.open_document(argv[1], docoptlist);

        if (doc == -1)
        {
            cerr << "Error " << tet.get_errnum()
                << " in " << tet.get_apiname()
                << "(): " << tet.get_errmsg() << endl;
            return(2);
        }

	/* get number of pages in the document */
        int n_pages = (int) tet.pcos_get_number(doc, "length:pages");

	/* loop over pages in the document */
        for (pageno = 1; pageno <= n_pages; ++pageno)
        {
            string text;
	    char path[256];
	    char path2[256];
            int page = tet.open_page(doc, pageno, pageoptlist);

            if (page == -1)
            {
                cerr << "Error " << tet.get_errnum()
                    << " in " << tet.get_apiname()
                    << "(): " << tet.get_errmsg() << endl;
                continue;                        /* try next page */
            }

            /* Retrieve all text fragments; This is actually not required
             * for granularity=page, but must be used for other granularities.
             */
            while ((text = tet.get_text(page)) != "")
            {
		/* print the retrieved text */
                out.write(text.c_str(), text.size());
                
                /* print a separator between chunks of text */
                out.write(utf16_separator.c_str(), utf16_separator.size());
            }

	    /* Retrieve all images on the page */
	    int imageno = -1;
	    const TET_image_info *image;
	    while ((image = tet.get_image_info(page)) != NULL)
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
		sprintf(path, "images[%d]/Width", image->imageid);
		width = (int) tet.pcos_get_number(doc, path);
		sprintf(path, "images[%d]/Height", image->imageid);
		height = (int) tet.pcos_get_number(doc, path);
		sprintf(path, "images[%d]/bpc", image->imageid);
		bpc = (int) tet.pcos_get_number(doc, path);
		sprintf(path, "images[%d]/colorspaceid", image->imageid);
		cs = (int) tet.pcos_get_number(doc, path);

		printf("page %d, image %d: id=%d, %.2fx%.2f point, ",
			pageno, imageno,
			image->imageid, image->width, image->height);
		printf("%dx%d pixel, ", width, height);

		if (cs != -1)
		{
		    sprintf(path, "colorspaces[%d]/components", cs);
		    sprintf(path2, "colorspaces[%d]/name", cs); 
		    printf("%dx%d bit %s\n",
			(int) tet.pcos_get_number(doc, path),
			bpc, tet.pcos_get_string(doc, path2).c_str()); 
		}
		else {
		    /* cs==-1 may happen for some JPEG 2000 images. bpc,
		     * colorspace name and number of components are not
		     * available in this case.
		     */
		    printf("JPEG2000\n");
		}

		if (inmemory)
		{
		    size_t len;

		    /* Fetch the image data and store it in memory */
		    const char *imagedata = tet.get_image_data(doc, &len,
			    image->imageid, baseimageoptlist);

		    if (imagedata == NULL)
		    {
			cerr << "Error " << tet.get_errnum()
			    << " in " << tet.get_apiname()
			    << "() on page " << pageno
			    << ": " << tet.get_errmsg() << endl;
			continue; /* process next image */
		    }

		    /*
		     * Client-specific image data consumption would go here
		     * We simply report the size of the data.
		     */
		    cerr << "Page " << pageno << ": "
			    << len << " bytes of image data" << endl;
		}
		else
		{
		   /*
		    * Fetch the image data and write it to a disk file. The
		    * output filenames are generated from the input filename
		    * by appending page number and image number.
		    */
		    sprintf(imageoptlist, "%s filename={%s_p%02d_%02d}",
			baseimageoptlist.c_str(), outfilebase.c_str(),
			pageno, imageno);

		    if (tet.write_image_file(doc, image->imageid,
				    imageoptlist) == -1)
		    {
			cerr << "Error " << tet.get_errnum()
			    << " in " << tet.get_apiname()
			    << "() on page " << pageno
			    << ": " << tet.get_errmsg() << endl;
			continue; /* process next image */
		    }
		}
	    }

            if (tet.get_errnum() != 0)
            {
                cerr << "Error " << tet.get_errnum()
                    << " in " << tet.get_apiname()
                    << "() on page " << pageno
                    << ": " << tet.get_errmsg() << endl;
            }

            tet.close_page(page);
        }

        tet.close_document(doc);
    }

    catch (TET::Exception &ex) {
        if (pageno ==0) {
            cerr << "Error " << ex.get_errnum()
                << " in " << ex.get_apiname()
                << "(): " << ex.get_errmsg() << endl;
        } else { cerr << "Error " << ex.get_errnum()
                << " in " << ex.get_apiname()
                << "() on page " << pageno
                << ": " << ex.get_errmsg() << endl;
        }
	return(2);
    }

    out.close();
    return(0);
}
