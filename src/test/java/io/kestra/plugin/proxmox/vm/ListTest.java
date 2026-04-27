package io.kestra.plugin.proxmox.vm;

import io.kestra.core.models.property.Property;
import io.kestra.plugin.proxmox.AbstractProxmoxTest;
import org.junit.jupiter.api.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

class ListTest extends AbstractProxmoxTest {

    @Test
    void listVms() throws Exception {
        wireMock.stubFor(get(urlEqualTo("/api2/json/nodes/pve/qemu"))
            .willReturn(okJson("""
                {"data":[{"vmid":100,"name":"test-vm","status":"running","cpus":2,"maxmem":4294967296}]}
                """)));

        var task = List.builder()
            .host(Property.ofValue(baseUrl()))
            .node(Property.ofValue(NODE))
            .username(Property.ofValue("root@pam"))
            .password(Property.ofValue("secret"))
            .build();

        var output = task.run(runContextFactory.of());

        assertThat(output.vms(), hasSize(1));
        assertThat(output.vms().getFirst().name(), is("test-vm"));
    }
}
