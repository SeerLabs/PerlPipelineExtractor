package wrapper;

import java.sql.*;
import org.apache.commons.exec.*;
import java.io.*;
import java.util.Properties;
import java.util.Date;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.logging.*;
import javax.xml.parsers.*;
import org.w3c.dom.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.*;
import javax.xml.transform.stream.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.text.SimpleDateFormat;

public class Wrapper {
   // document states
   public static final int BEING_PROCESSED = 2;
   public static final int PASSED = 1;
   public static final int UNPROCESSED = 0;
   public static final int FAILED = -1;


private static final Logger log = Logger.getLogger( Wrapper.class.getName() );   

   // number of external processes to start which parse pdf files 
   // this value is read from the config file
   private int maxProcesses;
   // number of external processes currently running
   private AtomicInteger currProcesses;

   private Connection dbConnection;

   private int OUT_LEVEL;

   public Wrapper() {
   }

   public void startProcessing() {
      //Read properties from config file
      String projectRootPath = System.getProperty("user.dir"); 
      Properties props = readConfigOptions(projectRootPath + "/config/config.properties");

      OUT_LEVEL = Integer.parseInt(props.getProperty("outputLevel"));
      
      String host = props.getProperty("host");
      String dbName = props.getProperty("dbName");
      String username = props.getProperty("username");
      String password = props.getProperty("password");
      String datasetId = props.getProperty("datasetId");
      String perlScriptName = props.getProperty("perlScriptName");
      if(OUT_LEVEL >= 2)
         System.out.println(projectRootPath);
      String baseDocumentPath = Util.removeSlash(props.getProperty("baseDocumentPath"));
      String parseResultsPath = Util.removeSlash(props.getProperty("parseResultsPath"));
      String logPath = props.getProperty("logDirPath");
      if(logPath.charAt(0) != '/') {
         logPath = String.format("%s/%s", projectRootPath, logPath);
      }
      String perlScriptPath = String.format("%s/lib/%s", projectRootPath, perlScriptName);
      int batchCnt = 0;
      int batchSize = Integer.parseInt(props.getProperty("batchSize"));

      maxProcesses = Integer.parseInt(props.getProperty("maxProcesses"));
      currProcesses = new AtomicInteger(0);

      //Make directory for perl scripts to write their results to
      File file = new File(Util.getJarDir() + "/results");
      file.mkdirs();

      file = new File(logPath);
      file.mkdirs();

      file = new File(projectRootPath + "/tmp");
      file.mkdirs();

      try {
         String logFile = String.format("%s/%s.log", logPath, datasetId);
         FileHandler handler = new FileHandler(logFile, true);
         handler.setFormatter(new SimpleFormatter());
         log.addHandler(handler);
         // print logs to STDOUT as well if OUT_LEVEL >= 1
         log.setUseParentHandlers(OUT_LEVEL >= 1);
      }
      catch(IOException e) {
         log.warning(e.toString());
      }


      // Connect to database
      Connection connection = getNewConnection(host, dbName, username, password);
      // wasn't going to make a class-level connection field, but easier to do it this way
      this.dbConnection = connection;

      // Init counters for generated xml files
      String fileDateString  = new SimpleDateFormat("yyyyMMddhhmm").format(new Date());
      int fileCounter = 1;
     
      boolean stopProcessing = false;
      while(!stopProcessing) {
         boolean processStarted = false;
         if(OUT_LEVEL >= 2)
            System.out.println("Checking to see if we should start a new process...");

         // if we don't have as many processes running as we can, start another
         if(currProcesses.get() < maxProcesses) {
            processStarted = true;
            int np = currProcesses.incrementAndGet();
            if(OUT_LEVEL >= 2)
               System.out.println("Starting process... Now running " + np + "/" + maxProcesses + " processes");

            // Perl script reads this file
            String xmlFilePath = String.format("%s/xml/%s-%d.xml", Util.getJarDir(), fileDateString, fileCounter);
            // And writes to this one
            String resultFilePath = String.format("%s/results/%s-%d", Util.getJarDir(), fileDateString, fileCounter);
            fileCounter++;

            ResultSet documents = getDocumentBatch(connection, batchSize);
            constructXML(documents, xmlFilePath);
            
            // return cursor to before first element and update all documents to status 2, processing
            try {
               documents.beforeFirst();
               ArrayList<Integer> ids = getIdsOfResultSet(documents);
               updateState(connection, ids, BEING_PROCESSED);

               processBatch(batchCnt, datasetId, xmlFilePath, perlScriptPath, baseDocumentPath, resultFilePath, parseResultsPath);
               batchCnt++;
            }
            catch (SQLException e) { 
               log.warning(e.toString());
            }
         }
        
         if(!processStarted) {
            // wait a little and check again if we should start a new process
            try {
               Thread.sleep(2000);
            }
            catch(InterruptedException e) {
               // Cool, we can just keep running...
            }
         }
         
         //read value of stopProcessing from config file, if true stop
         props = readConfigOptions(projectRootPath + "/dist/runtime.properties");
         stopProcessing = props.getProperty("stopProcessing", "false").equalsIgnoreCase("true");
      }

      // Java doesn't exit until all non-dameon threads are finished
      // So our threads which are watching the execution of the external programs 
      // are still running at this point in the code which is good
      // and the program won't finish until they are done
   }

