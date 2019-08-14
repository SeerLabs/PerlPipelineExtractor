/*---------------------------------------------------------------------------*
 |           TET - The PDFlib Text Extraction Toolkit                        |
 +---------------------------------------------------------------------------+
 |       Copyright (c) 2002-2009 PDFlib GmbH. All rights reserved.           |
 *---------------------------------------------------------------------------*
 |          Proprietary source code -- do not redistribute!                  |
 *---------------------------------------------------------------------------*/

/* $Id: tetlib.h,v 1.135.2.5 2009/02/11 13:06:34 rjs Exp $
 *
 * TET public function declarations
 *
 */

/*
 * ----------------------------------------------------------------------
 * Setup, mostly Windows calling conventions and DLL stuff
 * ----------------------------------------------------------------------
 */

#ifndef TETLIB_H
#define TETLIB_H

#include <stdio.h>

#ifdef WIN32

#if !defined(PDFLIB_CALL)
#define PDFLIB_CALL     __cdecl
#endif

#if !defined(PDFLIB_API)
#ifdef PDFLIB_EXPORTS
#define PDFLIB_API __declspec(dllexport) /* prepare a DLL (internal use only) */

#elif defined(PDFLIB_DLL)
#define PDFLIB_API __declspec(dllimport) /* library clients: import DLL */

#else   /* !PDFLIB_DLL */
#define PDFLIB_API /* */        /* default: generate or use static library */

#endif  /* !PDFLIB_DLL */
#endif /* !PDFLIB_API */

#else   /* !WIN32 */

#if ((defined __IBMC__ || defined __IBMCPP__) && defined __DLL__ && defined OS2)
    #define PDFLIB_CALL _Export
    #define PDFLIB_API
#endif  /* IBM VisualAge C++ DLL */

#ifndef PDFLIB_CALL
#define PDFLIB_CALL
#endif
#ifndef PDFLIB_API
#define PDFLIB_API
#endif

#endif  /* !WIN32 */

/* export all symbols for a shared library on the Mac */
#if defined(__MWERKS__) && defined(PDFLIB_EXPORTS)
#pragma export on
#endif

