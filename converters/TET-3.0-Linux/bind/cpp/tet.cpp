/*---------------------------------------------------------------------------*
 |              TET - The PDFlib Text Extraction Toolkit                     |
 +---------------------------------------------------------------------------+
 |           Copyright (c) 2005 PDFlib GmbH. All rights reserved.            |
 +---------------------------------------------------------------------------+
 |          Proprietary source code -- do not redistribute!                  |
 *---------------------------------------------------------------------------*/

// $Id: tet.cpp,v 1.19 2008/12/18 15:52:27 stm Exp $
//
// Implementation of C++ wrapper for TET
//
//

#include "tet.hpp"
#include <cstring>

#define CHAR(s) (s).c_str()
#define LEN(s)  ((int) (s).size())

#if defined(_MSC_VER) && defined(_MANAGED)
/*
 * As it is not possible to compile the C++ wrapper truly as managed code, we
 * leave it as unmanaged code, as it is anyway only a thin layer over the
 * unmanaged PDFlib C DLL.
 * The reason that it is not possible to make it wholly managed is that even
 * with C++ try/catch the error handler will be compiled as native code, as
 * a function pointer is taken of it and passed to the PDFlib C library.
 */
#pragma unmanaged
#endif

TET::Exception::Exception(string errmsg, int errnum, string apiname,
	void *opaque)
: m_errmsg(errmsg),
  m_errnum(errnum),
  m_apiname(apiname),
  m_opaque(opaque)
{ }

string TET::Exception::get_errmsg() { return m_errmsg; }
int TET::Exception::get_errnum() { return m_errnum; }
string TET::Exception::get_apiname() { return m_apiname; }
const void * TET::Exception::get_opaque() { return m_opaque; }

#define TETCPP_TRY	TET_TRY(tet)
#define TETCPP_CATCH  \
TET_CATCH(tet) {\
    throw Exception(TET_get_errmsg(tet), TET_get_errnum(tet),\
			    TET_get_apiname(tet), TET_get_opaque(tet));\
}

TET::TET(tet_error_fp errorhandler, void *opaque)
{

    tet = TET_new2(errorhandler, opaque);

    if (tet == (TET_cpp *)0) {
	throw Exception("No memory for TET object", 0, "tetlib.cpp", opaque);
    }

    TETCPP_TRY
    {
	TET_set_option(tet, "binding={C++} objorient");
    }
    TETCPP_CATCH;
}

TET::~TET()
{
    TET_delete(tet);
}


void
TET::close_document(int doc)
{
    TETCPP_TRY
        TET_close_document(tet, doc);
    TETCPP_CATCH;
}

void
TET::close_page( int page)
{
    TETCPP_TRY
        TET_close_page(tet, page);
    TETCPP_CATCH;
}

void
TET::create_pvf(string filename, const void *data, size_t size,
        string optlist)
{       
    TETCPP_TRY
        TET_create_pvf(tet, CHAR(filename), 0, data, size, CHAR(optlist));
    TETCPP_CATCH;
}   

void
TET::delete_pvf(string filename)
{
    TETCPP_TRY
        TET_delete_pvf(tet, CHAR(filename), 0);
    TETCPP_CATCH;
}

string
TET::get_apiname()
{
    const char *retval = NULL;

    TETCPP_TRY
        retval = TET_get_apiname(tet);
    TETCPP_CATCH;

    if (retval)
	return retval;
    else
	return "";
}

string
TET::get_errmsg()
{
    const char *retval = NULL;

    TETCPP_TRY
        retval = TET_get_errmsg(tet);
    TETCPP_CATCH;

    if (retval)
	return retval;
    else
	return "";
}

int
TET::get_errnum()
{
    int retval = 0;

    TETCPP_TRY
	retval = TET_get_errnum(tet);
    TETCPP_CATCH;

    return retval;

}

const
TET_char_info *
TET::get_char_info(int page)
{
    const TET_char_info *retval = (TET_char_info *)NULL;

    TETCPP_TRY
	retval = TET_get_char_info(tet, page);
    TETCPP_CATCH;

    return retval;
}

void *
TET::get_opaque()
{
    void *retval = NULL;

    TETCPP_TRY
	retval = TET_get_opaque(tet);
    TETCPP_CATCH;

    return retval;
}

const char *
TET::get_image_data(int doc, size_t *length, int imageid, string optlist)
{
    const char * retval = NULL;

    TETCPP_TRY
	retval = TET_get_image_data(tet, doc, length, imageid, CHAR(optlist));
    TETCPP_CATCH;

    return retval;
}

