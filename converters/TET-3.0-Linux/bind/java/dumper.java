import java.io.UnsupportedEncodingException;

import com.pdflib.TET;
import com.pdflib.TETException;

/* TET sample application for dumping PDF information with pCOS
 *
 * $Id: dumper.java,v 1.7 2008/12/15 20:26:53 rjs Exp $
 */

class dumper
{
    public static void main(String args[])
    {
        int exitstat = 0;

        if (args.length != 1)
        {
            System.err.println("usage: dumper <filename>");
            exitstat = 2;
        }
        else
        {
            TET tet = null;
            try
            {
                tet = new TET();
                String docoptlist = "requiredmode=minimum";
                String globaloptlist = "";

		/* This is where input files live. Adjust as necessary. */
		String searchpath = "../data";
		String  optlist;

                tet.set_option(globaloptlist);

		optlist = "searchpath={" + searchpath + "}";
		tet.set_option(optlist);


                int doc = tet.open_document(args[0], docoptlist);
                if (doc == -1)
                {
                    System.out.println("ERROR: " + tet.get_errmsg());
                }
                else
                {
                    print_infos(tet, doc);
                    tet.close_document(doc);
                }
            }
            catch (TETException e)
            {
                System.err.println("Error " + e.get_errnum() + " in "
                        + e.get_apiname() + "(): " + e.get_errmsg());
                exitstat = 1;
            }
            finally
            {
                if (tet != null)
                {
                    tet.delete();
                }
            }
        }
        
        System.exit(exitstat);
    }

    /**
     * Print infos about the document.
     * 
     * @param tet The TET object
     * @param doc The TET document handle
     * 
     * @throws TETException
     */
    private static void print_infos(TET tet, int doc) throws TETException
    {
        /* --------- general information (always available) */
        int pcosmode = (int) tet.pcos_get_number(doc, "pcosmode");

        System.out.println("   File name: "
                + tet.pcos_get_string(doc, "filename"));

        System.out.println(" PDF version: "
                + tet.pcos_get_string(doc, "pdfversionstring"));

        System.out.println("  Encryption: "
                + tet.pcos_get_string(doc, "encrypt/description"));

        System.out.println("   Master pw: "
                + (tet.pcos_get_number(doc, "encrypt/master") != 0 ? "yes" : "no"));

        System.out.println("     User pw: "
                + (tet.pcos_get_number(doc, "encrypt/user") != 0 ? "yes" : "no"));

        System.out.println("Text copying: "
                + (tet.pcos_get_number(doc, "encrypt/nocopy") != 0 ? "no" : "yes"));

        System.out.println("  Linearized: "
                + (tet.pcos_get_number(doc, "linearized") != 0 ? "yes" : "no"));

        if (pcosmode == 0)
        {
            System.out.println("Minimum mode: no more information available\n\n");
        }
        else
        {
            print_userpassword_infos(tet, doc, pcosmode);
        }
    }

    /**
     * Print infos that require at least the user password.
     * 
     * @param tet The tet object
     * @param doc The tet document handle
     * @param pcosmode The pCOS mode for the document
     * 
     * @throws TETException
     */
    private static void print_userpassword_infos(TET tet, int doc, int pcosmode)
            throws TETException
    {
        System.out.println("PDF/X status: " + tet.pcos_get_string(doc, "pdfx"));

        System.out.println("PDF/A status: " + tet.pcos_get_string(doc, "pdfa"));

        System.out.println("  Tagged PDF: "
                + (tet.pcos_get_number(doc, "tagged") != 0 ? "yes" : "no"));
        System.out.println();

        System.out.println("No. of pages: "
                + (int) tet.pcos_get_number(doc, "length:pages"));

        System.out.println(" Page 1 size: width="
                + (int) tet.pcos_get_number(doc, "pages[0]/width") + ", height="
                + (int) tet.pcos_get_number(doc, "pages[0]/height"));

        int count = (int) tet.pcos_get_number(doc, "length:fonts");
        System.out.println("No. of fonts: " + count);

        for (int i = 0; i < count; i++)
        {
            if (tet.pcos_get_number(doc, "fonts[" + i + "]/embedded") != 0)
                System.out.print("embedded ");
            else
                System.out.print("unembedded ");

            System.out.print(tet
                    .pcos_get_string(doc, "fonts[" + i + "]/type")
                    + " font ");
            System.out.println(tet
                    .pcos_get_string(doc, "fonts[" + i + "]/name"));
        }

        System.out.println();

        boolean plainmetadata =
            tet.pcos_get_number(doc, "encrypt/plainmetadata") != 0;

        if (pcosmode == 1 && !plainmetadata
                && tet.pcos_get_number(doc, "encrypt/nocopy") != 0)
        {
            System.out
                    .println("Restricted mode: no more information available");
        }
        else
        {
            print_masterpassword_infos(tet, doc);
        }
    }

    /**
     * Print document info keys and XMP metadata (requires master pw or
     * plaintext metadata).
     * 
     * @param tet
     * @param doc
     * @throws TETException
     */
    private static void print_masterpassword_infos(TET tet, int doc)
            throws TETException
    {
        String objtype;
        int count = (int) tet.pcos_get_number(doc, "length:/Info");

        for (int i = 0; i < count; i++)
        {
            objtype = tet.pcos_get_string(doc, "type:/Info[" + i + "]");
            String key = tet.pcos_get_string(doc, "/Info[" + i + "].key");

	    int len = 12 - key.length();
	    while (len-- > 0) System.out.print(" ");
            System.out.print(key + ": ");

            /*
             * Info entries can be stored as string or name objects
             */
            if (objtype.equals("string") || objtype.equals("name"))
            {
                System.out.println("'"
                        + tet.pcos_get_string(doc, "/Info[" + i + "]") + "'");
            }
            else
            {
                System.out.println("("
                        + tet.pcos_get_string(doc, "type:/Info[" + i + "]")
                        + "object)");
            }
        }

        System.out.println();
        System.out.print("XMP meta data: ");

        objtype = tet.pcos_get_string(doc, "type:/Root/Metadata");
        if (objtype.equals("stream"))
        {
            byte contents[] = tet.pcos_get_stream(doc, "", "/Root/Metadata");
            System.out.print(contents.length + " bytes ");

            try
            {
                String string = new String(contents, "UTF-8");
                System.out.println("(" + string.length()
                        + " Unicode characters)\n");
            }
            catch (UnsupportedEncodingException e)
            {
                System.err.println("Internal error: wrong encoding specified");
            }
        }
        else
        {
            System.out.println("not present\n\n");
        }
    }
}
