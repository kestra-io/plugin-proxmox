package io.kestra.plugin.proxmox.template;

import io.kestra.core.models.property.Property;
import io.kestra.plugin.proxmox.AbstractProxmoxTest;
import org.junit.jupiter.api.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

class CreateTest extends AbstractProxmoxTest {

    @Test
    void createTemplate() throws Exception {
        wireMock.stubFor(post(urlEqualTo("/api2/json/nodes/pve/qemu/100/template"))
            .willReturn(okJson("""
                {"data":null}
                """)));

        var task = Create.builder()
            .host(Property.ofValue(baseUrl()))
            .node(Property.ofValue(NODE))
            .username(Property.ofValue("root@pam"))
            .password(Property.ofValue("secret"))
            .vmName(Property.ofValue(VM_NAME))
            .build();

        // Create returns VoidOutput (null) — verify the API endpoint was called
        task.run(runContextFactory.of());

        wireMock.verify(postRequestedFor(urlEqualTo("/api2/json/nodes/pve/qemu/100/template")));
    }
}
