package org.ranflood.common.commands.types;

import org.ranflood.common.commands.Command;

import java.time.Instant;
import java.util.UUID;

public class RequestStatus {
    private final Command< ? > command;
    private final UUID id;
    private String status;
    private Instant timestamp;
    private String  data;

    public RequestStatus(Command< ? > command, String status, UUID id) {
        this.command = command;
        this.status = status;
        this.id = id;
        this.timestamp = Instant.now();
    }

    public Command< ? > getCommand() {
        return command;
    }

    public UUID getId() {
        return id;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp() {
        this.timestamp = Instant.now();
    }

    public void setData(String data) {
        this.data = data;
    }

    public String getData() { return this.data; }
}
