package wrapper;

import org.apache.commons.exec.*;

public class ProcessWatcher implements ExecuteResultHandler {

   private String resultsPath;
   private Wrapper wrapper;
   private int batchNum;
   private long start;

   public ProcessWatcher(String resultsPath, Wrapper wrapper, int batchNum) {
      this.resultsPath = resultsPath;
      this.wrapper = wrapper;
      this.batchNum = batchNum;
      this.start = System.currentTimeMillis();
   }

   public void onProcessComplete(int exitValue) {
      long elapsedSeconds = (System.currentTimeMillis() - start)/1000;
      wrapper.onProcessComplete(resultsPath, batchNum, elapsedSeconds);
   }

   public void onProcessFailed(ExecuteException e) {
      System.out.println("Execution failed:\n" + e);
      wrapper.onProcessFailed(resultsPath);
   }
}
