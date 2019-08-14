/* TET sample application for dumping PDF information with pCOS
 *
 * $Id: dumper.cpp,v 1.6 2008/12/15 20:26:53 rjs Exp $
 */

#include <iostream>
#include <fstream>

#include "tet.hpp"

const string docoptlist = "requiredmode=minimum";
const string globaloptlist = "";

int main(int argc, char **argv)
{
    int exitstat = 0;
    int i, count;
    TET tet;
    string  optlist;

    /* This is where input files live. Adjust as necessary. */
    const string searchpath("../data");

    try
    {
	if (argc != 2)
	{
	    cerr << "usage: dumper <filename>" << endl;
	    return(2);
	}

	tet.set_option(globaloptlist);

	optlist = "searchpath={" + searchpath + "}";
	tet.set_option(optlist);

	int doc = tet.open_document(argv[1], docoptlist);

	if (doc == -1)
	{
	    cerr << "Error " << tet.get_errnum()
		<< " in " << tet.get_apiname()
		<< "(): " << tet.get_errmsg() << endl;
	    return(2);
	}

	/* --------- general information (always available) */
	int pcosmode = (int) tet.pcos_get_number(doc, "pcosmode");

	cout << "   File name: " << 
	    tet.pcos_get_string(doc, "filename") << endl;

	cout << " PDF version: " << 
	    tet.pcos_get_string(doc, "pdfversionstring") << endl;

	cout << "  Encryption: " << 
	    tet.pcos_get_string(doc, "encrypt/description") << endl;

	cout << "   Master pw: " << 
	    (tet.pcos_get_number(doc, "encrypt/master") != 0 
	    ? "yes" : "no") << endl;

	cout << "     User pw: " << 
	    (tet.pcos_get_number(doc, "encrypt/user") != 0
	    ? "yes" : "no") << endl;

	cout << "Text copying: " <<
	    (tet.pcos_get_number(doc, "encrypt/nocopy") != 0
	    ? "no" : "yes") << endl;

	cout << "  Linearized: " <<
	    (tet.pcos_get_number(doc, "linearized") != 0
	    ? "yes" : "no") << endl;

	if (pcosmode == 0)
	{
	    cout << "Minimum mode: no more information available\n\n" << endl;
	    return(0);
	}

	cout << "PDF/X status: " << tet.pcos_get_string(doc, "pdfx") << endl;

	cout << "PDF/A status: " << tet.pcos_get_string(doc, "pdfa") << endl;

	cout << "  Tagged PDF: " <<
	    (tet.pcos_get_number(doc, "tagged") != 0 ? "yes" : "no") << endl;

	cout << "" << endl;

	cout << "No. of pages: " <<
		(int) tet.pcos_get_number(doc, "length:pages") << endl;

	cout << " Page 1 size: width="
		<< tet.pcos_get_number(doc, "pages[0]/width") << ", height="
		<< tet.pcos_get_number(doc, "pages[0]/height") << endl;

	count = (int) tet.pcos_get_number(doc, "length:fonts");
	cout << "No. of fonts: " << count << endl;

	for (i = 0; i < count; i++) {
	    char fonts[256];

	    sprintf(fonts, "fonts[%d]/embedded", i);
	    if (tet.pcos_get_number(doc, fonts) != 0)
		cout << "embedded ";
	    else
		cout << "unembedded ";

	    sprintf(fonts, "fonts[%d]/type", i);
	    cout << tet.pcos_get_string(doc, fonts) + " font ";
	    sprintf(fonts, "fonts[%d]/name", i);
	    cout << tet.pcos_get_string(doc, fonts) << endl;
	}

	cout <<  "" << endl;

	bool plainmetadata =
	    tet.pcos_get_number(doc, "encrypt/plainmetadata") != 0;

	if (pcosmode == 1 && !plainmetadata
		&& tet.pcos_get_number(doc, "encrypt/nocopy") != 0) {
	    cout << "Restricted mode: no more information available" << endl;
	    return(0);
	}

	string objtype;
	count = (int) tet.pcos_get_number(doc, "length:/Info");

	for (i = 0; i < count; i++)
	{
	    char info[256];
            string key;

            sprintf(info, "type:/Info[%d]", i);
            objtype = tet.pcos_get_string(doc, info);

            sprintf(info, "/Info[%d].key", i);
            key = tet.pcos_get_string(doc, info);
            cout.width(12);
            cout << key << ": ";

            /* Info entries can be stored as string or name objects */
            if (objtype == "name" || objtype == "string") {
                sprintf(info, "/Info[%d]", i);
                cout << "'" + tet.pcos_get_string(doc, info) << "'" << endl;
            } else {
                sprintf(info, "type:/Info[%d]", i);
                cout << "(" + tet.pcos_get_string(doc,info) << " object)"
			<< endl;
            }
	}

	cout << endl << "XMP meta data: ";

	objtype = tet.pcos_get_string(doc, "type:/Root/Metadata");
	if (objtype == "stream") {
	    const unsigned char *contents;
	    int len;

	    contents = tet.pcos_get_stream(doc, &len, "", "/Root/Metadata");
	    cout << len << " bytes ";
	    cout << "";

	    /* This demonstrates Unicode conversion. It doesn't work on
	     * EBCDIC platforms because on those TET_utf8_to_utf16()
	     * expects input in UTF-8-EBCDIC format, while TET_pcos_get_stream()
	     * always retrieves the original data in UTF-8 format.
	     */
	    string utf8((const char *)contents);
	    string dummy = tet.utf8_to_utf16(utf8, "utf16");
	    cout << "(" << dummy.length()/2
			<< " Unicode characters)" << endl << endl;
	} else {
	    cout << "not present" << endl << endl;
	}
	
	tet.close_document(doc);
    }
    catch (TET::Exception &ex) {
	cerr << "Error " << ex.get_errnum()
	    << " in " << ex.get_apiname()
	    << "(): " << ex.get_errmsg() << endl;
    }
    return(0);
}
