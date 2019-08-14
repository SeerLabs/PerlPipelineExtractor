/* PDF text extractor based on PDFlib TET
 *
 * $Id: extractor.java,v 1.28 2009/01/21 19:40:28 rjs Exp $
 */

import java.io.*;
import com.pdflib.TETException;
import com.pdflib.TET;
import java.text.DecimalFormat;

public class extractor
{
    /**
     * Global option list
     */
    static final String globaloptlist = "searchpath={../data " +
			"../../../resource/cmap}";
    
    /**
     * Document-specific option list
     */
    static final String docoptlist = "";
    
    /**
     * Page-specific option list
     */
    static final String pageoptlist = "granularity=page";
    
    /**
     * Separator to emit after each chunk of text. This depends on the
     * applications needs; for granularity=word a space character may be useful.
     */
    static final String separator = "\n";

    /**
     * Basic image extract options (more below)
     */
    static final String baseimageoptlist = "compression=auto format=auto";

    /**
     * Set inmemory to true to generate the image in memory.
     */
    static final boolean inmemory = false;
    
    public static void main (String argv[])
    {
        TET tet = null;
        
	try
        {

	    if (argv.length < 1 || argv.length > 2)
            {
                throw new Exception(
		    "usage: extractor <filename> [<outfilename>]");
            }

	    String outfilebase = argv.length == 1 ? argv[0] : argv[1];
            /*
             * The name for the text output file is generated from the
             * name of the input file by appending ".txt".
             */
            Writer outfp = new BufferedWriter(new OutputStreamWriter(
                    new FileOutputStream(outfilebase + ".txt"), "UTF-8"));

            tet = new TET();

            tet.set_option(globaloptlist);

            int doc = tet.open_document(argv[0], docoptlist);

            if (doc == -1)
            {
                throw new Exception("Error " + tet.get_errnum() + "in "
                        + tet.get_apiname() + "(): " + tet.get_errmsg());
            }
            
            /* get number of pages in the document */
            int n_pages = (int) tet.pcos_get_number(doc, "length:pages");

            /* loop over pages in the document */
            for (int pageno = 1; pageno <= n_pages; ++pageno)
            {
                String text;
                int page;
                int imageno = -1;
		
		page = tet.open_page(doc, pageno, pageoptlist);

                if (page == -1)
                {
                    print_tet_error(tet, pageno);
                    continue; /* try next page */
                }

                /*
                 * Retrieve all text fragments; This is actually not required
                 * for granularity=page, but must be used for other
                 * granularities.
                 */
                while ((text = tet.get_text(page)) != null)
                {
                    /* print the retrieved text */
                    outfp.write(text);

                    /* print a separator between chunks of text */
                    outfp.write(separator);
                }

                /* Retrieve all images on the page */
                while (tet.get_image_info(page) == 1)
                {
		    int width, height, bpc, cs;

		    imageno++;

		    /* Print the following information for each image:
		     * - page and image number
		     * - pCOS id (required for indexing the images[] array)
		     * - physical size of the placed image on the page
		     * - pixel size of the underlying PDF image
		     * - number of components, bits per component,and colorspace
		     */
		    width = (int) tet.pcos_get_number(doc,
				"images[" + tet.imageid + "]/Width");
		    height = (int) tet.pcos_get_number(doc,
				"images[" + tet.imageid + "]/Height");
		    bpc = (int) tet.pcos_get_number(doc,
				"images[" + tet.imageid + "]/bpc");
		    cs = (int) tet.pcos_get_number(doc,
				"images[" + tet.imageid + "]/colorspaceid");

		    DecimalFormat df = new DecimalFormat();
		    df.setMinimumFractionDigits(2);
		    df.setMaximumFractionDigits(2);

		    System.out.print( "page " + pageno + ", image " 
			    + imageno + ": id=" + tet.imageid  
			    + ", " + df.format(tet.width)
			    + "x" + df.format(tet.height)
			    + " point, ");
			    

		    System.out.print(width + "x" + height + " pixel, ");
		    
		    if (cs != -1)
		    {
			System.out.println(
			    (int) tet.pcos_get_number(doc, "colorspaces[" 
			    + cs + "]/components") + "x" + bpc + " bit " +
			    tet.pcos_get_string(doc, "colorspaces[" + cs 
			    + "]/name"));
		    }
		    else {
			/* cs==-1 may happen for some JPEG 2000 images. bpc,
			 * colorspace name and number of components are not
			 * available in this case.
			 */
			System.out.println("JPEG2000");
		    }

                    if (inmemory)
                    {
                        /* Fetch the image data and store it in memory */
                        byte imagedata[] = tet.get_image_data(doc, tet.imageid,
                                baseimageoptlist);

                        if (imagedata == null)
                        {
                            print_tet_error(tet, pageno);
                            continue; /* process next image */
                        }

                        /*
                         * Client-specific image data consumption would go here
                         * We simply report the size of the data.
                         */
                        System.out.println("Page " + pageno + ": "
                                + imagedata.length + " bytes of image data");
                    }
                    else
                    {
			/*
			 * Fetch the image data and write it to a disk file. The
			 * output filenames are generated from the inputfilename
			 * by appending page number and image number.
			 */
                        String imageoptlist = baseimageoptlist + " filename={"
			    + outfilebase + "_p" + pageno + "_" + imageno + "}";

                        if (tet.write_image_file(doc, tet.imageid,
                                        imageoptlist) == -1)
                        {
                            print_tet_error(tet, pageno);
                            continue; /* process next image */
                        }
                    }

                }

                if (tet.get_errnum() != 0)
                {
                    print_tet_error(tet, pageno);
                }

                tet.close_page(page);
            }

            tet.close_document(doc);
            outfp.close();
        }
	catch (TETException e)
	{
	    System.err.println("TET exception occurred in extractor sample:");
	    System.err.println("[" + e.get_errnum() + "] " + e.get_apiname() +
			    ": " + e.get_errmsg());
        }
        catch (Exception e)
        {
            System.err.println(e.getMessage());
        }
        finally
        {
            if (tet != null) {
		tet.delete();
            }
        }
    }

    /**
     * Report a TET error.
     * 
     * @param tet The TET object
     * @param pageno The page number on which the error occurred
     */
    private static void print_tet_error(TET tet, int pageno)
    {
        System.err.println("Error " + tet.get_errnum() + " in  "
                + tet.get_apiname() + "() on page " + pageno + ": "
                + tet.get_errmsg());
    }
}
