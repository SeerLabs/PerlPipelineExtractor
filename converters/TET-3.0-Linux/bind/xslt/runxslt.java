import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

/**
 * Simple Java wrapper to run TET XSLT samples.
 * <br><br>
 * usage: java runxsl [ &lt;input file&gt; &lt;stylesheet&gt; &lt;output file&gt; [ { &lt;param name&gt; &lt;param value&gt; } ... ] ]
 *<br><br>
 * If the program is invoked without arguments, it will run all the samples.
 * If the program is invoked with the arguments &lt;input file&gt;, &lt;stylesheet&gt; and
 * &lt;output file&gt;, the script will run the given stylesheet for the input file
 * and write the results to the output file. Stylesheet parameters can be
 * provided as pairwise arguments at the end of the command line.
 *<br>
 * @author stm
 *
 * @version $Id: runxslt.java,v 1.2 2008/11/12 13:22:23 tm Exp $
 */
public class runxslt
{
    private static String[][] runs =
    {
        {"FontReporter.tetml", "concordance.xsl", "concordance.txt"},
        {"FontReporter.tetml", "index.xsl", "index.txt"},
        {"FontReporter.tetml", "table.xsl", "table.csv"},
        {"FontReporter.tetml", "textonly.xsl", "textonly.txt"},
        {"FontReporter.tetml", "metadata.xsl", "metadata.txt"},
        {"FontReporter.tetml", "fontfilter.xsl", "fontfilter.txt"},
        {"FontReporter.tetml", "fontstat.xsl", "fontstat.txt"},
        {"FontReporter.tetml", "fontfinder.xsl", "fontfinder.txt"},
        {"FontReporter.tetml", "tetml2html.xsl", "tetml2html.html"}
    };
    
    class StyleSheetParameter
    {
        public String name;
        public String value;
    }
    
    /**
     * @param args
     */
    public static void main(String[] args)
    {
        runxslt runner = new runxslt();
        
        if (args.length > 3)
        {
            int paramCount = args.length - 3;
            
            // XSLT parameters must appear pairwise on the command line
            if (paramCount % 2 != 0)
            {
                System.err.println("Arguments for stylesheet parameters must occur pairwise");
                usage();
            }
            
            LinkedList params = new LinkedList();
            for (int i = 3; i < args.length; i += 2)
            {
                StyleSheetParameter p = runner.new StyleSheetParameter();
                p.name = args[i];
                p.value = args[i + 1];
                params.add(p);
            }
            
            runner.runXslt(args[0], args[1], args[2], params);
        }
        else if (args.length == 0)
        {
            for (int i = 0; i < runs.length; i += 1)
            {
                runner.runXslt(runs[i][0], runs[i][1], runs[i][2], null);
            }
        }
        else
        {
            usage();
        }
    }

    /**
     * Run the transformation, with optional stylesheet parameters.
     * 
     * @param inputFile The TETML input file
     * @param styleSheet The XSLT stylesheet
     * @param outputFile The output file
     * @param params A list of StyleSheetParameter instances
     */
    private void runXslt(String inputFile, String styleSheet, String outputFile,
            List params)
    {
        try
        {
            Source xmlInput = new StreamSource(inputFile);
            Source xsltSource = new StreamSource(styleSheet);
            Result result = new StreamResult(outputFile);
            
            TransformerFactory factory = TransformerFactory.newInstance();
            Transformer transformer = factory.newTransformer(xsltSource);
            
            if (params != null)
            {
                Iterator i = params.iterator();
                while (i.hasNext())
                {
                    StyleSheetParameter p = (StyleSheetParameter) i.next();
                    transformer.setParameter(p.name, p.value);
                }
            }
            
            System.out.println("Transforming input file \"" + inputFile
                    +  "\" with stylesheet \"" + styleSheet
                    + "\" to output file \"" + outputFile + "\"");
            
            transformer.transform(xmlInput, result);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    private static void usage()
    {
        System.err.println("usage: runxsl [ <input file> <stylesheet> <output file> [ { <param name> <param value> } ... ] ]");
        System.exit(1);
    }
}
