package io.kestra.plugin.proxmox.backup;

import io.kestra.core.models.property.Property;
import io.kestra.plugin.proxmox.AbstractProxmoxTest;
import org.junit.jupiter.api.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

class RestoreTest extends AbstractProxmoxTest {

    @Test
    void restoreBackup() throws Exception {
        wireMock.stubFor(post(urlEqualTo("/api2/json/nodes/pve/qemu"))
            .willReturn(okJson("""
                {"data":"UPID:pve:00001234:ABCDEF01:65A1B2C3:qmrestore:100:root@pam:"}
                """)));

        var task = Restore.builder()
            .host(Property.ofValue(baseUrl()))
            .node(Property.ofValue(NODE))
            .username(Property.ofValue("root@pam"))
            .password(Property.ofValue("secret"))
            .vmId(Property.ofValue(100))
            .archive(Property.ofValue("local:backup/vzdump-qemu-100.vma.zst"))
            .storage(Property.ofValue("local-lvm"))
            .build();

        var output = task.run(runContextFactory.of());

        assertThat(output.getVmId(), is("100"));
        assertThat(output.getUpid(), notNullValue());
    }
}
