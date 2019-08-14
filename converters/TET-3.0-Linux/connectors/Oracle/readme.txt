TET Connector for Oracle Text
=============================

This setup will work out-of-the-box if your database character set is
AL32UTF8. If the database character set is not AL32UTF8, a character set
conversion from UTF-8 to the database character set must be integrated into
the "tetfilter.sh" resp. "tetfilter.bat" scripts. On Unix/Linux platforms
this can be done for example with the "iconv" program.

Starting with Oracle Text 11.1.0.7 it should be possible to let the database
perform the character set conversion. Please refer to the section
"Using USER_FILTER with Charset and Format Columns" in the Oracle Text
11.1.0.7 documentation.

Installation and test on Unix/Linux:

Put the file "tetfilter.sh" into the directory $ORACLE_HOME/ctx/bin. Edit
the following line in this script to reflect the actual installation
directory of the TET package on your machine:

TETDIR="/home/user/TET-3.0-Linux"

The files "tetsetup_a.sql"/"tetcleanup_b.sql" and "tetsetup_b.sql"/
"tetcleanup_b.sql" can be used for quickly testing the filter with PDF files
that are included in the TET distribution. Edit the following line in the
"tetsetup_a.sql" script to reflect the actual installation directory of the
TET package on your machine:

define tetpath = '/home/user/TET-3.0-Linux'

Please refer to the section "TET Connector for Oracle Text" the TET Manual
for a detailed description how these scripts are intended to be used.

Installation and test on Windows:

Put the file "tetfilter.bat" into the directory %ORACLE_HOME%\bin. Edit
the following line in this script to reflect the actual installation
directory of the TET package on your machine:

SET TETDIR=C:\Program Files\PDFlib\TET 3.0

The files "tetsetup_a.sql"/"tetcleanip_a.sql" and "tetsetup_b.sql"/
"tetcleanup_b.sql" can be used for quickly testing the filter with PDF files
that are included in the TET distribution. Edit the following line in the
"tetsetup_a.sql" script to reflect the actual installation directory of the
TET package on your machine:

define tetpath = 'C:\Program Files\PDFlib\TET 3.0'

Please refer to the section "TET Connector for Oracle Text" the TET Manual
for a detailed description how these scripts are intended to be used.