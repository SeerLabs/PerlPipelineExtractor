import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;

import com.pdflib.TET;
import com.pdflib.TETException;

import oracle.jdbc.pool.OracleDataSource;

/**
 * Program to load the PDF documents from the "bind/data" directory of the
 * TET distribution into an Oracle database. The Oracle database must have
 * been prepared with the SQL script "tetsetup_b.sql" to contain a table
 * "pdftable_b" and a corresponding index "tetindex_b".
 * 
 * @version $Id: tet_pdf_loader.java,v 1.3 2009/01/28 10:25:42 stm Exp $
 */
public class tet_pdf_loader
{
    /**
     * Global option list
     */
    static final String GLOBAL_OPTLIST = "searchpath={../../../resource/cmap" +
                        " ../../../resource/glyphlist}";
    
    /**
     * Document-specific option list
     * 
     * Indexing of password-protected documents that disallow text extraction is
     * possible by using the "shrug" option of TET_open_document(). Please read
     * the relevant section in the PDFlib Terms and Conditions and the TET
     * Manual about the "shrug" option to understand the implications of
     * using this feature.
     * 
     * static final String DOC_OPTLIST = "shrug";
     */
     */
    static final String DOC_OPTLIST = "";

    /**
     * List of PDF test documents provided with the TET distribution.
     */
    private static final String[] PDF_TEST_FILES =
    {
        "FontReporter.pdf",
        "Whitepaper-XMP-metadata-in-PDFlib-products-J.pdf",
        "Whitepaper-XMP-metadata-in-PDFlib-products.pdf",
        "Whitepaper-PDFA-with-PDFlib-products-J.pdf",
        "Whitepaper-PDFA-with-PDFlib-products.pdf",
        "PDFlib-datasheet.pdf",
        "TET-PDF-IFilter-datasheet.pdf"
    };
    
    /**
     * Directory that contains above files, provided via system property
     * "tet.data.dir".
     */
    private static final String PDF_TEST_FILES_DIR =
        System.getProperty("tet.data.dir");
    
    /**
     * JDBC connection URL, provided via system property "tet.jdbc.connection".
     */
    private static final String JDBC_URL =
        System.getProperty("tet.jdbc.connection");

    /**
     * Database connection user name, provided via system property
     * "tet.jdbc.user".
     */
    private static final String JDBC_USER =
        System.getProperty("tet.jdbc.user");

    /**
     * Database connection password, provided via system property
     * "tet.jdbc.password".
     */
    private static final String JDBC_PASSWORD =
        System.getProperty("tet.jdbc.password");
    
    public static void main(String[] args) throws ClassNotFoundException, SQLException
    {
        if (PDF_TEST_FILES_DIR == null)
        {
            System.err.println("Path to the 'bind/data' directory in the TET installation directory must be specified via system property \"tet.data.dir\"");
            System.exit(1);
        }
        if (JDBC_URL == null)
        {
            System.err.println("JDBC connection URL must be specified via system property \"tet.jdbc.connection\"");
            System.exit(1);
        }
        if (JDBC_USER == null)
        {
            System.err.println("User name for database connection must be specified via system property \"tet.jdbc.user\"");
            System.exit(1);
        }
        if (JDBC_PASSWORD == null)
        {
            System.err.println("Password for database connection must be specified via system property \"tet.jdbc.password\"");
            System.exit(1);
        }
        
        insert_pdf_documents();
    }

    /**
     * Take all the documents specified in array PDF_TEST_FILES, extract
     * the title and the number of pages with TET, and put the document as a
     * blob together with the metadata into the table "pdftable_b".
     */
    public static void insert_pdf_documents() throws SQLException
    {
        Connection connection = null; // Database connection object

        TET tet = null;

        try
        {
            tet = new TET();
            
            tet.set_option(GLOBAL_OPTLIST);
            
            OracleDataSource ods = new OracleDataSource();
            ods.setURL(JDBC_URL);
            ods.setUser(JDBC_USER);
            ods.setPassword(JDBC_PASSWORD);
            connection = ods.getConnection();

            connection.setAutoCommit(false);
            
            PreparedStatement pstmt = connection
                .prepareStatement("INSERT INTO pdftable_b VALUES (?,?,?,?)");

            Statement stmt = connection
                    .createStatement(ResultSet.TYPE_SCROLL_SENSITIVE,
                            ResultSet.CONCUR_READ_ONLY);
            
            System.out.println("Loading PDF documents from \"" + PDF_TEST_FILES_DIR + "\"");
            
            for (int i = 0; i < PDF_TEST_FILES.length; i += 1)
            {
                File pdfFile = new File(PDF_TEST_FILES_DIR, PDF_TEST_FILES[i]);

                int pageCount = -1;
                String title = null;
                
                try
                {
                    int doc = tet.open_document(pdfFile.toString(), DOC_OPTLIST);
                    if (doc == -1)
                    {
                        System.err.println("Error opening document \""
                            + pdfFile.toString() + "\": error "
                            + tet.get_errnum() + " in "
                            + tet.get_apiname() + "(): " + tet.get_errmsg());
                    }
                    else
                    {
                        /*
                         * Get title and number of pages in the document
                         */
                        pageCount = (int) tet.pcos_get_number(doc, "length:pages");
                        String objType = tet.pcos_get_string(doc, "type:/Info/Title");
                        if (!objType.equals("null"))
                        {
                            title = tet.pcos_get_string(doc, "/Info/Title");
                        }
                        tet.close_document(doc);
                    }
                }
                catch (TETException e)
                {
                    System.err.println("Error while processing document \""
                            + pdfFile.toString() + "\": error "
                            + e.get_errnum() + " in "
                            + e.get_apiname() + "(): " + e.get_errmsg());
                    
                    /*
                     * Create a new TET object for processing the next
                     * document.
                     */
                    tet.delete();
                    tet = new TET();
                }
                
                /*
                 * Get the next primary key for inserting a new record.
                 */
                ResultSet srs = stmt
                        .executeQuery("SELECT MAX(pk) FROM pdftable_b");
                int number = 0;
                while (srs.next())
                {
                    number = srs.getInt(1);
                }
                srs.clearWarnings();
                
                number += 1;
                System.out.println("new primary key is " + number);
    
                /*
                 * Insert PDF document as BLOB and metadata into corresponding
                 * columns.
                 */
                pstmt.setInt(1, number);
                if (title != null)
                {
                    pstmt.setString(2, title);
                }
                else
                {
                    pstmt.setNull(2, Types.VARCHAR);
                }
                if (pageCount != -1)
                {
                    pstmt.setInt(3, pageCount);
                }
                else
                {
                    pstmt.setNull(3, Types.INTEGER);
                }
                FileInputStream is = new FileInputStream(pdfFile);
                pstmt.setBinaryStream(4, is, (int) pdfFile.length());
                pstmt.execute();
    
                connection.commit();
                System.out.println("Loaded \"" + PDF_TEST_FILES[i]
                    + "\", title \"" + title + "\", "
                    + "page count " + pageCount);
            }
            stmt.close();
            pstmt.close();
        }
        catch (FileNotFoundException e)
        {
            e.printStackTrace();
        }
        catch (TETException e)
        {
            System.err.println("Error setting up the TET object: error "
                    + e.get_errnum() + " in "
                    + e.get_apiname() + "(): " + e.get_errmsg());
            e.printStackTrace();
        }
        finally
        {
            if (tet != null)
            {
                tet.delete();
            }
            try
            {
                if (connection != null && !connection.isClosed())
                    connection.close(); // Close the database connection
            }
            catch (SQLException ex)
            {
                ex.printStackTrace();
            }
        }
    }
}
