/*---------------------------------------------------------------------------*
 |              TET - The PDFlib Text Extraction Toolkit                     |
 +---------------------------------------------------------------------------+
 |           Copyright (c) 2005 PDFlib GmbH. All rights reserved.            |
 +---------------------------------------------------------------------------+
 |          Proprietary source code -- do not redistribute!                  |
 *---------------------------------------------------------------------------*/

// $Id: tet.hpp,v 1.17 2008/09/25 14:17:58 rjs Exp $
//
// C++ wrapper for TET
//
//

#ifndef TETLIB_HPP
#define TETLIB_HPP

#include <string>

using namespace std;

// We use TET as a C++ class name, therefore hide the actual C struct
// name for TET usage with C++.
typedef struct TET_s TET_cpp;
#define TET TET_cpp
#include "tetlib.h"
#undef TET

#if defined(_MSC_VER) && defined(_MANAGED)
/*
 * Dummy declaration to prevent linker warning for .NET. If it doesn't see
 * a declaration of the structure it will complain. The structure is never
 * used in the .NET wrapper, so it is safe to declare it as an empty structure
 * here.
 */
struct TET_s {};

#pragma unmanaged
#endif

// The C++ class wrapper for TET

#if defined(_MSC_VER)
// Suppress Visual C++ warnings about ignored exception specifications.
#pragma warning(disable: 4290)
#endif

class TET {
public:
    class Exception
    {
    public:
	Exception(string errmsg, int errnum, string apiname, void *opaque);
	string get_errmsg();
	int get_errnum();
	string get_apiname();
	const void *get_opaque();
    private:
	string m_errmsg;
	int m_errnum;
	string m_apiname;
	void * m_opaque;
    }; // Exception

    TET(tet_error_fp errorhandler = NULL, void *opaque = NULL);

    ~TET();

    void close_document(int doc);
    void close_page( int page);
    void create_pvf(string filename, const void *data, size_t size,
        string optlist);
    void delete_pvf(string filename);
    string get_apiname();
    string get_errmsg();
    int get_errnum();
    const TET_char_info * get_char_info(int page);
    void * get_opaque();
    const char *get_image_data(int doc, size_t *length, int imageid,
	string optlist);
    const char *get_xml_data(int doc, size_t *length, string optlist);
    const TET_image_info * get_image_info(int page);
    string get_text(int page);
    int open_document(string filename, string optlist);
    int open_document_callback(void *opaque, size_t filesize,
	size_t (*readproc)(void *opaque, void *buffer, size_t size),
	int (*seekproc)(void *opaque, long offset), string optlist);
    int open_document_mem(const void *data, size_t lenght, string optlist);
    int open_page(int doc, int pageno, string optlist);
    double pcos_get_number(int doc, string path);
    string pcos_get_string(int doc, string path);
    const unsigned char * pcos_get_stream(int doc, int *length, string optlist,
	string path);
    int process_page(int doc, int pageno, string optlist);
    void set_option(string optlist);
    string utf16_to_utf8(string utf16string);
    string utf32_to_utf16(string utf32string, string ordering);
    string utf8_to_utf16(string utf8string, string ordering) ;
    int write_image_file(int doc, int imageid, string optlist);

private:

    TET_cpp *tet;

private:
    // Prevent use of copy constructor and assignment operator, as it is
    // fatal to copy the TET_cpp pointer to another object.
    TET(const TET&) {}
    TET& operator=(const TET&) { return *this; }
};

#if defined(_MSC_VER) && defined(_MANAGED)
#pragma managed
#endif

#endif	// TETLIB_HPP
