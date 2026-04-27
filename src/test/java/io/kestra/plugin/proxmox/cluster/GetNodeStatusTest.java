package io.kestra.plugin.proxmox.cluster;

import io.kestra.core.models.property.Property;
import io.kestra.plugin.proxmox.AbstractProxmoxTest;
import org.junit.jupiter.api.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

class GetNodeStatusTest extends AbstractProxmoxTest {

    @Test
    void getNodeStatus() throws Exception {
        wireMock.stubFor(get(urlEqualTo("/api2/json/nodes/pve/status"))
            .willReturn(okJson("""
                {"data":{"uptime":86400,"cpu":0.05,"memory":2147483648,"maxmem":8589934592,"pveversion":"pve-manager/8.0"}}
                """)));

        var task = GetNodeStatus.builder()
            .host(Property.ofValue(baseUrl()))
            .node(Property.ofValue(NODE))
            .username(Property.ofValue("root@pam"))
            .password(Property.ofValue("secret"))
            .build();

        var output = task.run(runContextFactory.of());

        assertThat(output.status(), notNullValue());
        assertThat(output.status().uptime(), is(86400L));
    }
}
