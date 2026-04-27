package io.kestra.plugin.proxmox.cluster;

import io.kestra.core.models.property.Property;
import io.kestra.plugin.proxmox.AbstractProxmoxTest;
import org.junit.jupiter.api.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

class ListResourcesTest extends AbstractProxmoxTest {

    @Test
    void listClusterResources() throws Exception {
        // The base stub in AbstractProxmoxTest already covers /cluster/resources.
        // This test exercises the task end-to-end and asserts the resources list is populated.
        var task = ListResources.builder()
            .host(Property.ofValue(baseUrl()))
            .node(Property.ofValue(NODE))
            .username(Property.ofValue("root@pam"))
            .password(Property.ofValue("secret"))
            .build();

        var output = task.run(runContextFactory.of());

        assertThat(output.resources(), not(empty()));
        assertThat(output.resources().getFirst().type(), notNullValue());
    }
}
