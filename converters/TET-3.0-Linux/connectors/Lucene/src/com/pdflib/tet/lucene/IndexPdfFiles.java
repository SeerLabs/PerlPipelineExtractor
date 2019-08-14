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
 * @version $Id: IndexPdfFiles.java,v 1.1 2008/11/11 20:37:48 tm Exp $
 */

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.IndexWriter;

import com.pdflib.TETException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Date;

public class IndexPdfFiles
{
    static String index_dir = "index";

    public static void main(String[] args) throws IOException
    {
        String usage = "java " + IndexPdfFiles.class + " <root_directory>";
        if (args.length == 0)
        {
            System.err.println("Usage: " + usage);
            System.exit(1);
        }

        File root = null;

        for (int i = 0; i < args.length; i++)
        {
            if (args[i].equals("-index"))
            {
                index_dir = args[++i];
            }
            else if (i != args.length - 1)
            {
                System.err.println("Usage: " + usage);
                System.exit(1);
            }
            else
            {
                root = new File(args[i]);
            }
        }

        Date start = new Date();
        try
        {
            IndexWriter writer = new IndexWriter(new File(index_dir),
                    new StandardAnalyzer(), true,
                    IndexWriter.MaxFieldLength.LIMITED);
            indexDocs(writer, root);

            writer.optimize();
            writer.close();

            Date end = new Date();

            System.out.print(end.getTime() - start.getTime());
            System.out.println(" total milliseconds");

        }
        catch (IOException e)
        {
            System.out.println(" caught a " + e.getClass()
                    + "\n with message: " + e.getMessage());
        }
    }

    public static void indexDocs(IndexWriter writer, File file)
            throws IOException
    {
        // do not try to index files that cannot be read
        if (file.canRead())
        {
            if (file.isDirectory())
            {
                String[] files = file.list();
                // an IO error could occur
                if (files != null)
                {
                    for (int i = 0; i < files.length; i++)
                    {
                        indexDocs(writer, new File(file, files[i]));
                    }
                }
            }
            else
            {
                String fileName = file.getName();
                if (fileName.toLowerCase().endsWith(".pdf"))
                {
                    System.out.println("adding " + file);
                    try
                    {
                        writer.addDocument(PdfDocument.Document(file));
                    }
                    // at least on windows, some temporary files raise this
                    // exception with an "access denied" message
                    // checking if the file can be read doesn't help
                    catch (FileNotFoundException fnfe)
                    {
                        ;
                    }
                    catch (TETException e)
                    {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}
