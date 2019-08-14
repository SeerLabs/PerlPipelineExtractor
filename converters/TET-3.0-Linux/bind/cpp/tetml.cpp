/**
 * Extract text from PDF document as XML. If an output filename is specified,
 * write the XML to the output file. Otherwise fetch the XML in memory, parse it
 * and print some information to System.out.
 * 
 * $Id: tetml.cpp,v 1.4 2008/12/23 17:31:09 rjs Exp $
 */

#include <iostream>
#include <fstream>

#include "tet.hpp"

/* Global option list. */
const string globaloptlist = "searchpath={../data "
		 "../../../resource/cmap}";

/* Document specific option list. */
const string basedocoptlist = "";

/* Page-specific option list. */
const string pageoptlist = "granularity=word";

/* Word counter for in-memory processing code. */
int word_count = 0;

/* set this to 1 to generate TETML output in memory */
int inmemory = 0;

int main(int argc, char **argv)
{
    TET tet;

    if (argc != 3)
    {
	cerr << "usage: tetml <pdffilename> <xmlfilename>" << endl;
	return(2);
    }

    try
    {
	tet.set_option(globaloptlist);
	char docoptlist[512];

	if (inmemory) {
	    sprintf(docoptlist, "tetml={} %s", basedocoptlist.c_str());
	} else {
	    sprintf(docoptlist, "tetml={filename=%s}",
			    argv[2], basedocoptlist.c_str());
	}

	if (inmemory) {
	    cout << "Processing TETML output for document \""
		    << argv[1] << "\" in memory..." << endl;
	} else {
	    cout << "Extracting TETML for document \""
		    << argv[1] << "\" to file \"" << argv[2] << "\"..." << endl;
	}
	
	int doc = tet.open_document(argv[1], docoptlist);
	if (doc == -1)
	{
	    cerr << "Error " << tet.get_errnum() << " in "
		    << tet.get_apiname() << "(): " << tet.get_errmsg() << endl;
	    return(2);
	}

	int n_pages = (int) tet.pcos_get_number(doc, "length:pages");

	/*
	 * Loop over pages in the document;
	 */
	for (int pageno = 0; pageno <= n_pages; ++pageno)
	{
	    tet.process_page(doc, pageno, pageoptlist);
	}

	/*
	 * This could be combined with the last page-related call.
	 */
	tet.process_page(doc, 0, "tetml={trailer}");

	if (inmemory)
	{
	    /* Retrieve the generated TETML data from memory. Since we have
	     * only a single call the result will contain the full TETML.
	     */
	    const char *tetml;
	    size_t len;
	    tetml = tet.get_xml_data(doc, &len, "");

	    if (tetml == NULL)
	    {
		cerr << "tetml: couldn't retrieve XML data" << endl;
		return(2);
	    }
	    ofstream ofp(argv[2]);
	    if (!ofp) {
		cerr << "tetml: couldn't open output file " << argv[2] << endl;
		return(2);
	    }
	    ofp << tetml;
	    ofp.close();
	}

	tet.close_document(doc);
    }
    catch (TET::Exception &ex) {
	cerr << "Error " << ex.get_errnum() << " in "
		<< ex.get_apiname() << "(): " << ex.get_errmsg() << endl;
    }
    return(0);
}
