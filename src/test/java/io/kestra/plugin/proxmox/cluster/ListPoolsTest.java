package io.kestra.plugin.proxmox.cluster;

import io.kestra.core.models.property.Property;
import io.kestra.plugin.proxmox.AbstractProxmoxTest;
import org.junit.jupiter.api.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

class ListPoolsTest extends AbstractProxmoxTest {

    @Test
    void listPools() throws Exception {
        wireMock.stubFor(get(urlEqualTo("/api2/json/pools"))
            .willReturn(okJson("""
                {"data":[{"poolid":"prod","comment":"Production pool"}]}
                """)));

        var task = ListPools.builder()
            .host(Property.ofValue(baseUrl()))
            .node(Property.ofValue(NODE))
            .username(Property.ofValue("root@pam"))
            .password(Property.ofValue("secret"))
            .build();

        var output = task.run(runContextFactory.of());

        assertThat(output.pools(), hasSize(1));
        assertThat(output.pools().getFirst().poolid(), is("prod"));
    }
}
