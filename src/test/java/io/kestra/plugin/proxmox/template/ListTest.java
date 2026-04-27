package io.kestra.plugin.proxmox.template;

import io.kestra.core.models.property.Property;
import io.kestra.plugin.proxmox.AbstractProxmoxTest;
import org.junit.jupiter.api.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

class ListTest extends AbstractProxmoxTest {

    @Test
    void listTemplates() throws Exception {
        // Override the base cluster/resources stub for the ?type=vm query to return a template entry.
        // WireMock matches more-specific stubs first; using withQueryParam makes this more specific
        // than the urlPathEqualTo stub registered in the base class.
        wireMock.stubFor(get(urlPathEqualTo("/api2/json/cluster/resources"))
            .withQueryParam("type", equalTo("vm"))
            .willReturn(okJson("""
                {"data":[
                  {"vmid":100,"name":"test-vm","status":"stopped","type":"qemu","node":"pve","template":1}
                ]}
                """)));

        var task = List.builder()
            .host(Property.ofValue(baseUrl()))
            .node(Property.ofValue(NODE))
            .username(Property.ofValue("root@pam"))
            .password(Property.ofValue("secret"))
            .build();

        var output = task.run(runContextFactory.of());

        assertThat(output.templates(), hasSize(1));
        assertThat(output.templates().getFirst().name(), is("test-vm"));
    }
}
