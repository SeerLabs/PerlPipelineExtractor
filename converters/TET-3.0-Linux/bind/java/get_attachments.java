/* 
 * PDF text extractor which also searches PDF file attachments.
 *
 * $Id: get_attachments.java,v 1.7 2008/12/30 15:22:50 rp Exp $
 */

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;

import com.pdflib.TET;
import com.pdflib.TETException;

public class get_attachments
{
    /**
     * Global option list.
     */
    static final String globaloptlist = "searchpath={../data " +
			"../../../resource/cmap}";

    /**
     * Document specific option list.
     */
    static final String docoptlist = "";

    /**
     * Page-specific option list.
     */
    static final String pageoptlist = "granularity=page";

    /**
     * Separator to emit after each chunk of text. This depends on the
     * application's needs; for granularity=word a space character may be
     * useful.
     */
    static final String separator = "\n";

    /**
     * Extract text from a document for which a TET handle is already available.
     * 
     * @param tet
     *            The TET object
     * @param doc
     *            A valid TET document handle
     * @param outfp
     *            Output file handle
     * 
     * @throws TETException
     * @throws IOException
     */
    static void extract_text(TET tet, int doc, Writer outfp)
            throws TETException, IOException
    {
        /*
         * Get number of pages in the document.
         */
        int n_pages = (int) tet.pcos_get_number(doc, "length:pages");

        /* loop over pages */
        for (int pageno = 1; pageno <= n_pages; ++pageno)
        {
            String text;
            int page;

            page = tet.open_page(doc, pageno, pageoptlist);

            if (page == -1)
            {
                System.err.println("Error " + tet.get_errnum() + " in  "
                        + tet.get_apiname() + "() on page " + pageno + ": "
                        + tet.get_errmsg());
                continue; /* try next page */
            }

            /*
             * Retrieve all text fragments; This loop is actually not required
             * for granularity=page, but must be used for other granularities.
             */
            while ((text = tet.get_text(page)) != null)
            {
                outfp.write(text); // print the retrieved text

                /* print a separator between chunks of text */
                outfp.write(separator);
            }

            if (tet.get_errnum() != 0)
            {
                System.err.println("Error " + tet.get_errnum() + " in  "
                        + tet.get_apiname() + "() on page " + pageno + ": "
                        + tet.get_errmsg());
            }

            tet.close_page(page);
        }
    }

    /**
     * Open a named physical or virtual file, extract the text from it, search
     * for document or page attachments, and process these recursively. Either
     * filename must be supplied for physical files, or data+length from which a
     * virtual file will be created. The caller cannot create the PVF file since
     * we create a new TET object here in case an exception happens with the
     * embedded document - the caller can happily continue with his TET object
     * even in case of an exception here.
     * 
     * @param outfp
     * @param filename
     * @param realname
     * @param data
     * 
     * @return 0 if successful, otherwise a non-null code to be used as exit
     *         status
     */
    static int process_document(Writer outfp, String filename, String realname,
            byte[] data)
    {
        int retval = 0;
        TET tet = null;
        try
        {
            final String pvfname = "/pvf/attachment";

            tet = new TET();

            /*
             * Construct a PVF file if data instead of a filename was provided
             */
            if (filename == null || filename.length() == 0)
            {
                tet.create_pvf(pvfname, data, "");
                filename = pvfname;
            }

            tet.set_option(globaloptlist);

            int doc = tet.open_document(filename, docoptlist);

            if (doc == -1)

            {
                System.err.println("Error " + tet.get_errnum() + " in  "
                        + tet.get_apiname() + "() (source: attachment '"
                        + realname + "'): " + tet.get_errmsg());

                retval = 5;
            }
            else
            {
                process_document(outfp, tet, doc);
            }

            /*
             * If there was no PVF file deleting it won't do any harm
             */
            tet.delete_pvf(pvfname);
        }
        catch (TETException e)
        {
            System.err.println("Error " + e.get_errnum() + " in  "
                    + e.get_apiname() + "() (source: attachment '" + realname
                    + "'): " + e.get_errmsg());
            retval = 1;
        }
        catch (IOException e)
        {
            e.printStackTrace();
            retval = 1;
        }
        finally
        {
            if (tet != null)
            {
                tet.delete();
            }
        }

        return retval;
    }

    /**
     * Process a single file.
     * 
     * @param outfp Output stream for messages
     * @param tet The TET object
     * @param doc The TET document handle
     * 
     * @throws TETException
     * @throws IOException
     */
    private static void process_document(Writer outfp, TET tet, int doc)
            throws TETException, IOException
    {
        String objtype;

        // -------------------- Extract the document's own page contents
        extract_text(tet, doc, outfp);

        // -------------------- Process all document-level file attachments

        // Get the number of document-level file attachments.
        int filecount = (int) tet.pcos_get_number(doc,
                "length:names/EmbeddedFiles");

        for (int file = 0; file < filecount; file++)
        {
            String attname;

            /*
             * fetch the name of the file attachment; check for Unicode file
             * name (a PDF 1.7 feature)
             */
            objtype = tet.pcos_get_string(doc, "type:names/EmbeddedFiles["
                + file + "]/UF");

            if (objtype.equals("string"))
            {
                attname = tet.pcos_get_string(doc,
                    "names/EmbeddedFiles[" + file + "]/UF");
            }
            else
            {
                objtype = tet.pcos_get_string(doc, "type:names/EmbeddedFiles["
                        + file + "]/F");
    
                if (objtype.equals("string"))
                {
                    attname = tet.pcos_get_string(doc, "names/EmbeddedFiles["
                            + file + "]/F");
                }
                else
                {
                    attname = "(unnamed)";
                }
            }
            /* fetch the contents of the file attachment and process it */
            objtype = tet.pcos_get_string(doc, "type:names/EmbeddedFiles["
                    + file + "]/EF/F");

            if (objtype.equals("stream"))
            {
                outfp.write("----- File attachment '" + attname + "':\n");
                byte attdata[] = tet.pcos_get_stream(doc, "",
                        "names/EmbeddedFiles[" + file + "]/EF/F");

                process_document(outfp, null, attname, attdata);
                outfp.write("----- End file attachment '" + attname + "'\n");
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
                String attname;

                val = tet.pcos_get_string(doc, "pages[" + page + "]/Annots["
                        + annot + "]/Subtype");

                attname = "page " + (page + 1) + ", annotation " + (annot + 1);
                if (val.equals("FileAttachment"))
                {
                    String attpath = "pages[" + page
                            + "]/Annots[" + annot + "]/FS/EF/F";
                    /*
                     * fetch the contents of the attachment and process it
                     */
                    objtype = tet.pcos_get_string(doc, "type:" + attpath);

                    if (objtype.equals("stream"))
                    {
                        outfp.write("----- Page level attachment '" + attname + "':\n");
                        byte attdata[] = tet.pcos_get_stream(doc, "", attpath);
                        process_document(outfp, null, attname, attdata);
                        outfp.write("----- End page level attachment '" + attname + "'\n");
                    }
                }
            }
        }

        tet.close_document(doc);
    }

    public static void main(String[] args)
    {
        int ret = 0;

        if (args.length != 2)
        {
            System.err
                    .println("usage: get_attachments <infilename> <outfilename>");
            System.exit(2);
        }

        Writer outfp;
        try
        {
            outfp = new BufferedWriter(new OutputStreamWriter(
                    new FileOutputStream(args[1]), "UTF-8"));

            ret = process_document(outfp, args[0], args[0], null);

            outfp.close();
        }
        catch (Exception e)
        {
            e.printStackTrace();
            ret = 1;
        }

        System.exit(ret);
    }
}
