package io.kestra.plugin.proxmox.backup;

import io.kestra.core.models.property.Property;
import io.kestra.plugin.proxmox.AbstractProxmoxTest;
import org.junit.jupiter.api.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

class ListTest extends AbstractProxmoxTest {

    @Test
    void listBackups() throws Exception {
        wireMock.stubFor(get(urlPathEqualTo("/api2/json/nodes/pve/storage/local/content"))
            .withQueryParam("content", equalTo("backup"))
            .willReturn(okJson("""
                {"data":[{"volid":"local:backup/vzdump-qemu-100.vma.zst","content":"backup","size":1073741824}]}
                """)));

        var task = List.builder()
            .host(Property.ofValue(baseUrl()))
            .node(Property.ofValue(NODE))
            .username(Property.ofValue("root@pam"))
            .password(Property.ofValue("secret"))
            .storage(Property.ofValue("local"))
            .build();

        var output = task.run(runContextFactory.of());

        assertThat(output.backups(), hasSize(1));
        assertThat(output.backups().getFirst().volid(), is("local:backup/vzdump-qemu-100.vma.zst"));
    }
}
