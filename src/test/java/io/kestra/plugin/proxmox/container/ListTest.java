package io.kestra.plugin.proxmox.container;

import io.kestra.core.models.property.Property;
import io.kestra.plugin.proxmox.AbstractProxmoxTest;
import org.junit.jupiter.api.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

class ListTest extends AbstractProxmoxTest {

    @Test
    void listContainers() throws Exception {
        wireMock.stubFor(get(urlEqualTo("/api2/json/nodes/pve/lxc"))
            .willReturn(okJson("""
                {"data":[{"vmid":200,"name":"test-ct","status":"running","cpus":1,"maxmem":536870912}]}
                """)));

        var task = List.builder()
            .host(Property.ofValue(baseUrl()))
            .node(Property.ofValue(NODE))
            .username(Property.ofValue("root@pam"))
            .password(Property.ofValue("secret"))
            .build();

        var output = task.run(runContextFactory.of());

        assertThat(output.containers(), hasSize(1));
        assertThat(output.containers().getFirst().name(), is("test-ct"));
    }
}
