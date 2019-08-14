package wrapper;

import java.io.*;
import java.net.*;

public class Util {

   public Util() {
   }

   public static String getJarDir() {
         try {
            URL url = Util.class.getProtectionDomain().getCodeSource().getLocation();
            String jarDirPath = new File(url.toURI()).getParentFile().getPath();
            return jarDirPath;
         }
         catch(URISyntaxException e) {
            System.err.println(e);
         }
         return "";
   }

   /**
    * @return str without a trailing '/' if it was present
    */
   public static String removeSlash(String str) {
      if(str.endsWith("/")) {
         return str.substring(0, str.length()-1);
      }
      else {
         return str;
      }
   }

   public static int countChar(String str, char c) {
      int count = 0;
      for(int i = 0; i < str.length(); ++i) {
         if(str.charAt(i) == c) {
            count++;
         }
      }
      return count;
   }

   /**
    * Returns a new string padded with zeroes on the left side to be 
    * at least minLength long
    */
   public static String zfill(String str, int minLength) {
      int diff = minLength - str.length();
      if(diff > 0) {
         return String.format("%0" + diff + "d%s", 0, str);
      }
      else {
         return str;
      }
   }

   /**
    * Takes an integer id and returns a string in the format xxx/yyy/zzz/xxx.yyy.xxx.pdf
    */
   public static String idToIdPath(int id) {
     int p1 = id / 1000000;
     int p2 = (id % 1000000) / 1000;
     int p3 = id % 1000;
     String s1 = Util.zfill(p1+"", 3);
     String s2 = Util.zfill(p2+"", 3);
     String s3 = Util.zfill(p3+"", 3);
     return String.format("%s/%s/%s/%s.%s.%s.pdf", s1, s2, s3, s1, s2, s3);
   }
}
