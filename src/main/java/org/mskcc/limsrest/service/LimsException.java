package org.mskcc.limsrest.service;

public class LimsException extends Exception {
   public LimsException(String message) { super(message); }

   public LimsException(Throwable t){
     super(t);
   }

   public LimsException(String message, Throwable t){
     super(message, t);
   }
}