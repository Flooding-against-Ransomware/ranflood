package org.ranflood.daemon.commands;

import org.ranflood.common.commands.BufferCommand;
import org.ranflood.common.commands.types.RequestStatus;
import org.ranflood.daemon.RequestsLogBuffer;

import java.util.UUID;

public class BufferCommandImpl {
    private BufferCommandImpl(){}

    public static class Get extends BufferCommand.Get {

        public Get( String id ) {
            super( id );
        }

        @Override
        public RequestStatus execute(UUID id) {
            return RequestsLogBuffer.getRequestStatus(UUID.fromString( this.id() ));
        }
    }
}