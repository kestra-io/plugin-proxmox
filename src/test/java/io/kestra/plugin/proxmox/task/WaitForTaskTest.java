package io.kestra.plugin.proxmox.task;

import io.kestra.core.models.property.Property;
import io.kestra.plugin.proxmox.AbstractProxmoxTest;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

class WaitForTaskTest extends AbstractProxmoxTest {

    @Test
    void waitForTask() throws Exception {
        // Base stub in AbstractProxmoxTest already covers /nodes/.*/tasks/.*/status
        // and immediately returns stopped/OK, so the poll loop exits on the first check.
        var task = WaitForTask.builder()
            .host(Property.ofValue(baseUrl()))
            .node(Property.ofValue(NODE))
            .username(Property.ofValue("root@pam"))
            .password(Property.ofValue("secret"))
            .upid(Property.ofValue(UPID))
            .timeoutSeconds(Property.ofValue(30))
            .build();

        var output = task.run(runContextFactory.of());

        assertThat(output.getUpid(), is(UPID));
    }
}
