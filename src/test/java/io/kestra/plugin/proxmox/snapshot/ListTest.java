package io.kestra.plugin.proxmox.snapshot;

import io.kestra.core.models.property.Property;
import io.kestra.plugin.proxmox.AbstractProxmoxTest;
import org.junit.jupiter.api.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

class ListTest extends AbstractProxmoxTest {

    @Test
    void listSnapshots() throws Exception {
        wireMock.stubFor(get(urlEqualTo("/api2/json/nodes/pve/qemu/100/snapshot"))
            .willReturn(okJson("""
                {"data":[{"name":"snap1","description":"test snap","snaptime":1700000000}]}
                """)));

        var task = List.builder()
            .host(Property.ofValue(baseUrl()))
            .node(Property.ofValue(NODE))
            .username(Property.ofValue("root@pam"))
            .password(Property.ofValue("secret"))
            .vmName(Property.ofValue(VM_NAME))
            .build();

        var output = task.run(runContextFactory.of());

        assertThat(output.snapshots(), hasSize(1));
        assertThat(output.snapshots().getFirst().name(), is("snap1"));
    }
}
