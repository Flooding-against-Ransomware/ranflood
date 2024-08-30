package org.ranflood.common.commands;

import org.ranflood.common.commands.types.RequestStatus;

import java.util.UUID;

import static org.ranflood.common.RanfloodLogger.error;

public class BufferCommand {
    private BufferCommand(){
    }

    public static class Get implements Command< RequestStatus > {

        private final String id;

        public Get( String id ) {
            this.id = id;
        }

        public String id() {
            return id;
        }

        @Override
        public RequestStatus execute(UUID id) {
            error( "Unsupported exception" );
            throw new UnsupportedOperationException( "Execution is not implemented by the AbstractCommand class" );
        }

        @Override
        public String name() {
            return "Get version";
        }

        @Override
        public boolean isAsync() {
            return false;
        }

    }
}