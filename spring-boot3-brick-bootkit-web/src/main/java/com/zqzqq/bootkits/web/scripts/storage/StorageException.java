package com.zqzqq.bootkits.web.scripts.storage;

/**
 * 存储异常
 * 
 * @author brick-bootkit
 */
public class StorageException extends Exception {
    
    private static final long serialVersionUID = 1L;
    
    public StorageException(String message) {
        super(message);
    }
    
    public StorageException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public StorageException(Throwable cause) {
        super(cause);
    }
}