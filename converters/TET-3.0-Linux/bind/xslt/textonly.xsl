<?xml version="1.0"?>
<!--
    (C) PDFlib GmbH 2008 www.pdflib.com

    Purpose: Extract raw text from TETML input, including text from attachments.
    Nested attachments are processed recursively.
    
    Required input: TETML in any mode
    
    Stylesheet parameters: none
    
    Version: $Id: textonly.xsl,v 1.6 2008/11/18 14:29:36 stm Exp $
-->

<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:tet="http://www.pdflib.com/XML/TET3/TET-3.0"
>
    <xsl:output method="text" />

    <xsl:template match="/">
        <xsl:apply-templates select="tet:TET/tet:Document" />
    </xsl:template>

    <!-- Extract text from the top-level document, then process any attachments -->
    <xsl:template match="tet:TET/tet:Document">
        <xsl:apply-templates select="tet:Pages//tet:Text" />
        <xsl:apply-templates select="tet:Attachments/tet:Attachment/tet:Document" />
    </xsl:template>

    <!-- Recursively process attachments -->
    <xsl:template match="tet:Attachment/tet:Document">
        <xsl:variable name="attachment-id">
            <xsl:text>attachment: </xsl:text>
            <xsl:value-of select="@filename" />
            <xsl:text> level: </xsl:text>
            <xsl:value-of select="../@level" />
        </xsl:variable>
        
        <xsl:text>--- begin </xsl:text>
        <xsl:value-of select="$attachment-id" />
        <xsl:text> ---&#xa;</xsl:text>
        
        <xsl:apply-templates select="tet:Pages//tet:Text" />
        <xsl:apply-templates select="tet:Attachments/tet:Attachment/tet:Document" />
        
        <xsl:text>--- end </xsl:text>
        <xsl:value-of select="$attachment-id" />
        <xsl:text> ---&#xa;</xsl:text>
    </xsl:template>

    <!-- Output of raw text -->
    <xsl:template match="tet:Text">
        <xsl:value-of select="." />
        <xsl:text>&#xa;</xsl:text>
    </xsl:template>
</xsl:stylesheet>
