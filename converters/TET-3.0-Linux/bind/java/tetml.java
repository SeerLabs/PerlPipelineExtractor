/**
 * Extract text from PDF document as XML. If an output filename is specified,
 * write the XML to the output file. Otherwise fetch the XML in memory, parse it
 * and print some information to System.out.
 * 
 * @version $Id: tetml.java,v 1.8 2008/12/30 15:22:50 rp Exp $
 */

import java.io.ByteArrayInputStream;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;

import com.pdflib.TET;
import com.pdflib.TETException;

public class tetml
{
    /**
     * Global option list.
     */
    static final String globaloptlist = "searchpath={../data " +
			"../../../resource/cmap}";

    /**
     * Document specific option list.
     */
    static final String basedocoptlist = "";

    /**
     * Page-specific option list.
     */
    static final String pageoptlist = "granularity=word";

    /**
     * Word counter for in-memory processing code.
     */
    int word_count = 0;
    
    /**
     * SAX handler class to count the words in the document.
     */
    private class sax_handler extends DefaultHandler
    {
        public void startElement (String uri, String local_name,
	    String qualified_name, Attributes attributes) throws SAXException
        {
            if (local_name.equals("Word"))
            {
                word_count += 1;
            }
            else if (local_name.equals("Font"))
            {
                System.out.println("Font " + attributes.getValue("", "name")
                        + " (" + attributes.getValue("", "type") + ")");
            }
        }
    }
    
    public static void main(String[] args)
    {
        if (args.length < 1 || args.length > 2)
        {
            System.err.println("usage: tetml <pdffilename> [ <xmlfilename> ]");
            return;
        }

        final boolean inmemory = args.length == 1;
        
        /*
         * For JRE 1.4 the property must be set what XML parser to use, later
         * JREs seem to have a default set internally.
         * It seems to be the case that in 1.4 
         * org.apache.crimson.parser.XMLReaderImpl is always available.
         */
        String jre_version = System.getProperty("java.version");
        if (jre_version.startsWith("1.4")) {
            System.setProperty("org.xml.sax.driver",
                "org.apache.crimson.parser.XMLReaderImpl");
        }

        /*
         * We need a tetml object, otherwise it's not possible to set up the
         * handler for the SAX parser with the local sax_handler class.
         */
        tetml t = new tetml();
        t.process_xml(args, inmemory);
    }

    private void process_xml(String[] args, final boolean inmemory)
    {
        TET tet = null;
        try
        {
            tet = new TET();
            tet.set_option(globaloptlist);

            final String docoptlist =
                (inmemory ? "tetml={}" : "tetml={filename={" + args[1] + "}}")
                    + " " + basedocoptlist;

            if (inmemory)
            {
                System.out.println("Processing TETML output for document \""
                        + args[0] + "\" in memory...");
            }
            else
            {
                System.out.println("Extracting TETML for document \""
                        + args[0] + "\" to file \"" + args[1] + "\"...");
            }
            
            final int doc = tet.open_document(args[0], docoptlist);
            if (doc == -1)
            {
                System.err.println("Error " + tet.get_errnum() + " in "
                        + tet.get_apiname() + "(): " + tet.get_errmsg());
                tet.delete();
                return;
            }

            final int n_pages = (int) tet.pcos_get_number(doc, "length:pages");

            /*
             * Loop over pages in the document;
             */
            for (int pageno = 0; pageno <= n_pages; ++pageno)
            {
                tet.process_page(doc, pageno, pageoptlist);
            }

            /*
             * This could be combined with the last page-related call.
             */
            tet.process_page(doc, 0, "tetml={trailer}");

            if (inmemory)
            {
                /*
                 * Get the XML document as a byte array.
                 */
                final byte[] tetml = tet.get_xml_data(doc, "");

                if (tetml == null)
                {
                    System.err.println("tetml: couldn't retrieve XML data");
                    return;
                }
                
                /*
                 * Process the in-memory XML document to print out some
                 * information that is extracted with the sax_handler class.
                 */

                XMLReader reader = XMLReaderFactory.createXMLReader();
                reader.setContentHandler(new sax_handler());
                reader.parse(new InputSource(new ByteArrayInputStream(tetml)));
                System.out.println("Found " + word_count + " words in document");
            }

            tet.close_document(doc);
        }
        catch (TETException e)
        {
            System.err.println("Error " + e.get_errnum() + " in "
                    + e.get_apiname() + "(): " + e.get_errmsg());
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        finally
        {
            if (tet != null)
            {
                tet.delete();
            }
        }
    }
}
