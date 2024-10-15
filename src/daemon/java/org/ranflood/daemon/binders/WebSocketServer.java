package org.ranflood.daemon.binders;

import jakarta.websocket.OnClose;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;
import org.ranflood.common.commands.*;
import org.ranflood.common.commands.transcoders.JSONTranscoder;
import org.ranflood.common.commands.transcoders.ParseException;
import org.ranflood.common.commands.types.CommandResult;
import org.ranflood.common.commands.types.RanfloodType;
import org.ranflood.common.commands.types.RequestStatus;
import org.ranflood.daemon.Ranflood;
import org.ranflood.daemon.commands.FloodCommandImpl;
import org.ranflood.daemon.commands.SnapshotCommandImpl;
import org.ranflood.daemon.commands.VersionCommandImpl;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import static org.ranflood.common.RanfloodLogger.error;
import static org.ranflood.common.RanfloodLogger.log;
import static org.ranflood.common.commands.transcoders.JSONTranscoder.*;
import static org.ranflood.daemon.utils.BindCmdToImpl.bindToImpl;

@ServerEndpoint("/websocket")
public class WebSocketServer {
	@OnOpen
	public void onOpen(Session session) {
		log("Connected: " + session.getId());
	}

	@OnMessage
	public void onMessage(String message, Session session) {
		UUID id;
		try{
			id = extractIdFromJson(message);
		} catch (Exception e) {
			error(e.getMessage());
			throw new RuntimeException(e);
		}

		Ranflood.daemon().executeCommand(() -> {
			log("WebSocket server received [" + message + "]");
			try {
				Command< ? > command = bindToImpl( JSONTranscoder.fromJson( message ) );

				if ( command.isAsync() ) {
					RequestStatus requestStatus = new RequestStatus(command, "in progress", id);
					session.getBasicRemote().sendText(requestStatusToJson(requestStatus));

					Ranflood.daemon().executeCommand( () -> {
						Object result = command.execute(id);
						if ( result instanceof CommandResult.Successful ) {
							log( ( ( CommandResult.Successful ) result ).message());
							requestStatus.setStatus("success");
						} else {
							error( ( ( CommandResult.Failed ) result ).message());
							requestStatus.setStatus("error");
							requestStatus.setData(( ( CommandResult.Failed ) result ).message());
						}

						try{
							session.getBasicRemote().sendText(requestStatusToJson(requestStatus));
						} catch (IOException e) {
							throw new RuntimeException(e);
						}
					} );
				} else {
					RequestStatus requestStatus = new RequestStatus(command, "in progress", id);
					try{
						if ( command instanceof  VersionCommand.Get) {
							String version = ( ( VersionCommandImpl.Get ) command ).execute(id);
							requestStatus.setData(version);
							requestStatus.setStatus("success");
						}
						else {
							List< ? extends RanfloodType> l =
									( command instanceof SnapshotCommand.List ) ?
											( ( SnapshotCommandImpl.List ) command ).execute(id)
											: ( ( FloodCommandImpl.List ) command ).execute(id);
							requestStatus.setData(wrapListRanfloodType(l));
							requestStatus.setStatus("success");
						}
						session.getBasicRemote().sendText(requestStatusToJson(requestStatus));
					}catch (Exception exception){
						requestStatus.setData(exception.getMessage());
						requestStatus.setStatus("error");
						session.getBasicRemote().sendText(requestStatusToJson(requestStatus));
					}
				}
			} catch (ParseException e) {
				error(e.getMessage());
				try {
					session.getBasicRemote().sendText(JSONTranscoder.wrapError(e.getMessage()));
				} catch (IOException ex) {
					throw new RuntimeException(ex);
				}
			} catch (Exception e) {
				error(e.getMessage());
				try {
					session.getBasicRemote().sendText(e.getMessage());
				} catch (IOException ex) {
					throw new RuntimeException(ex);
				}
			}
		});
	}

	@OnClose
	public void onClose(Session session) {
		log("Disconnected: " + session.getId());
	}
}
