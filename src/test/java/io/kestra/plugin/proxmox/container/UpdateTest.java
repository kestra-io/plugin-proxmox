package io.kestra.plugin.proxmox.container;

import io.kestra.core.models.property.Property;
import io.kestra.plugin.proxmox.AbstractProxmoxTest;
import org.junit.jupiter.api.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

class UpdateTest extends AbstractProxmoxTest {

    @Test
    void updateContainer() throws Exception {
        wireMock.stubFor(put(urlEqualTo("/api2/json/nodes/pve/lxc/200/config"))
            .willReturn(okJson("""
                {"data":null}
                """)));

        var task = Update.builder()
            .host(Property.ofValue(baseUrl()))
            .node(Property.ofValue(NODE))
            .username(Property.ofValue("root@pam"))
            .password(Property.ofValue("secret"))
            .vmName(Property.ofValue(CT_NAME))
            .cores(Property.ofValue(2))
            .memory(Property.ofValue(1024))
            .build();

        // Update returns VoidOutput (null) — verify the API endpoint was called
        task.run(runContextFactory.of());

        wireMock.verify(putRequestedFor(urlEqualTo("/api2/json/nodes/pve/lxc/200/config")));
    }
}
