package org.sssfile.exceptions;

/**
 * An error in writing a shard's secret to its file.
 */
public class ReadShardException extends Exception {
    public ReadShardException(String message) {
        super(message);
    }
}
