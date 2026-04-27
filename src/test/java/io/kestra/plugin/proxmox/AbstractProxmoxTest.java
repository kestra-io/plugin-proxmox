package io.kestra.plugin.proxmox;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import io.kestra.core.junit.annotations.KestraTest;
import io.kestra.core.runners.RunContextFactory;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.RegisterExtension;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

@KestraTest
public abstract class AbstractProxmoxTest {

    protected static final String NODE = "pve";
    protected static final String VM_NAME = "test-vm";
    protected static final int VMID = 100;
    protected static final String UPID = "UPID:pve:00001234:ABCDEF01:65A1B2C3:qmstart:100:root@pam:";
    protected static final String CT_NAME = "test-ct";
    protected static final int CT_ID = 200;

    @RegisterExtension
    protected static WireMockExtension wireMock = WireMockExtension.newInstance()
        .options(wireMockConfig().dynamicPort())
        .build();

    @Inject
    protected RunContextFactory runContextFactory;

    @BeforeEach
    void stubCommon() {
        // Ticket auth
        wireMock.stubFor(post(urlEqualTo("/api2/json/access/ticket"))
            .willReturn(okJson("""
                {"data":{"ticket":"PVE:root@pam:test","CSRFPreventionToken":"test-csrf","username":"root@pam"}}
                """)));

        // VMID resolution via cluster resources
        wireMock.stubFor(get(urlPathEqualTo("/api2/json/cluster/resources"))
            .willReturn(okJson("""
                {"data":[
                  {"vmid":100,"name":"test-vm","status":"running","type":"qemu","node":"pve"},
                  {"vmid":200,"name":"test-ct","status":"running","type":"lxc","node":"pve"}
                ]}
                """)));

        // Task status — always immediately done
        wireMock.stubFor(get(urlMatching("/api2/json/nodes/.*/tasks/.*/status"))
            .willReturn(okJson("""
                {"data":{"status":"stopped","exitstatus":"OK","upid":"%s"}}
                """.formatted(UPID))));
    }

    protected String baseUrl() {
        return "http://localhost:" + wireMock.getPort();
    }
}
