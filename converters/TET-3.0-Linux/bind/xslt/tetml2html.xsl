<?xml version="1.0"?>
<!--
    (C) PDFlib GmbH 2008 www.pdflib.com

    Purpose: convert TETML to HTML
    
    Required input: TETML in wordplus mode
    
    Stylesheet parameters:
    
    debug:              0: no debug info, >0: increasingly verbose
    
    toc-generate:       0: no table of contents, 1: generate table of contents
    toc-exclude-min, toc-exclude-max:
        Specify a range of pages to exclude from the generation of the HTML
        table of contents. This can be used to prevent duplicate entries if
        also entries in the PDF table of contents are detected as headings
        because of their font size.
    
    h<n>.min-size, h<n>.max-size, with n=1..5:
        "Para" elements must include at least one character whose size is greater
        or equal to the h<n>.min-size parameter and less than or equal to the
        h<n>.max-size parameter to be recognized as a h1..h5 heading

    Version: $Id: tetml2html.xsl,v 1.15 2008/11/19 08:33:50 stm Exp $
-->

<xsl:stylesheet version="1.0"
        xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
        xmlns:tet="http://www.pdflib.com/XML/TET3/TET-3.0">
        
        <xsl:output method="html" indent="yes"/>
        
        <xsl:param name="debug">0</xsl:param>
        
        <xsl:param name="toc-generate">1</xsl:param>
        <xsl:param name="toc-exclude-min">-1</xsl:param>
        <xsl:param name="toc-exclude-max">-1</xsl:param>
        
        <xsl:param name="h1.min-size">24.9</xsl:param>
        <xsl:param name="h1.max-size">10000</xsl:param>
        
        <xsl:param name="h2.min-size">24</xsl:param>
        <xsl:param name="h2.max-size">24.8</xsl:param>
                
        <xsl:param name="h3.min-size">15</xsl:param>
        <xsl:param name="h3.max-size">23.9</xsl:param>
                
        <xsl:param name="h4.min-size">10001</xsl:param>
        <xsl:param name="h4.max-size">10000</xsl:param>
                
        <xsl:param name="h5.min-size">10001</xsl:param>
        <xsl:param name="h5.max-size">10000</xsl:param>
        
        <xsl:template match="/">
		<!-- Make sure that the input TETML was prepared in wordplus mode including geometry -->
		<xsl:if test="tet:TET/tet:Document/tet:Pages/tet:Page/tet:Content[not(@granularity = 'word') or not(@geometry = 'true')]">
			<xsl:message terminate="yes">
				<xsl:text>Stylesheet tetml2html.xsl processing TETML for document '</xsl:text>
				<xsl:value-of select="tet:TET/tet:Document/@filename" />
				<xsl:text>': this stylesheet requires TETML in wordplus mode. </xsl:text>
				<xsl:text>Create the input in page mode "wordplus".</xsl:text>
			</xsl:message>
		</xsl:if>
        	<html>
        		<head>
        		<title>
        			<xsl:text>HTML version of </xsl:text>
        			<xsl:value-of select="tet:TET/tet:Document/@filename"/>
        		</title>
        		</head>
        		<body>
                                <xsl:if test="$toc-generate &gt; 0">
                                        <xsl:apply-templates
                                                select="tet:TET/tet:Document/tet:Pages/tet:Page[not(@number &gt;= $toc-exclude-min and
                                                                        @number &lt;= $toc-exclude-max)]" mode="toc" />
                                </xsl:if>
        			<xsl:apply-templates select="tet:TET/tet:Document/tet:Pages/tet:Page" mode="body" />
                	</body>
                </html>
        </xsl:template>
   
        <!--
                Group of templates for generating the Table of Contents. These
                templates are all defined with mode "toc". They generate
                links to anchors for all the Para elements that are identified
                as headings. 
        -->
        <xsl:template match="tet:Page" mode="toc">
                <xsl:for-each select="tet:Content/tet:Para">
                        <xsl:choose>
                                <xsl:when test="tet:Word/tet:Box/tet:Glyph[@size &gt;= $h1.min-size and @size &lt;= $h1.max-size]">
                                        <xsl:call-template name="toc-entry">
                                                <xsl:with-param name="toc-heading" select="'h1'" />
                                        </xsl:call-template>
                                </xsl:when>
                                <xsl:when test="tet:Word/tet:Box/tet:Glyph[@size &gt;= $h2.min-size and @size &lt;= $h2.max-size]">
                                        <xsl:call-template name="toc-entry">
                                                <xsl:with-param name="toc-heading" select="'h2'" />
                                        </xsl:call-template>
                                </xsl:when>
                                <xsl:when test="tet:Word/tet:Box/tet:Glyph[@size &gt;= $h3.min-size and @size &lt;= $h3.max-size]">
                                        <xsl:call-template name="toc-entry">
                                                <xsl:with-param name="toc-heading" select="'h3'" />
                                        </xsl:call-template>
                                </xsl:when>
                                <xsl:when test="tet:Word/tet:Box/tet:Glyph[@size &gt;= $h4.min-size and @size &lt;= $h4.max-size]">
                                        <xsl:call-template name="toc-entry">
                                                <xsl:with-param name="toc-heading" select="'h4'" />
                                        </xsl:call-template>
                                </xsl:when>
                                <xsl:when test="tet:Word/tet:Box/tet:Glyph[@size &gt;= $h5.min-size and @size &lt;= $h5.max-size]">
                                        <xsl:call-template name="toc-entry">
                                                <xsl:with-param name="toc-heading" select="'h5'" />
                                        </xsl:call-template>
                                </xsl:when>
                                <!-- no xsl:otherwise as normal Paras are suppressed in the TOC -->
                        </xsl:choose>
                </xsl:for-each>
        </xsl:template>
        
        <!--
                Generate an entry for the provided Para element
                as the specified heading element $toc-heading (h1..h5)
        -->
        <xsl:template name="toc-entry">
                <xsl:param name="toc-heading" />
                
                <xsl:element name="{$toc-heading}">
                        <a>
                                <xsl:attribute name="href"><xsl:text>#</xsl:text><xsl:value-of select="generate-id()"/></xsl:attribute>
                                <xsl:apply-templates select="tet:Word/tet:Text"/>
                        </a>
                </xsl:element>
        </xsl:template>
                          
        <!--
                Group of templates to generate the text body of the document.
                The headings are identified in the same manner as in toc mode,
                only that in this case the anchors are generated through
                "id" attributes for the h1, h2, ... elements. 
        -->
        <xsl:template match="tet:Page" mode="body">
        	<xsl:if test="$debug &gt; 0">
           	        <hr/><i>
   	        	<xsl:text>[Page </xsl:text>
   	        	<xsl:value-of select="@number"/>
   	        	<xsl:text> of </xsl:text>
   	        	<xsl:value-of select="ancestor::tet:Document[1]/@filename"/>
   	        	<xsl:text>]</xsl:text>
   	        	</i>
                        <xsl:apply-templates select="tet:Exception" />
   	        </xsl:if>
                <xsl:for-each select="tet:Content/tet:Para | tet:Content/tet:Table | tet:Content/tet:PlacedImage">
                        <xsl:choose>
                                <xsl:when test="local-name() = 'Para' and tet:Word/tet:Box/tet:Glyph[@size &gt;= $h1.min-size and @size &lt;= $h1.max-size]">
                                        <xsl:call-template name="heading">
                                                <xsl:with-param name="heading-type" select="'h1'" />
                                        </xsl:call-template>
                                </xsl:when>
                                <xsl:when test="local-name() = 'Para' and tet:Word/tet:Box/tet:Glyph[@size &gt;= $h2.min-size and @size &lt;= $h2.max-size]">
                                        <xsl:call-template name="heading">
                                                <xsl:with-param name="heading-type" select="'h2'" />
                                        </xsl:call-template>
                                </xsl:when>
                                <xsl:when test="local-name() = 'Para' and tet:Word/tet:Box/tet:Glyph[@size &gt;= $h3.min-size and @size &lt;= $h3.max-size]">
                                        <xsl:call-template name="heading">
                                                <xsl:with-param name="heading-type" select="'h3'" />
                                        </xsl:call-template>
                                </xsl:when>
                                <xsl:when test="local-name() = 'Para' and tet:Word/tet:Box/tet:Glyph[@size &gt;= $h4.min-size and @size &lt;= $h4.max-size]">
                                        <xsl:call-template name="heading">
                                                <xsl:with-param name="heading-type" select="'h4'" />
                                        </xsl:call-template>
                                </xsl:when>
                                <xsl:when test="local-name() = 'Para' and tet:Word/tet:Box/tet:Glyph[@size &gt;= $h5.min-size and @size &lt;= $h5.max-size]">
                                        <xsl:call-template name="heading">
                                                <xsl:with-param name="heading-type" select="'h5'" />
                                        </xsl:call-template>
                                </xsl:when>
                                <xsl:otherwise>
                                        <xsl:apply-templates mode="body" select="." />
                                </xsl:otherwise>
                        </xsl:choose>
                </xsl:for-each>
        </xsl:template>

        <!-- Print out exceptions in an eye-catching color -->
        <xsl:template match="tet:Exception">
                <div style="color: red">
                        <xsl:text>Exception occurred at page level:&#xa;"</xsl:text>
                        <xsl:value-of select="." />
                        <xsl:text>"</xsl:text>
                </div>
        </xsl:template>
        
        <!--
                Generate a heading element for the provided Para element
                as the specified heading element $heading-type (h1..h5)
        -->
        <xsl:template name="heading">
                <xsl:param name="heading-type" />
                
                <xsl:element name="{$heading-type}">
                        <xsl:attribute name="id"><xsl:value-of select="generate-id()"/></xsl:attribute>
                        <xsl:apply-templates select="tet:Word/tet:Text" />
                </xsl:element>
        </xsl:template>

        <xsl:template mode="body" match="tet:Para">
                <p><xsl:apply-templates select="tet:Word/tet:Text" /></p>
        </xsl:template>
        
        <xsl:template mode="body" match="tet:Table">
        	<table border="1">
        		<tbody><xsl:apply-templates select="tet:Row" mode="body" /></tbody>
        	</table>
        </xsl:template>

        <xsl:template mode="body" match="tet:Row">
        	<tr> <xsl:apply-templates select="tet:Cell" mode="body" /> </tr>
        </xsl:template>
        
        <!-- Process tables also recursively -->
        <xsl:template mode="body" match="tet:Cell">
        	<td>
                        <xsl:if test="@colSpan">
                                <xsl:attribute name="colspan">
                                        <xsl:value-of select="@colSpan" />
                                </xsl:attribute>
                        </xsl:if>
                        <xsl:apply-templates mode="body" select="tet:Para | tet:Table | tet:PlacedImage" />
                </td>
        </xsl:template>
        
        <xsl:template mode="body" match="tet:PlacedImage">
		<xsl:if test="$debug &gt; 0">
			<p>
				<i>
				<xsl:text>[ +++ Image </xsl:text>
				<xsl:variable name="current-image-id" select="@image" />
				<xsl:value-of select="@image"/>
				<xsl:text> </xsl:text>
				<xsl:value-of select="ancestor::tet:Document[1]//tet:Image[@id = $current-image-id]/@width"/>
				<xsl:text>x</xsl:text>
				<xsl:value-of select="ancestor::tet:Document[1]//tet:Image[@id = $current-image-id]/@height"/>
				<xsl:text> +++ ]</xsl:text>
				</i>
			</p>
		</xsl:if>
        </xsl:template>
        
        <xsl:template match="tet:Text">
                <xsl:value-of select="."/>
                <xsl:text> </xsl:text>
        </xsl:template>

</xsl:stylesheet>
