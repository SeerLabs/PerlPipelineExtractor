/**
* Extract text from PDF and filter according to font name and size. This can be
* used to identify headings in the document and create a table of contents.
* 
* $Id: fontfilter.cpp,v 1.3 2008/12/23 17:31:09 rjs Exp $
*/

#include <iostream>
#include <iomanip>
#include <fstream>

#include "tet.hpp"

/* Global option list. */
const string globaloptlist = "searchpath={../data "
		 "../../../resource/cmap}";

/* Document specific option list. */
const string docoptlist = "";

/* Page-specific option list. */
const string pageoptlist = "granularity=line";

/* Search text with at least this size (use 0 to catch all sizes). */
const double fontsizetrigger = 10;

/* Catch text where the font name contains this string (use empty string to
 * catch all font names).
 */
const string fontnametrigger = "Bold";

int main(int argc, char **argv)
{
    TET tet;
    int pageno = 0;

    if (argc != 2)
    {
	cerr << "usage: fontfilter <infilename>" << endl;
	return(2);
    }

    try
    {
	tet.set_option(globaloptlist);

	int doc = tet.open_document(argv[1], docoptlist);
	if (doc == -1)
	{
	    cerr << "Error " << tet.get_errnum() << " in "
		    << tet.get_apiname() << "(): " << tet.get_errmsg() << endl;
	    return(2);
	}

	/*
	 * Loop over pages in the document
	 */
	int n_pages = (int) tet.pcos_get_number(doc, "length:pages");
	for (pageno = 1; pageno <= n_pages; ++pageno)
	{
	    string text;
	    int page = tet.open_page(doc, pageno, pageoptlist);

	    if (page == -1)
	    {
		cerr << "Error " << tet.get_errnum() << " in "
		    << tet.get_apiname() << "(): " << tet.get_errmsg() << endl;
		continue; /* try next page */
	    }

	    /* Retrieve all text fragments for the page */
	    while ((text = tet.get_text(page)) != "")
	    {
		const TET_char_info *ci;

		/* Loop over all characters */
		while ((ci = tet.get_char_info(page)) != NULL)
		{
		    string fontname;
                    char path[256];

		    /* We need only the font name and size; the text
		     * position could be fetched from ci->x and ci->y.
		     */
                    sprintf(path, "fonts[%d]/name", ci->fontid);
                    fontname = tet.pcos_get_string(doc, path);

		    /* Check whether we found a match */
		    if (ci->fontsize >= fontsizetrigger
			    && fontname.find(fontnametrigger) != string::npos)
		    {
			cout << "[" << fontname << " "
			    << fixed << setprecision(2) << ci->fontsize << "] " 
			    << text << endl;
		    }

		    /*
		     * In this sample we check only the first character of
		     * each fragment.
		     */
		    break;
		}
	    }

	    if (tet.get_errnum() != 0)
	    {
		cerr << "Error " << tet.get_errnum() << " in "
		    << tet.get_apiname() << "(): " << tet.get_errmsg() << endl;
	    }

	    tet.close_page(page);
	}

	tet.close_document(doc);
    }
    catch (TET::Exception &ex) {
	if (pageno == 0) {
	    cerr << "Error " << ex.get_errnum() << " in "
		    << ex.get_apiname() << "(): " << ex.get_errmsg() << endl;
	} else {
	    cerr << "Error " << ex.get_errnum() << " in "
		    << ex.get_apiname() << "() on page " << pageno << ": "
		    << ex.get_errmsg() << endl;
	}
	return(2);
    }
    return(0);
}
