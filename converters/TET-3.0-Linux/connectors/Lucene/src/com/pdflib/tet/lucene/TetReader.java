package com.pdflib.tet.lucene;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import com.pdflib.TET;
import com.pdflib.TETException;

/**
 * (C) PDFlib GmbH 2008 www.pdflib.com
 * <p>
 * java.io.Reader implementation for reading plain text from a PDF document by
 * using the TET Java binding. This code has been tested with Lucene Java 2.4.0.
 * <p>
 * The TetReader recursively creates TetReader objects for PDF attachments.
 * 
 * @version $Id: TetReader.java,v 1.3 2009/01/28 10:25:43 stm Exp $
 */
public class TetReader extends Reader
{
    static final String SEARCH_PATH =
        "searchpath={"
        + System.getProperty("tet.searchpath", "../../resource/cmap ../../resource")
        + "}";
    
    /**
     * Global option list. This searchpath is intended for the TET Lucene
     * demonstration environment, which is intended to be executed under
     * the working directory "<TET install dir>/bind/lucene", with the
     * resources residing under "<TET install dir>/resource". Other options
     * can be appended.
     */
    static final String GLOBAL_OPT_LIST = SEARCH_PATH + "";
    
    /**
     * The options for the TET open_document function.
     * 
     * Indexing of password-protected documents that disallow text extraction is
     * possible by using the "shrug" option of TET_open_document(). Please read
     * the relevant section in the PDFlib Terms and Conditions and the TET
     * Manual about the "shrug" option to understand the implications of
     * using this feature.
     * 
     * private static final String DOC_OPT_LIST = "shrug";
     */
    private static final String DOC_OPT_LIST = "";
    
    /**
     * Page-specific option list. Note that "granularity=page" is hard-wired in
     * the invocation of the TET open_document function. Image extraction is
     * not needed for Lucene, so we suppress it here.
     */
    static final String PAGE_OPT_LIST = "skipengines={image}";
    
    /**
     * The TET object.
     */
    private TET tet;

    /**
     * A buffer for fetching the text from TET.
     */
    private char[] extractedText;

    /**
     * Current position in the buffer.
     */
    private int pos;

    /**
     * The TET handle for the document.
     */
    private int doc;

    /**
     * The current page number.
     */
    private int pageno;

    /**
     * The total number of pages in the document.
     */
    private int pages;

    /**
     * A list of Attachment instances that are queued for extraction.
     */
    private List attachments;
    
    /**
     * Iterator for the attachments list.
     */
    private Iterator attachmentIterator;
    
    /**
     * Reader for current attachment. Currently always is a TetReader. This
     * could be extended with readers for attachments in formats other than PDF.
     */
    private Reader attachmentReader;

    /**
     * False if reading the document itself, true if reading from one
     * of the attachments. 
     */
    private boolean readingAttachments;
    
    /**
     * Nesting-level for extracting attachments.
     */
    private int attachmentLevel = 0;
    
    /**
     * Maximum number of accepted levels for nested attachments.
     */
    private static final int MAX_NESTING_LEVEL = 10;
    
    /**
     * Helper routine for indenting with spaces.
     */
    private String getIndentation()
    {
        StringBuffer s = new StringBuffer();
        for (int i = 0; i < attachmentLevel; i += 1)
        {
            s.append(' ');
        }
        return s.toString();
    }
    
    /**
     * Helper class for storing information about a single attachment.
     */
    private class Attachment
    {
        public Attachment(String attName, String dataPath)
        {
            name = attName;
            pCosStreamPath = dataPath;
        }

        public Reader getReader() throws IOException
        {
            Reader newReader = null;
            
            try
            {
                byte[] attdata = tet.pcos_get_stream(doc, "",
                        pCosStreamPath);
                newReader = new TetReader(attdata, attachmentLevel + 1);
                System.out.println(getIndentation() + " adding attachment \""
                        + name + "\"");
            }
            catch (TETException e)
            {
                /*
                 * It is not a problem per se if an attachment cannot be
                 * opened, as this exception will occur with all attachments
                 * that are not PDF documents, but for the demo this is
                 * reported as an exception.
                 */
                throw new IOException(getIndentation()
                        + " Warning: unable to open attachment \""
                        + name + "\": " + e.toString());
            }
            
            return newReader;
        }
        
