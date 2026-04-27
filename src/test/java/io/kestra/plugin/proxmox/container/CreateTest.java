package io.kestra.plugin.proxmox.container;

import io.kestra.core.models.property.Property;
import io.kestra.plugin.proxmox.AbstractProxmoxTest;
import org.junit.jupiter.api.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

class CreateTest extends AbstractProxmoxTest {

    @Test
    void createContainer() throws Exception {
        wireMock.stubFor(post(urlEqualTo("/api2/json/nodes/pve/lxc"))
            .willReturn(okJson("""
                {"data":"UPID:pve:00001234:ABCDEF01:65A1B2C3:vzcreate:200:root@pam:"}
                """)));

        var task = Create.builder()
            .host(Property.ofValue(baseUrl()))
            .node(Property.ofValue(NODE))
            .username(Property.ofValue("root@pam"))
            .password(Property.ofValue("secret"))
            .vmId(Property.ofValue(CT_ID))
            .osTemplate(Property.ofValue("local:vztmpl/debian-12-standard_12.7-1_amd64.tar.zst"))
            .hostname(Property.ofValue("my-container"))
            .build();

        var output = task.run(runContextFactory.of());

        assertThat(output.getVmId(), is(String.valueOf(CT_ID)));
        assertThat(output.getUpid(), notNullValue());
    }
}