   /**
    * @configFileName path of properties file relative to the location of the jar file
    */
   private Properties readConfigOptions(String configFileName) {
      Properties props = null;
      try {
         FileInputStream inputStream = new FileInputStream(configFileName);
         props = new Properties();
         props.load(inputStream);
      }
      catch(Exception e) {
         log.severe("Failed to load config values, aborting.\n" + e);
         System.exit(1);
      }

      return props;
   }

   /** Gets a connection to a database and exits on failure */
   private Connection getNewConnection(String host, String databaseName, String username, String password) {
      try {
         Class.forName("com.mysql.jdbc.Driver");
         String connectionPath = String.format("jdbc:mysql://%s/%s", host, databaseName);
         return DriverManager.getConnection(connectionPath, username, password);
      }
      catch(ClassNotFoundException e) {
         log.severe(e.toString());
         System.exit(1);
      }
      catch(SQLException e) {
         log.severe(e.toString());
         System.exit(1);
      }
      return null;
   }

   /** Gets a batch of documents from the database */
   private ResultSet getDocumentBatch(Connection connection, int batchSize) {
      try {
         PreparedStatement query = connection.prepareStatement("SELECT * from main_crawl_document WHERE state = ? AND (id >=30000000) ORDER BY priority desc LIMIT ?;",
            ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
         query.setInt(1, UNPROCESSED);
         query.setInt(2, batchSize);
         ResultSet rs = query.executeQuery();
         return rs;
      }
      catch(SQLException e) {
         log.warning(e.toString());
      }
      return null;
   }

   /** Constructs an xml document containing relevant document info for each document 
    *  XML format is a top level documents node filled with doc nodes
    *  Each doc node has its id in numerical form as an attribute and
    *  it contains text content which is the path to the document
    * */
   private void constructXML(ResultSet documents, String outputFilePath) {
      DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
      try {
         DocumentBuilder builder = dbf.newDocumentBuilder();
         Document xmlDoc = builder.newDocument();
         Element root = xmlDoc.createElement("documents");
         while(documents.next()) {
               int id = documents.getInt("id");
               String docPath = Util.idToIdPath(id);
               Element doc = xmlDoc.createElement("doc");
               doc.setAttribute("id", id+"");
               doc.appendChild(xmlDoc.createTextNode(docPath));
               root.appendChild(doc);
         }
         xmlDoc.appendChild(root);

         // write xml file to disk
         TransformerFactory tf = TransformerFactory.newInstance();
         Transformer transformer = tf.newTransformer();
         DOMSource source = new DOMSource(xmlDoc);
         File file = new File(outputFilePath);
         file.getParentFile().mkdirs();
         StreamResult result = new StreamResult(file);
         transformer.transform(source, result);
      }
      catch(SQLException e) {
         log.warning(e.toString());
      }
      catch(ParserConfigurationException e) {
         log.warning(e.toString());
      }
      catch(TransformerException e) {
         log.warning(e.toString());
      }
   }
   
   /**
    * @return An ArrayList<Integer> of the id of each element in set
    */
   private ArrayList<Integer> getIdsOfResultSet(ResultSet set) {
      ArrayList<Integer> list = new ArrayList<Integer>();
      try {
         while(set.next()) {
            list.add(set.getInt("id")); 
         }
      }
      catch (SQLException e) {
         log.warning(e.toString());
      }
      return list;
   }

   /**
    * Updates the state of each document in ids to newState
    * @param connection An db connection
    * @param ids An ArrayList of integer ids of documents in the db
    * @param newState the new state for all documents with an id in ids
    */
   private void updateState(Connection connection, ArrayList<Integer> ids, int newState) {
      StringBuilder idString = new StringBuilder();
      for(Integer id : ids) {
         if(idString.length() != 0) {
            idString.append(',');
         }
         idString.append(id);
      }
      updateState(connection, idString.toString(), newState);
   }

   /**
    * Updates the state of each document in ids to newState
    * @param connection An db connection
    * @param ids a comma seperated list of ids - ex) "5,23,45,98"
    * @param newState the new state for all documents with an id in ids
    */
   private void updateState(Connection connection, String ids, int newState) {
      if(ids.length() == 0) return;
      try {
         Statement statement = connection.createStatement();
         String query = String.format("UPDATE main_crawl_document SET state = %d where id in (%s);", newState, ids);
         if(OUT_LEVEL >= 2)
            System.out.println("Update query: " + query);
         statement.executeUpdate(query);
      }
      catch (SQLException e) {
         log.warning(e.toString());
      }
   }

   /** 
    * Starts a new process which runs a perl script that actually processes the documents
    */
   private void processBatch(int batchNum, String datasetId, String xmlFilePath, String scriptPath, String baseDocumentPath, String resultFilePath, String parseResultsPath) {
      try {
         String jarDir = Util.getJarDir();
         String cmd = String.format("perl %s %s %s %s %s %s %d %d", scriptPath, datasetId, xmlFilePath, baseDocumentPath, resultFilePath, parseResultsPath, batchNum, OUT_LEVEL);
         if(OUT_LEVEL >= 2)
            System.out.println(cmd);
         CommandLine commandLine = CommandLine.parse(cmd);
         DefaultExecutor executor = new DefaultExecutor();
         ProcessWatcher watcher = new ProcessWatcher(resultFilePath, this, batchNum);
         executor.execute(commandLine, watcher);
      }
      catch(ExecuteException e) {
         log.warning(e.toString());
      }
      catch(IOException e) {
         log.warning(e.toString());
      }
   }

   public synchronized void onProcessComplete(String resultsPath, int batchNum, long elapsedSeconds) {
      currProcesses.decrementAndGet();
      try {
         FileReader reader = new FileReader(resultsPath);
         Scanner scanner = new Scanner(reader);
         // expecting two lines, one that starts with 'failed:' and one that starts with 'passed:'
         // then a comma seperated list of ids
         // ex file)
         // failed:1,3,8,23,24
         // passed:2,25,90
         int failedCnt = 0;
         int passedCnt = 0;
         while(scanner.hasNextLine()) {
            String line = scanner.nextLine();
            if(line.startsWith("failed:")) {
               // get comma seperated part of string
               line = line.substring(7).trim();
               // find how many documents are in list
               failedCnt = (line.length() > 0 ? Util.countChar(line, ',')+1 : 0);
               updateState(dbConnection, line, FAILED);

            }
            else if(line.startsWith("passed:")) {
               line = line.substring(7).trim();
               passedCnt = (line.length() > 0 ? Util.countChar(line, ',')+1 : 0);
               updateState(dbConnection, line, PASSED);
            }
         }
         String msg = String.format("[Batch %3d] Passed: %8d  Failed: %8d  Time(s): %6d", batchNum, passedCnt, failedCnt, elapsedSeconds);
         log.info(msg);
      }
      catch(FileNotFoundException e) {
      }
   }

   public synchronized void onProcessFailed(String resultsPath) {
      currProcesses.decrementAndGet();
   }

   public static void main(String[] args) {
      Wrapper wrapper = new Wrapper();
      wrapper.startProcessing();
   }
}
