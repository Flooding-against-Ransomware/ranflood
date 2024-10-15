package org.ranflood.daemon.utils;

import org.ranflood.common.commands.Command;
import org.ranflood.common.commands.FloodCommand;
import org.ranflood.common.commands.SnapshotCommand;
import org.ranflood.common.commands.VersionCommand;
import org.ranflood.daemon.commands.FloodCommandImpl;
import org.ranflood.daemon.commands.SnapshotCommandImpl;
import org.ranflood.daemon.commands.VersionCommandImpl;

public class BindCmdToImpl {
    public static Command< ? > bindToImpl(Command< ? > command ) {
        if ( command instanceof SnapshotCommand.Add ) {
            return new SnapshotCommandImpl.Add( ( ( SnapshotCommand.Add ) command ).type() );
        }
        if ( command instanceof SnapshotCommand.Remove ) {
            return new SnapshotCommandImpl.Remove( ( ( SnapshotCommand.Remove ) command ).type() );
        }
        if ( command instanceof SnapshotCommand.List ) {
            return new SnapshotCommandImpl.List();
        }
        if ( command instanceof FloodCommand.Start ) {
            return new FloodCommandImpl.Start( ( ( FloodCommand.Start ) command ).type() );
        }
        if ( command instanceof FloodCommand.Stop ) {
            return new FloodCommandImpl.Stop(
                    ( ( FloodCommand.Stop ) command ).method(),
                    ( ( FloodCommand.Stop ) command ).id()
            );
        }
        if ( command instanceof FloodCommand.List ) {
            return new FloodCommandImpl.List();
        }
        if ( command instanceof VersionCommand.Get ) {
            return new VersionCommandImpl.Get();
        }
        throw new UnsupportedOperationException( "" );
    }
}
