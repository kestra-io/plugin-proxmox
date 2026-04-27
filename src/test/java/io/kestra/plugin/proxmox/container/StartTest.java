package io.kestra.plugin.proxmox.container;

import io.kestra.core.models.property.Property;
import io.kestra.plugin.proxmox.AbstractProxmoxTest;
import org.junit.jupiter.api.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

class StartTest extends AbstractProxmoxTest {

    @Test
    void startContainer() throws Exception {
        wireMock.stubFor(post(urlEqualTo("/api2/json/nodes/pve/lxc/200/status/start"))
            .willReturn(okJson("""
                {"data":"UPID:pve:00001234:ABCDEF01:65A1B2C3:vzstart:200:root@pam:"}
                """)));

        var task = Start.builder()
            .host(Property.ofValue(baseUrl()))
            .node(Property.ofValue(NODE))
            .username(Property.ofValue("root@pam"))
            .password(Property.ofValue("secret"))
            .vmName(Property.ofValue(CT_NAME))
            .build();

        var output = task.run(runContextFactory.of());

        assertThat(output.getVmId(), is(String.valueOf(CT_ID)));
        assertThat(output.getUpid(), notNullValue());
    }
}
