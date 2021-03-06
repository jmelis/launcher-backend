package io.fabric8.launcher.osio.client;

import org.immutables.value.Value;

/**
 * @author <a href="mailto:ggastald@redhat.com">George Gastaldi</a>
 */
@Value.Immutable
public interface Space {
    String getId();

    String getName();
}