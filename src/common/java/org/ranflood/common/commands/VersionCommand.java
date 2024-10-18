package org.ranflood.common.commands;

import java.util.UUID;

import static org.ranflood.common.RanfloodLogger.error;

public class VersionCommand {
    private VersionCommand(){
    }

    public static class Get implements Command< String > {

        @Override
        public String execute(UUID id) {
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
