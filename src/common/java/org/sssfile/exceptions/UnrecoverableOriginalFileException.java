package org.sssfile.exceptions;

/**
 * An error in writing a shard's secret to its file.
 */
public class UnrecoverableOriginalFileException extends Exception {
    public UnrecoverableOriginalFileException(String message) {
        super(message);
    }
}