        /**
         * The stream containing the attachment data.
         */
        public String pCosStreamPath;
        
        /**
         * The name of the attachment.
         */
        public String name;
    }
    
    /**
     * Create a TetReader for a PDF file on disk.
     * 
     * @param f
     *            the PDF file
     * 
     * @throws TETException
     */
    public TetReader(File f) throws TETException
    {
        tet = new TET();

        setupReader(f.getAbsolutePath());
    }

    /**
     * Create a TetReader from a PDF document in memory.
     * 
     * @param f
     *            the contents of the PDF file
     * 
     * @throws TETException
     */
    public TetReader(byte[] pdf) throws TETException
    {
        tet = new TET();
        
        /*
         * Read the data into a TET PVF (private virtual file).
         */
        String pfvNname = "/pvf/memfile";
        tet.create_pvf(pfvNname, pdf, "");
        
        setupReader(pfvNname);
    }
    
    /**
     * Private constructor for extracting attachments, with a nesting level.
     * 
     * @param pdf
     * @param level
     * @throws TETException 
     */
    private TetReader(byte[] pdf, int level) throws TETException
    {
        attachmentLevel = level;
        if (attachmentLevel > MAX_NESTING_LEVEL)
        {
            throw new TETException(
                    getIndentation()
                    + "Exceeded maximum depth for nested attachments ("
                    + MAX_NESTING_LEVEL + ")");
        }
        
        tet = new TET();
        
        /*
         * Read the data into a TET PVF (private virtual file).
         */
        String pfvNname = "/pvf/memfile";
        tet.create_pvf(pfvNname, pdf, "");
        
        setupReader(pfvNname);        
    }

    /**
     * Common method for the constructors to initialize the reader.
     * 
     * @param filename
     *            the filename of the PDF document, can be a PVF filename
     * 
     * @throws TETException
     */
    private void setupReader(String filename) throws TETException
    {
        tet.set_option(GLOBAL_OPT_LIST);
        doc = tet.open_document(filename, DOC_OPT_LIST);
        if (doc == -1)
        {
            throw new TETException(tet.get_apiname() + ": " + tet.get_errnum()
                    + " " + tet.get_errmsg());
        }
        pages = (int) tet.pcos_get_number(doc, "length:pages");
        extractedText = null;
        pageno = 1;
        attachments = new LinkedList();
        prepareAttachments();
        readingAttachments = false;
        attachmentIterator = attachments.iterator();
    }
    
    /**
     * Collect information about all file-level and page-level attachments.
     * 
     * @throws TETException 
     */
    private void prepareAttachments() throws TETException
    {
        String objtype;
        int filecount = (int) tet.pcos_get_number(doc,
                "length:names/EmbeddedFiles");

        for (int file = 0; file < filecount; file++)
        {
            String attName;

            /*
             * fetch the name of the file attachment; check for Unicode file
             * name (a PDF 1.7 feature)
             */
            objtype = tet.pcos_get_string(doc,
                    "type:names/EmbeddedFiles[" + file + "]/UF");

            if (objtype.equals("string"))
            {
                attName = tet.pcos_get_string(doc, "names/EmbeddedFiles["
                        + file + "]/UF");
            }
            else
            {
                objtype = tet.pcos_get_string(doc, "type:names/EmbeddedFiles["
                        + file + "]/F");

                if (objtype.equals("string"))
                {
                    attName = tet.pcos_get_string(doc, "names/EmbeddedFiles["
                            + file + "]/F");
                }
                else
                {
                    attName = "(unnamed)";
                }
            }

            /* fetch the contents of the file attachment and process it */
            String dataPath = "names/EmbeddedFiles[" + file + "]/EF/F";
            objtype = tet.pcos_get_string(doc, "type:" + dataPath);

            if (objtype.equals("stream"))
            {
                attachments.add(new Attachment(attName, dataPath));
            }
        }

        // -------------------- Process all page-level file attachments

        int pagecount = (int) tet.pcos_get_number(doc, "length:pages");

        // Check all pages for annotations of type FileAttachment
        for (int page = 0; page < pagecount; page++)
        {
            int annotcount = (int) tet.pcos_get_number(doc, "length:pages["
                    + page + "]/Annots");

            for (int annot = 0; annot < annotcount; annot++)
            {
                String val;
                String attName;

                val = tet.pcos_get_string(doc, "pages[" + page + "]/Annots["
                        + annot + "]/Subtype");

                attName = "page " + (page + 1) + ", annotation " + (annot + 1);
                if (val.equals("FileAttachment"))
                {
                    /*
                     * fetch the contents of the attachment and process it
                     */
                    String dataPath = "pages[" + page
                            + "]/Annots[" + annot + "]/FS/EF/F";
                    objtype = tet.pcos_get_string(doc, "type:" + dataPath);

                    if (objtype.equals("stream"))
                    {
                        attachments.add(new Attachment(attName, dataPath));
                    }
                }
            }
        }
    }

