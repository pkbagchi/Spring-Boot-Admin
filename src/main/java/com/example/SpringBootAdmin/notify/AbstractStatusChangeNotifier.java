package com.example.SpringBootAdmin.notify;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import de.codecentric.boot.admin.server.notify.AbstractEventNotifier;
import reactor.core.publisher.Mono;

import de.codecentric.boot.admin.server.domain.entities.Instance;
import de.codecentric.boot.admin.server.domain.entities.InstanceRepository;
import de.codecentric.boot.admin.server.domain.events.InstanceDeregisteredEvent;
import de.codecentric.boot.admin.server.domain.events.InstanceEvent;
import de.codecentric.boot.admin.server.domain.events.InstanceStatusChangedEvent;
import de.codecentric.boot.admin.server.domain.values.InstanceId;

/**
 * Abstract Notifier for status change which allows filtering of certain status changes.
 *
 * @author Johannes Edmeier
 */
public abstract class AbstractStatusChangeNotifier extends AbstractEventNotifier {

    private final Map<InstanceId, String> lastStatuses = new HashMap<>();

    /**
     * List of changes to ignore. Must be in Format OLD:NEW, for any status use * as
     * wildcard, e.g. *:UP or OFFLINE:*
     */
    private String[] ignoreChanges = { "UNKNOWN:UP" };

    public AbstractStatusChangeNotifier(InstanceRepository repository) {
        super(repository);
    }

    @Override
    public Mono<Void> notify(InstanceEvent event) {
        return super.notify(event).then(Mono.fromRunnable(() -> updateLastStatus(event)));
    }

    @Override
    protected boolean shouldNotify(InstanceEvent event, Instance instance) {
        if (event instanceof InstanceStatusChangedEvent) {
            InstanceStatusChangedEvent statusChange = (InstanceStatusChangedEvent) event;
            String from = getLastStatus(event.getInstance());
            String to = statusChange.getStatusInfo().getStatus();
            return Arrays.binarySearch(ignoreChanges, from + ":" + to) < 0
                    && Arrays.binarySearch(ignoreChanges, "*:" + to) < 0
                    && Arrays.binarySearch(ignoreChanges, from + ":*") < 0;
        }
        return false;
    }

    protected final String getLastStatus(InstanceId instanceId) {
        return lastStatuses.getOrDefault(instanceId, "UNKNOWN");
    }

    protected void updateLastStatus(InstanceEvent event) {
        if (event instanceof InstanceDeregisteredEvent) {
            lastStatuses.remove(event.getInstance());
        }
        if (event instanceof InstanceStatusChangedEvent) {
            lastStatuses.put(event.getInstance(), ((InstanceStatusChangedEvent) event).getStatusInfo().getStatus());
        }
    }

    public void setIgnoreChanges(String[] ignoreChanges) {
        String[] copy = Arrays.copyOf(ignoreChanges, ignoreChanges.length);
        Arrays.sort(copy);
        this.ignoreChanges = copy;
    }

    public String[] getIgnoreChanges() {
        return ignoreChanges;
    }

}