const TET_image_info *
TET::get_image_info(int page)
{
    const TET_image_info *retval = (TET_image_info *)NULL;

    TETCPP_TRY
	retval = TET_get_image_info(tet, page);
    TETCPP_CATCH;

    return retval;
}

string
TET::get_text(int page)
{
    const char *cretval = NULL;
    string retval = "";
    int len;

    TETCPP_TRY
	cretval = TET_get_text(tet, page, &len);
	if (cretval)
	    retval = string(cretval, 2 * len);
    TETCPP_CATCH;

    return retval;
}

const char*
TET::get_xml_data(int doc, size_t *length, string optlist)
{
    const char *retval = NULL;

    TETCPP_TRY
	retval = TET_get_xml_data(tet, doc, length, CHAR(optlist));
    TETCPP_CATCH;

    return retval;
}

int
TET::open_document(string filename, string optlist)
{
    int retval = 0;

    TETCPP_TRY
	retval = TET_open_document(tet, CHAR(filename), 0, CHAR(optlist));
    TETCPP_CATCH;

    return retval;
}

int
TET::open_document_callback(void *opaque, size_t filesize,
    size_t (*readproc)(void *opaque, void *buffer, size_t size),
    int (*seekproc)(void *opaque, long offset), string optlist)
{
    int retval = 0;

    TETCPP_TRY
	retval = TET_open_document_callback(tet, opaque, filesize, readproc, seekproc, CHAR(optlist));
    TETCPP_CATCH;

    return retval;
}

int
TET::open_document_mem(const void *data, size_t length, string optlist)
{
    int retval = 0;

    TETCPP_TRY
	retval = TET_open_document_mem(tet, data, length, CHAR(optlist));
    TETCPP_CATCH;
    
    return retval;
}

int
TET::open_page(int doc, int pageno, string optlist)
{
    int retval = 0;

    TETCPP_TRY
	retval = TET_open_page(tet, doc, pageno, CHAR(optlist));
    TETCPP_CATCH;

    return retval;
}

double
TET::pcos_get_number(int doc, string path)
{
    double retval = 0;

    TETCPP_TRY
	retval = TET_pcos_get_number(tet, doc, "%s", CHAR(path));
    TETCPP_CATCH;

    return retval;
}

string
TET::pcos_get_string(int doc, string path)
{
    const char *cretval = NULL;
    string retval = "";

    TETCPP_TRY
	cretval = TET_pcos_get_string(tet, doc, "%s", CHAR(path));
	if (cretval)
	    retval = string(cretval, strlen(cretval));
    TETCPP_CATCH;

    return retval;
}

const unsigned char *
TET::pcos_get_stream(int doc, int *length, string optlist, string path)
{
    const unsigned char *retval = NULL;

    TETCPP_TRY
	retval = TET_pcos_get_stream(tet, doc, length, CHAR(optlist),
							    "%s", CHAR(path));
    TETCPP_CATCH;

    return retval;
}

int
TET::process_page(int doc, int pageno, string optlist)
{
    int retval = 0;

    TETCPP_TRY
	retval = TET_process_page(tet, doc, pageno, CHAR(optlist));
    TETCPP_CATCH;
    return retval;
}

void
TET::set_option(string optlist)
{
    TETCPP_TRY
	TET_set_option(tet, CHAR(optlist));
    TETCPP_CATCH;
}

string
TET::utf16_to_utf8(string utf16string)
{
    const char *retval = NULL;

    TETCPP_TRY
        retval = TET_utf16_to_utf8(tet, CHAR(utf16string),
                        (int) LEN(utf16string), NULL);
    TETCPP_CATCH;

    if (retval)
        return retval;
    else
        return "";
}

string
TET::utf32_to_utf16(string utf32string, string ordering)
{
    int size;
    const char *buf;
    string retval = "";

    TETCPP_TRY
        buf = TET_utf32_to_utf16(tet, CHAR(utf32string), LEN(utf32string),
		CHAR(ordering), &size);
        if (buf)
            retval = string(buf, size);
    TETCPP_CATCH;

    return retval;
}

string
TET::utf8_to_utf16(string utf8string, string ordering)
{
    int size;
    const char *buf;
    string retval = "";

    TETCPP_TRY
        buf = TET_utf8_to_utf16(tet, CHAR(utf8string), CHAR(ordering), &size);
        if (buf)
            retval = string(buf, size);
    TETCPP_CATCH;

    return retval;
}

int
TET::write_image_file(int doc, int imageid, string optlist)
{
    int retval;

    TETCPP_TRY
        retval = TET_write_image_file(tet, doc, imageid, CHAR(optlist));
    TETCPP_CATCH;

    return retval;
}