    public void close() throws IOException
    {
        try
        {
            tet.close_document(doc);
        }
        catch (TETException e)
        {
            tet.delete();
            throw new IOException(e.get_errmsg());
        }
        tet.delete();
        tet = null;
    }

    public int read(char[] cbuf, int off, int len) throws IOException
    {
        int retval = -1;
        
        if (!readingAttachments) {
            retval = readFromDocument(cbuf, off, len);
            if (retval == -1) {
                readingAttachments = true;
            }
        }
        if (readingAttachments) {
            retval = readFromAttachments(cbuf, off, len);
        }
        
        return retval;
    }

    private int readFromAttachments(char[] cbuf, int off, int len)
            throws IOException
    {
        int retval = -1;

        /*
         * If there is no current attachment reader, set one up, or read from
         * the current attachment reader. If it is exhausted, proceed to the
         * next attachment.
         */
        if (attachmentReader == null)
        {
            retval = openNextAttachment(cbuf, off);
        }
        else
        {
            retval = attachmentReader.read(cbuf, off, len);
            if (retval == -1)
            {
                attachmentReader.close();
                attachmentReader = null;
                retval = openNextAttachment(cbuf, off);
            }
        }

        return retval;
    }

    /**
     * Open the next attachment from the list if available, and put a single
     * blank into the output buffer to separate the contents of the next
     * attachment from the preceding text.
     * 
     * @param cbuf
     *            The output buffer
     * @param off
     *            The offset where to write the single blank if there is another
     *            attachment
     * @throws IOException
     *             A TETException has occurred.
     */
    private int openNextAttachment(char[] cbuf, int off) throws IOException
    {
        int retval = -1;
        
        while (retval == -1 && attachmentIterator.hasNext()) {
            Attachment a = (Attachment) attachmentIterator.next();
            try
            {
                attachmentReader = a.getReader();
                cbuf[off] = ' ';
                retval = 1;
            }
            catch (IOException e)
            {
                System.err.println(e.getMessage());
            }
        }
        
        return retval;
    }

    /**
     * Read from the current document.
     * 
     * @param cbuf
     * @param off
     * @param len
     * @return
     * @throws IOException
     */
    private int readFromDocument(char[] cbuf, int off, int len)
            throws IOException
    {
        // Fill buffer if it is exhausted
        if (extractedText == null || pos >= extractedText.length)
        {
            if (pageno <= pages)
            {
                String text;
                try
                {
                    /*
                     * This algorithm relies on getting the text page-wise, so
                     * any granularity setting in PAGE_OPT_LIST is overridden
                     * here with "granularity=page".
                     */
                    int page = tet.open_page(doc, pageno,
                	    		PAGE_OPT_LIST + " granularity=page");
                    text = tet.get_text(page);
                    tet.close_page(page);
                }
                catch (TETException e)
                {
                    throw new IOException(e.toString());
                }

                extractedText = text == null ? null : text.toCharArray();
                pos = 0;
                pageno += 1;
            }
        }

        // Copy from internal buffer to Lucene buffer
        int retval = -1;
        if (extractedText != null && pos <= extractedText.length)
        {
            int remainingChars = extractedText.length - pos;
            retval = Math.min(remainingChars, len);
            System.arraycopy(extractedText, pos, cbuf, off, retval);
            pos += retval;
        }

        // Clear buffer if it is exhausted
        if (extractedText != null && pos >= extractedText.length)
        {
            extractedText = null;
        }

        return retval;
    }

    /**
     * @return the TET object
     */
    TET getTet()
    {
        return tet;
    }

    /**
     * @return the TET document handle
     */
    int getDoc()
    {
        return doc;
    }
}
