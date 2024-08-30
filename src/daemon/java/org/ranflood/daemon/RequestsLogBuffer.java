package org.ranflood.daemon;

import org.ranflood.common.commands.Command;
import org.ranflood.common.commands.types.RequestStatus;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class RequestsLogBuffer {
    private static final Map<UUID, RequestStatus> buffer = new ConcurrentHashMap<>();

    public static void addRequest(UUID id, Command< ? > command) {
        buffer.put(id, new RequestStatus(command, "in progress", id));
    }

    public static void updateStatus(UUID id, String status) {
        RequestStatus requestStatus = buffer.get(id);
        if (requestStatus != null) {
            requestStatus.setStatus(status);
            requestStatus.setTimestamp();
        }
    }

    public static void setErrorMsg(UUID id, String errorMsg) {
        RequestStatus requestStatus = buffer.get(id);
        if (requestStatus != null) {
            requestStatus.setErrorMsg(errorMsg);
        }
    }

    public static RequestStatus getRequestStatus(UUID id) {
        return buffer.get(id);
    }

    public static void cleanUpExpiredRequests(long expirationTimeInSeconds) {
        Instant now = Instant.now();
        buffer.entrySet().removeIf(entry ->
                entry.getValue().getTimestamp().plusSeconds(expirationTimeInSeconds).isBefore(now)
        );
    }
}
