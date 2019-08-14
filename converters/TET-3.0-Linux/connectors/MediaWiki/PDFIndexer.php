<?php
/*
 * (C) PDFlib GmbH 2008 www.pdflib.com
 *
 * This code is based on the MediaWiki extension which can be found at
 * http://www.mediawiki.org/wiki/Manual:Uploaded_document_searching
 *
 * This module requires PDFlib TET (Text Extraction Toolkit) installed
 * in PHP. It is available at http://www.pdflib.com/download/tet
 *
 * $Id: PDFIndexer.php,v 1.10 2009/01/28 10:25:43 stm Exp $
 *
 */

$wgHooks['ArticleSave'][] = 'tetSaveFileIndexer';
$wgHooks['UploadComplete'][] = 'tetUploadCompleteFileIndexer';

$tetNewFileIndex = false;


function tetUploadCompleteFileIndexer(&$image){
    global $tetNewFileIndex, $wgDebugLogFile;

    $tooLarge = 524828; # 512K                                                                      
    // retrieve the file name from the uploaded extension
    $file = $image->mLocalFile->getPath();

    // extract the extension of the destination filename
    $extension = substr(strrchr($file, '.'),1);
    if (strtolower($extension)== "pdf"){
        try {
            /* place extracted text as plaintext to the description
             */

            $globaloptlist = "searchpath={" . dirname( __FILE__ ) . "/resource/cmap} ";

            /* document-specific option list
             *
             * Indexing of password-protected documents that disallow text
             * extraction is possible by using the "shrug" option of
             * TET_open_document(). Please read the relevant section in the
             * PDFlib Terms and Conditions and the TET Manual about the
             * "shrug" option to understand the implications of using this
             * feature.
             *
             * $docoptlist = "shrug";
             */
            $docoptlist = "";

            /* page-specific option list */
            $pageoptlist = "granularity=page";

            $tet = new TET();

            $tet->set_option($globaloptlist);

            $file = $image->mLocalFile->getPath();

            $doc = $tet->open_document($file, $docoptlist);

            if ($doc == -1)
            {
                wfErrorLog( "PDFIndexer: Error ". $tet->get_errnum() . " in " . $tet->get_apiname() . "(): " . $tet->get_errmsg() . "\n", $wgDebugLogFile );
                return true;
            }

            /* add document Info to the Description field */
            $count = $tet->pcos_get_number($doc, "length:/Info");
    
            for ($i=0; $i < $count; $i++) {
                $objtype = $tet->pcos_get_string($doc, "type:/Info[$i]");

                /* Info entries can be stored as string or name objects */
                if ($objtype == "string" || $objtype == "name") {
                $NewDesc .= sprintf("%12s: %10s\n", 
                    $tet->pcos_get_string($doc, "/Info[$i].key"),
                    $tet->pcos_get_string($doc, "/Info[$i]"));
                }
            }

            /* get number of pages in the document */
            $n_pages = $tet->pcos_get_number($doc, "length:pages");

            for ($pageno = 1; $pageno <= $n_pages; ++$pageno) /* loop over pages */
            {
                /* reset time limit */
                set_time_limit(60);
                $page = $tet->open_page($doc, $pageno, $pageoptlist);

                if ($page == -1)
                {
                    continue;                        /* try next page */
                }

                $text = $tet->get_text($page);
                $text =  str_replace("-->","",$text);
                $tetNewFileIndex .= $text;


                $tet->close_page($page);
            }

            $tet->close_document($doc);

            $tet->delete();
	    }
	    catch (TETException $e) {
            wfErrorLog( "PDFIndexer: TET exception occurred in extractor sample:\n". $tet->get_errnum() . " in " . $tet->get_apiname() . "(): " . $tet->get_errmsg() . "\n", $wgDebugLogFile );
            return true;
	    }
	    catch (Exception $e) {
            wfErrorLog("PDFIndexer: " . $e . "\n",  $wgDebugLogFile);
            return true;
	    }
    }

    /* check if extracted text is too long. 
       If so, only store single words and truncate the word list 
       if necessary. Use 512kb text buffer */
    if (strlen($tetNewFileIndex)>$tooLarge){
        $tetNewFileIndex =  
            implode(" ", array_unique(explode(" ", $tetNewFileIndex)));
        wfErrorLog("PDFIndexer: text length reached limit => build word list\n",  $wgDebugLogFile);
        if (strlen($tetNewFileIndex)>$tooLarge){
            $tetNewFileIndex = substr($tetNewFileIndex,0,$tooLarge);
            wfErrorLog("PDFIndexer: word list reached limit => truncate it\n",  $wgDebugLogFile);
        }
    }

    // add comments around the text
    $tetNewFileIndex = "<!-- \r\n" . $tetNewFileIndex . "\r\n//-->";

	if($tetNewFileIndex !== false){
		$article = new Article( $image->mLocalFile->getTitle() );
		$article->loadContent();
		$article->doEdit($article->mContent, "PDFIndexer: PDF uploaded.\n");
	}

	return true;
}

function tetSaveFileIndexer(&$article, &$user, &$text, &$summary, $minor, $watch, $sectionanchor, &$flags){
	global $tetNewFileIndex;

	if ($tetNewFileIndex !== false ){
	    /* store word list in $text */
	    $text = $tetNewFileIndex;
	}
	$tetNewFileIndex = false;
	return true;
}



/**
  * Add extension information to Special:Version
  */
$wgExtensionCredits['other'][] = array(
        'name' => 'PDFIndexer',
        'author' => 'Rainer Plöckl',
        'version' => '$Id: PDFIndexer.php,v 1.10 2009/01/28 10:25:43 stm Exp $',
        'description' => 'extract PDF text content with PDFlib TET and make it searchable in the Wiki.<br/><b>This extension requires MediaWiki 1.11.2 or newer. For older versions please see the PDFlib TET manual for instructions.</b>',
        'url' => 'http://www.pdflib.com/download/tet/'
        );
?>
