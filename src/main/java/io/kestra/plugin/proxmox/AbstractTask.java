package io.kestra.plugin.proxmox;

import io.kestra.core.models.annotations.PluginProperty;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.RunnableTask;
import io.kestra.core.models.tasks.Task;
import io.kestra.core.runners.RunContext;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@SuperBuilder(toBuilder = true)
@ToString
@EqualsAndHashCode
@NoArgsConstructor
@Getter
public abstract class AbstractTask<T extends io.kestra.core.models.tasks.Output> extends Task implements RunnableTask<T> {

    static final int DEFAULT_PORT = 8006;

    @Schema(
        title = "Proxmox host",
        description = "Hostname or IP address of the Proxmox VE node (no scheme, no port)."
    )
    @NotNull
    @PluginProperty(group = "connection")
    protected Property<String> host;

    @Schema(
        title = "API port",
        description = "Defaults to 8006."
    )
    @Builder.Default
    @PluginProperty(group = "connection")
    protected Property<Integer> port = Property.ofValue(DEFAULT_PORT);

    @Schema(
        title = "Proxmox node name",
        description = "Name of the cluster node that scopes all API calls (e.g. pve, node1)."
    )
    @NotNull
    @PluginProperty(group = "connection")
    protected Property<String> node;

    @Schema(
        title = "Username",
        description = "PAM or PVE user in the form user@realm (e.g. root@pam). Required when using ticket-based auth."
    )
    @PluginProperty(group = "connection")
    protected Property<String> username;

    @Schema(
        title = "Password",
        description = "Password for ticket-based authentication."
    )
    @PluginProperty(group = "connection", secret = true)
    protected Property<String> password;

    @Schema(
        title = "API token ID",
        description = "Full token identifier in the form user@realm!tokenname (e.g. root@pam!mytoken). Use together with tokenSecret."
    )
    @PluginProperty(group = "connection")
    protected Property<String> tokenId;

    @Schema(
        title = "API token secret",
        description = "The UUID secret associated with the token ID."
    )
    @PluginProperty(group = "connection", secret = true)
    protected Property<String> tokenSecret;

    @Schema(
        title = "Verify SSL",
        description = "Validate the server TLS certificate. Defaults to false because Proxmox nodes commonly use self-signed certificates."
    )
    @Builder.Default
    @PluginProperty(group = "advanced")
    protected Property<Boolean> verifySsl = Property.ofValue(false);

    protected ProxmoxClient createClient(RunContext runContext) throws Exception {
        return ClientFactory.create(
            runContext.render(host).as(String.class).orElseThrow(),
            runContext.render(port).as(Integer.class).orElse(DEFAULT_PORT),
            runContext.render(node).as(String.class).orElseThrow(),
            runContext.render(username).as(String.class).orElse(null),
            runContext.render(password).as(String.class).orElse(null),
            runContext.render(tokenId).as(String.class).orElse(null),
            runContext.render(tokenSecret).as(String.class).orElse(null),
            runContext.render(verifySsl).as(Boolean.class).orElse(false),
            runContext
        );
    }

    @Builder
    @Getter
    public static class Output implements io.kestra.core.models.tasks.Output {

        @Schema(title = "VM or container ID", description = "Integer VMID as a string.")
        private final String vmId;

        @Schema(title = "VM or container name")
        private final String vmName;

        @Schema(title = "Proxmox task UPID", description = "Unique task identifier returned by mutating API calls.")
        private final String upid;

        public static Output of(String vmId, String vmName, String upid) {
            return Output.builder()
                .vmId(vmId)
                .vmName(vmName)
                .upid(upid)
                .build();
        }
    }
}
