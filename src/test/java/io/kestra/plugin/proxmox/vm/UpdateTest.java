package io.kestra.plugin.proxmox.vm;

import io.kestra.core.models.property.Property;
import io.kestra.plugin.proxmox.AbstractProxmoxTest;
import org.junit.jupiter.api.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

class UpdateTest extends AbstractProxmoxTest {

    @Test
    void updateVm() throws Exception {
        wireMock.stubFor(put(urlEqualTo("/api2/json/nodes/pve/qemu/100/config"))
            .willReturn(okJson("""
                {"data":null}
                """)));

        var task = Update.builder()
            .host(Property.ofValue(baseUrl()))
            .node(Property.ofValue(NODE))
            .username(Property.ofValue("root@pam"))
            .password(Property.ofValue("secret"))
            .vmName(Property.ofValue(VM_NAME))
            .cores(Property.ofValue(2))
            .memory(Property.ofValue(2048))
            .build();

        // Update returns VoidOutput (null) — verify the API endpoint was called
        task.run(runContextFactory.of());

        wireMock.verify(putRequestedFor(urlEqualTo("/api2/json/nodes/pve/qemu/100/config")));
    }
}
