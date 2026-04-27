package io.kestra.plugin.proxmox.vm;

import io.kestra.core.models.property.Property;
import io.kestra.plugin.proxmox.AbstractProxmoxTest;
import org.junit.jupiter.api.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

class CloneTest extends AbstractProxmoxTest {

    @Test
    void cloneVm() throws Exception {
        wireMock.stubFor(post(urlEqualTo("/api2/json/nodes/pve/qemu/100/clone"))
            .willReturn(okJson("""
                {"data":"UPID:pve:00001234:ABCDEF01:65A1B2C3:qmclone:100:root@pam:"}
                """)));

        var task = Clone.builder()
            .host(Property.ofValue(baseUrl()))
            .node(Property.ofValue(NODE))
            .username(Property.ofValue("root@pam"))
            .password(Property.ofValue("secret"))
            .vmName(Property.ofValue(VM_NAME))
            .newId(Property.ofValue(201))
            .build();

        var output = task.run(runContextFactory.of());

        assertThat(output.getVmId(), is("201"));
        assertThat(output.getUpid(), notNullValue());
    }
}
