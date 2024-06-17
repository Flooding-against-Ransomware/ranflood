package org.sssfile.exceptions;

/**
 * An error in writing a shard's secret to its file.
 */
public class WriteShardException extends Exception {
    public WriteShardException(String message) {
        super(message);
    }
}
