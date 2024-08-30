package org.ranflood.daemon.commands;

import org.ranflood.common.commands.VersionCommand;
import org.ranflood.daemon.Ranflood;

import java.util.UUID;


public class VersionCommandImpl {
    private VersionCommandImpl(){
    }

    public static class Get extends VersionCommand.Get {

        @Override
        public String execute(UUID id) {
            return Ranflood.version();
        }

    }

}
