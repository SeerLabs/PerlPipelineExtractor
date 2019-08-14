import com.pdflib.TET;
import com.pdflib.TETException;

import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;

/**
 * Extract text from PDF and filter according to font name and size. This can be
 * used to identify headings in the document and create a table of contents.
 * 
 * @version $Id: fontfilter.java,v 1.7 2009/01/01 17:51:50 tm Exp $
 */
class fontfilter
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
    static final String pageoptlist = "granularity=line";

    /**
     * Search text with at least this size (use 0 to catch all sizes).
     */
    static final double fontsizetrigger = 10;

    /**
     * Catch text where the font name contains this string (use empty string to
     * catch all font names).
     */
    static final String fontnametrigger = "Bold";

    /**
     * The encoding in which the output is sent to System.out. For running
     * the example in a Windows command window, you can set this for example to
     * "windows-1252" for getting Latin-1 output.
     */
    private static final String OUTPUT_ENCODING = "UTF-8";

    /**
     * For printing to System.out in the encoding specified via OUTPUT_ENCODING.
     */
    private static PrintStream out;


    public static void main(String[] args) throws UnsupportedEncodingException 
    {

        TET tet = null;
        int pageno = 0;

        if (args.length != 1)
        {
            System.out.println("usage: fontfilter <infilename>");
            return;
        }

        try
        {
	    out = new PrintStream(System.out, true, OUTPUT_ENCODING);
            tet = new TET();
            tet.set_option(globaloptlist);

            final int doc = tet.open_document(args[0], docoptlist);
            if (doc == -1)
            {
                System.err.println("Error " + tet.get_errnum() + " in "
                        + tet.get_apiname() + "(): " + tet.get_errmsg());
                return;
            }

            /*
             * Loop over pages in the document
             */
            final int n_pages = (int) tet.pcos_get_number(doc, "length:pages");
            for (pageno = 1; pageno <= n_pages; ++pageno)
            {
                process_page(tet, doc, pageno);
            }

            tet.close_document(doc);
        }
        catch (TETException e)
        {
            if (pageno == 0)
            {
                System.err.println("Error " + e.get_errnum() + " in "
                        + e.get_apiname() + "(): " + e.get_errmsg() + "\n");
            }
            else
            {
                System.err.println("Error " + e.get_errnum() + " in "
                        + e.get_apiname() + "() on page " + pageno + ": "
                        + e.get_errmsg() + "\n");
            }
        }
        finally
        {
            tet.delete();
        }
    }

    /**
     * Process all words on the page and print the words that match the
     * desired font.
     * 
     * @param tet TET object
     * @param doc TET document handle
     * @param pageno Page to process
     * 
     * @throws TETException An error occurred in the TET API
     */
    private static void process_page(TET tet, final int doc, int pageno)
            throws TETException
    {
        final int page = tet.open_page(doc, pageno, pageoptlist);

        if (page == -1)
        {
            System.err.println("Error " + tet.get_errnum() + " in "
                    + tet.get_apiname() + "(): " + tet.get_errmsg());
            return; /* try next page */
        }

        /* Retrieve all text fragments for the page */
        for (String text = tet.get_text(page); text != null;
                text = tet.get_text(page))
        {
            /* Loop over all characters */
            for (int ci = tet.get_char_info(page); ci != -1;
                    ci = tet.get_char_info(page))
            {
                /*
                 * We need only the font name and size; the text
                 * position could be fetched from tet.x and tet.y.
                 */
                final String fontname = tet.pcos_get_string(doc,
                        "fonts[" + tet.fontid + "]/name");

                /* Check whether we found a match */
                if (tet.fontsize >= fontsizetrigger
                        && fontname.indexOf(fontnametrigger) != -1)
                {
                    /* print the retrieved font name, size, and text */
                    BigDecimal roundedValue =
                        (new BigDecimal(tet.fontsize)).
                            setScale(2, BigDecimal.ROUND_HALF_UP);
                    out.println("[" + fontname + " "
                            + roundedValue.toString() + "] " + text);
                }

                /*
                 * In this sample we check only the first character of
                 * each fragment.
                 */
                break;
            }
        }

        if (tet.get_errnum() != 0)
        {
            System.err.println("Error " + tet.get_errnum() + " in "
                    + tet.get_apiname() + "(): " + tet.get_errmsg());
        }

        tet.close_page(page);
    }
}