/* Make our declarations C++ compatible */
#ifdef __cplusplus
extern "C" {
#endif

/*
 * There's a redundant product name literal elsewhere that needs to be
 * changed with this one!
 */
#define IFILTER_PRODUCTNAME     "TET PDF IFilter"
#define IFILTER_WCHAR_PRODUCTNAME L"TET PDF IFilter"
#define IFILTER_PRODUCTDESCR    "PDFlib TET PDF IFilter"
#define IFILTER_COPYRIGHT \
        "(c) 2008-2009 PDFlib GmbH  www.pdflib.com  sales@pdflib.com\n"

#define IFILTER_MAJORVERSION	3
#define IFILTER_MINORVERSION	0
#define IFILTER_REVISION        0
/* ALWAYS change both version strings in the same way */
#define IFILTER_VERSIONSTRING	"3.0"
#define IFILTER_WCHAR_VERSIONSTRING	L"3.0"

#define TET_PRODUCTNAME         "TET"
#define TET_PRODUCTDESCR        "PDFlib Text Extraction Toolkit"
#define TET_COPYRIGHT \
        "(c) 2002-2009 PDFlib GmbH  www.pdflib.com  sales@pdflib.com\n"

#define TET_MAJORVERSION	3
#define TET_MINORVERSION	0
#define TET_REVISION		0
/* ALWAYS change both version strings in the same way */
#define TET_VERSIONSTRING	"3.0p1"
#define TET_WCHAR_VERSIONSTRING L"3.0p1"

/* Opaque data type for the TET context. */
#if !defined(TET) || defined(ACTIVEX)
typedef struct TET_s TET;
#endif

/*
 * Enabling "Enums are always int" is necessary because
 * our libraries are compiled with this setting, and clients may use
 * a different setting.
 */
#if defined(__MWERKS__)
#pragma enumsalwaysint on
#endif


/*
 * ----------------------------------------------------------------------
 * pCOS-specific enums and defines
 * ----------------------------------------------------------------------
 */

/*
 * PDFlib GmbH products implement the following pCOS interface numbers:
 *
 * pCOS interface   Products
 * 1                TET 2.0, 2.1
 * 2                pCOS 1.0
 * 3                PDFlib+PDI 7, PPS 7, TET 2.2, pCOS 2.0, PLOP 3.0, TET 2.3
 * 4                PLOP 4.0, TET 3.0
 */

#ifndef PCOS_INTERFACE
#define PCOS_INTERFACE	4

/* document access levels.
*/
typedef enum
{
    pcos_mode_minimum	 = 0, /* encrypted doc (opened w/o password)	      */
    pcos_mode_restricted = 1, /* encrypted doc (opened w/ user password)      */
    pcos_mode_full	 = 2  /* unencrypted doc or opened w/ master password */
} pcos_mode;


/* object types.
*/
typedef enum
{
    pcos_ot_null	= 0,
    pcos_ot_boolean	= 1,
    pcos_ot_number	= 2,
    pcos_ot_name	= 3,
    pcos_ot_string	= 4,
    pcos_ot_array	= 5,
    pcos_ot_dict	= 6,
    pcos_ot_stream	= 7,
    pcos_ot_fstream	= 8
} pcos_object_type;

#endif /* PCOS_INTERFACE */


/*
 * ----------------------------------------------------------------------
 * TET-specific enums, structures, and defines
 * ----------------------------------------------------------------------
 */

/* Image formats returned by TET_write_image_file() */
typedef enum
{
    tet_if_error= -1,
    tet_if_auto	=  0,
    tet_if_tiff	= 10,
    tet_if_jpeg	= 20,
    tet_if_jpx	= 30,
    tet_if_raw	= 40		/* unsupported */
} tet_image_format;

/* TET_char_info character types with real geometry info.
*/
#define TET_CT__REAL		0
#define TET_CT_NORMAL		0
#define TET_CT_SEQ_START	1

/* TET_char_info character types with artificial geometry info.
*/
#define TET_CT__ARTIFICIAL	10
#define TET_CT_SEQ_CONT		10
#define TET_CT_SUR_TRAIL	11
#define TET_CT_INSERTED		12


/* TET_char_info text rendering modes.
*/
#define TET_TR_FILL		0	/* fill text                          */
#define TET_TR_STROKE		1	/* stroke text (outline)              */
#define TET_TR_FILLSTROKE	2	/* fill and stroke text               */
#define TET_TR_INVISIBLE	3	/* invisible text                     */
#define TET_TR_FILL_CLIP	4	/* fill text and
                                           add it to the clipping path        */
#define TET_TR_STROKE_CLIP	5	/* stroke text and
                                           add it to the clipping path        */
#define TET_TR_FILLSTROKE_CLIP	6	/* fill and stroke text and
                                           add it to the clipping path        */
#define TET_TR_CLIP		7	/* add text to the clipping path      */


typedef struct
{
    int	        uv;		/* current character in UTF-32                */
    int		type;		/* character type, see TET_CT_* above         */
    int		unknown;	/* 1 if glyph couldn't be mapped to Unicode   */

    double	x;		/* x position of the char's reference point   */
    double	y;		/* y position of the char's reference point   */
    double	width;		/* horizontal character extent                */
    double	alpha;		/* text baseline angle in degrees             */
    double	beta;		/* vertical character slanting angle          */

    int		fontid;		/* pCOS font id                               */
    double	fontsize;	/* size of the font                           */


    int		textrendering;	/* text rendering mode, see TET_TR_* above    */
} TET_char_info;


typedef struct
{
    double	x;		/* x position of the image's reference point */
    double	y;		/* y position of the image's reference point */
    double	width;		/* width and height of the image on the page */
    double	height;		/* in points, measured along the edges       */
    double	alpha;		/* direction of the pixel rows (in degrees)  */
    double	beta;		/* direction of columns, relative to the     */
    				/* perpendicular of alpha                    */
    int		imageid;	/* pCOS image id	                     */
} TET_image_info;


/*
 * ----------------------------------------------------------------------
 * TET API functions
 * ----------------------------------------------------------------------
 */

/* Release a document handle and all internal resources related to that
   document. */
PDFLIB_API void PDFLIB_CALL
TET_close_document(
    TET *tet,
    int doc);

/* Release a page handle and all related resources. */
PDFLIB_API void PDFLIB_CALL
TET_close_page(
    TET *tet,
    int page);

/* Create a named virtual read-only file from data provided in memory. */
PDFLIB_API void PDFLIB_CALL
TET_create_pvf(
    TET *tet,
    const char *filename,
    int len,
    const void *data,
    size_t size,
    const char *optlist);

/* Delete a TET object and release all related internal resources. */
PDFLIB_API void PDFLIB_CALL
TET_delete(TET *tet);

/* Delete a named virtual file and free its data structures (but not the
   contents).
   Returns: -1 if the virtual file exists but is locked, and
             1 otherwise.
 */
PDFLIB_API int PDFLIB_CALL
TET_delete_pvf(
    TET *tet,
    const char *filename,
    int len);

/* Get the name of the API function which caused an exception or failed. */
PDFLIB_API const char * PDFLIB_CALL
TET_get_apiname(
    TET *tet);

/* Get detailed information for the next character in the most recent
   text fragment. */
PDFLIB_API const TET_char_info * PDFLIB_CALL
TET_get_char_info(
    TET *tet,
    int page);

/* Get the text of the last thrown exception or the reason for a failed
   function call. */
PDFLIB_API const char * PDFLIB_CALL
TET_get_errmsg(
    TET *tet);

/* Get the number of the last thrown exception or the reason for a failed
   function call. */
PDFLIB_API int PDFLIB_CALL
TET_get_errnum(
    TET *tet);

/* Retrieve image data in memory. */
PDFLIB_API const char * PDFLIB_CALL
TET_get_image_data(
    TET *tet,
    int doc,
    size_t *length,
    int imageid,
    const char *optlist);

/* Retrieve information about the next image on the page (but not the actual
   pixel data). */
PDFLIB_API const TET_image_info * PDFLIB_CALL
TET_get_image_info(
    TET *tet,
    int page);

/* Fetch the opaque client pointer from with a TET context. Unsupported. */
PDFLIB_API void * PDFLIB_CALL
TET_get_opaque(TET *tet);

/* Get the next text fragment from a page's content. */
PDFLIB_API const char * PDFLIB_CALL
TET_get_text(
    TET *tet,
    int page,
    int *len);

/* Create a new TET object. */
PDFLIB_API TET * PDFLIB_CALL
TET_new(void);

/* Create a new TET context with a user-supplied error handler and
** opaque pointer. Unsupported.
*/
typedef void (*tet_error_fp)(TET *tet, int type, const char *msg);

PDFLIB_API TET * PDFLIB_CALL
TET_new2(tet_error_fp errorhandler, void *opaque);

/* Open a disk-based or virtual PDF document for content extraction. */
PDFLIB_API int PDFLIB_CALL
TET_open_document(
    TET *tet,
    const char *filename,
    int len,
    const char *optlist);

/* Open a PDF document from a custom data source for content extraction. */
PDFLIB_API int PDFLIB_CALL
TET_open_document_callback(
    TET *tet,
    void *opaque,
    size_t filesize,
    size_t (*readproc)(void *opaque, void *buffer, size_t size),
    int (*seekproc)(void *opaque, long offset),
    const char *optlist);

/* Deprecated; use TET_create_pvf( ) and TET_open_document( ). */
PDFLIB_API int PDFLIB_CALL
TET_open_document_mem(
    TET *tet,
    const void *data,
    size_t size,
    const char *optlist);

/* Open a page for content extraction. */
PDFLIB_API int PDFLIB_CALL
TET_open_page(
    TET *tet,
    int doc,
    int pagenumber,
    const char *optlist);

/* Get the value of a pCOS path with type number or boolean. */
PDFLIB_API double PDFLIB_CALL
TET_pcos_get_number(
    TET *tet,
    int doc,
    const char *path, ...);

/* Get the value of a pCOS path with type name, string or boolean. */
PDFLIB_API const char * PDFLIB_CALL
TET_pcos_get_string(
    TET *tet,
    int doc,
    const char *path, ...);

/* Get the contents of a pCOS path with type stream, fstream, or string. */
PDFLIB_API const unsigned char * PDFLIB_CALL
TET_pcos_get_stream(
    TET *tet,
    int doc,
    int *length,
    const char *optlist,
    const char *path, ...);

/* Set one or more global options for TET. */
PDFLIB_API void PDFLIB_CALL
TET_set_option(
    TET *tet,
    const char *optlist);

/* Convert a string from UTF-16 format to UTF-8. */
PDFLIB_API const char * PDFLIB_CALL
TET_utf16_to_utf8(
    TET *tet,
    const char *utf16string,
    int len,
    int *size);

/* Convert a string from UTF-8 format to UTF-16. */
PDFLIB_API const char * PDFLIB_CALL
TET_utf8_to_utf16(
   TET *tet,
   const char *utf8string,
   const char *ordering,
   int *size);

/* Convert a string from UTF-32 format to UTF-16. */
PDFLIB_API const char * PDFLIB_CALL
TET_utf32_to_utf16(
   TET *tet,
   const char *utf32string,
   int len,
   const char *ordering,
   int *size);

/* Write image data to disk. */
PDFLIB_API int PDFLIB_CALL
TET_write_image_file(
    TET *tet,
    int doc,
    int imageid,
    const char *optlist);

/* Process a page and create TETML output. */
PDFLIB_API int PDFLIB_CALL
TET_process_page(
    TET *tet,
    int doc,
    int pageno,
    const char *optlist);

/* Retrieve data from memory. */
PDFLIB_API const char * PDFLIB_CALL
TET_get_xml_data(
    TET *tet,
    int doc,
    size_t *length,
    const char *optlist);


/*
 * ----------------------------------------------------------------------
 * Exception handling with try/catch implementation
 * ----------------------------------------------------------------------
 */

/* Set up an exception handling frame; must always be paired with TET_CATCH().
*/
#define TET_TRY(tet)		if (setjmp(tet_jbuf(tet)->jbuf) == 0)

/* Inform the exception machinery that a TET_TRY() will be left without
   entering the corresponding TET_CATCH( ) clause. */

#define TET_EXIT_TRY(tet)	tet_exit_try(tet)

/* Catch an exception; must always be paired with TET_TRY(). */

#define TET_CATCH(tet)		if (tet_catch(tet))

/* Re-throw an exception to another handler. */

#define TET_RETHROW(tet)	tet_rethrow(tet)


/*
 * ----------------------------------------------------------------------
 * Private stuff, do not use explicitly but only via the above macros!
 * ----------------------------------------------------------------------
 */

#include <setjmp.h>

typedef struct
{
    jmp_buf	jbuf;
} tet_jmpbuf;

PDFLIB_API tet_jmpbuf * PDFLIB_CALL
tet_jbuf(
    TET *tet);

PDFLIB_API void PDFLIB_CALL
tet_exit_try(
    TET *tet);

PDFLIB_API int PDFLIB_CALL
tet_catch(
    TET *tet);

PDFLIB_API void PDFLIB_CALL
tet_rethrow(
    TET *tet);

PDFLIB_API void PDFLIB_CALL
tet_throw(
    TET *tet,
    const char *parm1,
    const char *parm2,
    const char *parm3);

#ifdef __cplusplus
}	/* extern "C" */
#endif

#if defined(__MWERKS__) && defined(PDFLIB_EXPORTS)
#pragma export off
#endif

#if defined(__MWERKS__)
#pragma enumsalwaysint reset
#endif

#endif /* TETLIB_H */

