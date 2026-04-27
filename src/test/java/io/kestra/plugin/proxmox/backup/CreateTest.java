package io.kestra.plugin.proxmox.backup;

import io.kestra.core.models.property.Property;
import io.kestra.plugin.proxmox.AbstractProxmoxTest;
import org.junit.jupiter.api.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

class CreateTest extends AbstractProxmoxTest {

    @Test
    void createBackup() throws Exception {
        wireMock.stubFor(post(urlEqualTo("/api2/json/nodes/pve/vzdump"))
            .willReturn(okJson("""
                {"data":"UPID:pve:00001234:ABCDEF01:65A1B2C3:vzdump:100:root@pam:"}
                """)));

        var task = Create.builder()
            .host(Property.ofValue(baseUrl()))
            .node(Property.ofValue(NODE))
            .username(Property.ofValue("root@pam"))
            .password(Property.ofValue("secret"))
            .vmName(Property.ofValue(VM_NAME))
            .storage(Property.ofValue("local"))
            .build();

        var output = task.run(runContextFactory.of());

        assertThat(output.getVmId(), is(String.valueOf(VMID)));
        assertThat(output.getUpid(), notNullValue());
    }
}
