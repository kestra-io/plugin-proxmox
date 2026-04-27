package io.kestra.plugin.proxmox.vm;

import io.kestra.core.models.property.Property;
import io.kestra.plugin.proxmox.AbstractProxmoxTest;
import org.junit.jupiter.api.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

class CreateTest extends AbstractProxmoxTest {

    @Test
    void createVm() throws Exception {
        wireMock.stubFor(post(urlEqualTo("/api2/json/nodes/pve/qemu"))
            .willReturn(okJson("""
                {"data":"UPID:pve:00001234:ABCDEF01:65A1B2C3:qmcreate:200:root@pam:"}
                """)));

        var task = Create.builder()
            .host(Property.ofValue(baseUrl()))
            .node(Property.ofValue(NODE))
            .username(Property.ofValue("root@pam"))
            .password(Property.ofValue("secret"))
            .vmId(Property.ofValue(200))
            .vmName(Property.ofValue("kestra-test-vm"))
            .cores(Property.ofValue(1))
            .memory(Property.ofValue(512))
            .disk(Property.ofValue("local-lvm:4"))
            .net(Property.ofValue("virtio,bridge=vmbr0"))
            .powerOn(Property.ofValue(false))
            .build();

        var output = task.run(runContextFactory.of());

        assertThat(output.getVmId(), is("200"));
        assertThat(output.getVmName(), is("kestra-test-vm"));
    }
}
