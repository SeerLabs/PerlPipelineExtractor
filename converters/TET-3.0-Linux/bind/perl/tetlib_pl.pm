# $Id: tetlib_pl.pm,v 1.5 2008/08/21 16:55:38 rjs Exp $
package tetlib_pl;
require Exporter;
require DynaLoader;
$VERSION=3.0;
@ISA = qw(Exporter DynaLoader);
package tetlibc;
bootstrap tetlib_pl;
var_tetlib_init();
@EXPORT = qw( );

# ---------- BASE METHODS -------------

package tetlib_pl;

sub TIEHASH {
    my ($classname,$obj) = @_;
    return bless $obj, $classname;
}

sub CLEAR { }

sub this {
    my $ptr = shift;
    return tied(%$ptr);
}


# ------- FUNCTION WRAPPERS --------

package tetlib_pl;

*TET_delete = *tetlibc::TET_delete;
*TET_get_char_info = *tetlibc::TET_get_char_info;
*TET_get_image_info = *tetlibc::TET_get_image_info;
*TET_new = *tetlibc::TET_new;

*TET_close_document = *tetlibc::TET_close_document;
*TET_close_page = *tetlibc::TET_close_page;
*TET_create_pvf = *tetlibc::TET_create_pvf;
*TET_delete = *tetlibc::TET_delete;
*TET_delete_pvf = *tetlibc::TET_delete_pvf;
*TET_get_apiname = *tetlibc::TET_get_apiname;
*TET_get_errmsg = *tetlibc::TET_get_errmsg;
*TET_get_errnum = *tetlibc::TET_get_errnum;
*TET_get_image_data = *tetlibc::TET_get_image_data;
*TET_get_text = *tetlibc::TET_get_text;
*TET_open_document = *tetlibc::TET_open_document;
*TET_open_document_mem = *tetlibc::TET_open_document_mem;
*TET_open_page = *tetlibc::TET_open_page;
*TET_pcos_get_number = *tetlibc::TET_pcos_get_number;
*TET_pcos_get_string = *tetlibc::TET_pcos_get_string;
*TET_pcos_get_stream = *tetlibc::TET_pcos_get_stream;
*TET_set_option = *tetlibc::TET_set_option;
*TET_utf16_to_utf8 = *tetlibc::TET_utf16_to_utf8;
*TET_utf32_to_utf16 = *tetlibc::TET_utf32_to_utf16;
*TET_utf8_to_utf16 = *tetlibc::TET_utf8_to_utf16;
*TET_write_image_file = *tetlibc::TET_write_image_file;
*TET_process_page = *tetlibc::TET_process_page;
*TET_get_xml_data = *tetlibc::TET_get_xml_data;
@EXPORT = qw( 
TET_delete
TET_get_char_info
TET_get_image_info
TET_new

TET_close_document
TET_close_page
TET_create_pvf
TET_delete
TET_delete_pvf
TET_get_apiname
TET_get_errmsg
TET_get_errnum
TET_get_image_data
TET_get_text
TET_open_document
TET_open_document_mem
TET_open_page
TET_pcos_get_number
TET_pcos_get_string
TET_pcos_get_stream
TET_set_option
TET_utf16_to_utf8
TET_utf32_to_utf16
TET_utf8_to_utf16
TET_write_image_file
TET_process_page
TET_get_xml_data
);

# ------- VARIABLE STUBS --------

package tetlib_pl;

1;
