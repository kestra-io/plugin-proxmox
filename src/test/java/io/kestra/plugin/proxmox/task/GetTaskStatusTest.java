package io.kestra.plugin.proxmox.task;

import io.kestra.core.models.property.Property;
import io.kestra.plugin.proxmox.AbstractProxmoxTest;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

class GetTaskStatusTest extends AbstractProxmoxTest {

    @Test
    void getTaskStatus() throws Exception {
        // Base stub in AbstractProxmoxTest already covers /nodes/.*/tasks/.*/status.
        var task = GetTaskStatus.builder()
            .host(Property.ofValue(baseUrl()))
            .node(Property.ofValue(NODE))
            .username(Property.ofValue("root@pam"))
            .password(Property.ofValue("secret"))
            .upid(Property.ofValue(UPID))
            .build();

        var output = task.run(runContextFactory.of());

        assertThat(output.taskStatus(), notNullValue());
        assertThat(output.taskStatus().status(), is("stopped"));
    }
}
