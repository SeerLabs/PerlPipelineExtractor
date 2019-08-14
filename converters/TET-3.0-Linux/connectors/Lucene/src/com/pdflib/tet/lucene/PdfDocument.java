package com.pdflib.tet.lucene;

/**
 * (C) PDFlib GmbH 2008 www.pdflib.com
 *
 * This code is based on the Lucene Java "Basic Demo", which is part of the
 * Lucene Java distribution available at http://lucene.apache.org/java. This
 * code has been tested with Lucene Java 2.4.0.
 * 
 * The original code carried the following copyright notice:
 *
 * Copyright 2004 The Apache Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 * @version $Id: PdfDocument.java,v 1.1 2008/11/11 20:37:48 tm Exp $
 */

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.lucene.document.DateTools;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;

import com.pdflib.TET;
import com.pdflib.TETException;

/**
 * A utility for making Lucene Documents from PDF documents. For demonstration
 * purposes the class can be configured to read each PDF document into memory,
 * and let TET read the PDF document from memory. This could be used in a
 * web crawler that reads the documents to index over some method (e.g. HTTP)
 * into memory, and passes the PDF document as a byte array to the
 * constructor of the TetReader class.
 * 
 * @see TetReader
 */
public class PdfDocument
{
    /**
     * Whether to pass the PDF documents in memory to TET.
     */
    private static final boolean PASS_PDF_IN_MEMORY = false;
    
    /**
     * The directory separator.
     */
    static char dirSep = System.getProperty("file.separator").charAt(0);

    /**
     * Makes a Document instance for a PDF file. This sample was derived from
     * the Lucene org.apache.lucene.demo.HTMLDocument class.
     * <p>
     * The document has the following fields:
     * <ul>
     * <li><code>path</code>--containing the pathname of the file, as a stored,
     * tokenized field;
     * <li><code>modified</code>--containing the last modified date of the file
     * as a keyword field as encoded by <a
     * href="lucene.document.DateField.html">DateField</a>;
     * <li><code>contents</code>--containing the full contents of the file, as a
     * Reader field;
     * <li>all PDF DocInfo fields;
     * <li><code>font</code>--the names of all fonts found in the PDF document.
     * </ul>
     * 
     * @throws TETException
     * @throws IOException 
     */
    public static Document Document(File f)
            throws TETException, IOException
    {
        Document doc = new Document();

        /*
         *  Add the url as a field named "path". Use a field that is
         * indexed (i.e. searchable), but don't tokenize the field into words.
         */
        doc.add(new Field("path", f.getPath().replace(dirSep, '/'),
                Field.Store.YES, Field.Index.NOT_ANALYZED));

        /*
         * Add the last modified date of the file a field named "modified". Use
         * a Keyword field, so that it's searchable, but so that no attempt is
         * made to tokenize the field into words.
         */
        doc.add(new Field("modified", DateTools.timeToString(f.lastModified(),
                DateTools.Resolution.MINUTE), Field.Store.YES,
                Field.Index.NOT_ANALYZED));

        /*
         * Create a TetReader that either reads the file directly from
         * disk, or that gets the contents as a byte array.
         */
        TetReader reader;
        if (PASS_PDF_IN_MEMORY) {
            byte[] bytes = getBytesFromFile(f);
            reader = new TetReader(bytes);
        }
        else {
            reader = new TetReader(f);
        }
        
        /*
         * Add the contents of the file sa a field named "contents". Use a Text
         * field, specifying a Reader, so that the text of the file is
         * tokenized.
         */        
        doc.add(new Field("contents", reader));

        /*
         * Borrow the TET object and the document handle from the TetReader
         * to extract some properties.
         */
        TET tet = reader.getTet();
        int tetHandle = reader.getDoc();

        /*
         * Index the DocInfo Title and Subject fields with the field names that
         * are expected by the Lucene demo web application, "title" and
         * "subject".
         */
        String objType = tet.pcos_get_string(tetHandle, "type:/Info/Subject");
        if (!objType.equals("null"))
        {
            doc.add(new Field("summary", tet.pcos_get_string(tetHandle,
                    "/Info/Subject"), Field.Store.YES, Field.Index.ANALYZED));
        }
        objType = tet.pcos_get_string(tetHandle, "type:/Info/Title");
        if (!objType.equals("null"))
        {
            doc.add(new Field("title", tet.pcos_get_string(tetHandle,
                    "/Info/Title"), Field.Store.YES, Field.Index.ANALYZED));
        }

        /*
         * Add all the DocInfo fields, including Subject and Title with their
         * name written in upper-case
         */
        int infoFields = (int) tet.pcos_get_number(tetHandle, "length:/Info");

        for (int i = 0; i < infoFields; i += 1)
        {
            String key = tet.pcos_get_string(tetHandle, "/Info[" + i + "].key");
            String value = tet.pcos_get_string(tetHandle, "/Info[" + i + "]");
            doc.add(new Field(key, value, Field.Store.YES,
                            Field.Index.ANALYZED));
        }

        int fonts = (int) tet.pcos_get_number(tetHandle, "length:fonts");

        for (int font = 0; font < fonts; font += 1)
        {
            String fontName = tet.pcos_get_string(tetHandle,
                    "fonts[" + font + "]/name");
            doc.add(new Field("font", fontName, Field.Store.YES,
                    Field.Index.ANALYZED));
        }

        return doc;
    }
    
 
    /**
     * Reads a file into a byte array.
     * 
     * @param file
     *            the file to read in
     * 
     * @return a byte array with the contents of the file
     * 
     * @throws IOException
     */
    public static byte[] getBytesFromFile(File file) throws IOException {
        InputStream is = new FileInputStream(file);
    
        // Get the size of the file
        long length = file.length();

        /*
         * You cannot create an array using a long type. It needs to be an int
         * type. Before converting to an int type, check to ensure that file is
         * not larger than Integer.MAX_VALUE.
         */
        if (length > Integer.MAX_VALUE) {
            throw new IOException("file \"" + file.toString()
                    + "\" is too large (" + length + " bytes)");
        }
    
        // Create the byte array to hold the data
        byte[] bytes = new byte[(int)length];
    
        // Read in the bytes
        int numRead = is.read(bytes, 0, bytes.length);
    
        // Ensure all the bytes have been read in
        if (numRead < bytes.length) {
            throw new IOException("Could not completely read file "+file.getName());
        }
    
        // Close the input stream and return bytes
        is.close();
        return bytes;
    }

    /**
     * Private constructor to prevent instantiation.
     */
    private PdfDocument()
    {
    }
}
