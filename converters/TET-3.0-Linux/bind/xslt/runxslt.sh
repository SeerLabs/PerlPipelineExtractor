#!/bin/sh
# Shell script for applying XSLT stylesheets to TETML output
# via the xsltproc command-line program
#
# $Id:  $

XSLTPROC=xsltproc

$XSLTPROC --output concordance.txt concordance.xsl FontReporter.tetml
$XSLTPROC --output index.txt index.xsl FontReporter.tetml
$XSLTPROC --output table.csv table.xsl FontReporter.tetml
$XSLTPROC --output textonly.txt textonly.xsl FontReporter.tetml
$XSLTPROC --output metadata.txt metadata.xsl FontReporter.tetml
$XSLTPROC --output fontfilter.txt fontfilter.xsl FontReporter.tetml
$XSLTPROC --output fontstat.txt fontstat.xsl FontReporter.tetml
$XSLTPROC --output fontfinder.txt fontfinder.xsl FontReporter.tetml
$XSLTPROC --output tetml2html.html tetml2html.xsl FontReporter.tetml
