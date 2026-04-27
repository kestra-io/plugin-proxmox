package io.kestra.plugin.proxmox.vm;

import io.kestra.core.models.property.Property;
import io.kestra.plugin.proxmox.AbstractProxmoxTest;
import org.junit.jupiter.api.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

class MigrateTest extends AbstractProxmoxTest {

    @Test
    void migrateVm() throws Exception {
        wireMock.stubFor(post(urlEqualTo("/api2/json/nodes/pve/qemu/100/migrate"))
            .willReturn(okJson("""
                {"data":"UPID:pve:00001234:ABCDEF01:65A1B2C3:qmmigrate:100:root@pam:"}
                """)));

        var task = Migrate.builder()
            .host(Property.ofValue(baseUrl()))
            .node(Property.ofValue(NODE))
            .username(Property.ofValue("root@pam"))
            .password(Property.ofValue("secret"))
            .vmName(Property.ofValue(VM_NAME))
            .targetNode(Property.ofValue("pve2"))
            .build();

        var output = task.run(runContextFactory.of());

        assertThat(output.getVmId(), is(String.valueOf(VMID)));
        assertThat(output.getUpid(), notNullValue());
    }
}
