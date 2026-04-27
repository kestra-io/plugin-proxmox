package io.kestra.plugin.proxmox.container;

import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.annotations.PluginProperty;
import io.kestra.core.models.property.Property;
import io.kestra.core.runners.RunContext;
import io.kestra.plugin.proxmox.AbstractTask;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
@Schema(
    title = "Start an LXC container on Proxmox VE",
    description = "Issues a start command to an LXC container and waits for the Proxmox task to complete."
)
@Plugin(
    examples = {
        @Example(
            title = "Start a container by ID",
            full = true,
            code = """
                id: start_container
                namespace: company.team

                tasks:
                  - id: start
                    type: io.kestra.plugin.proxmox.container.Start
                    host: "{{ secret('PROXMOX_HOST') }}"
                    username: "{{ secret('PROXMOX_USERNAME') }}"
                    password: "{{ secret('PROXMOX_PASSWORD') }}"
                    node: pve
                    vmName: "300"
                """
        )
    }
)
public class Start extends AbstractTask<AbstractTask.Output> {

    @Schema(title = "Container name or ID", description = "Container name (resolved at runtime) or integer VMID.")
    @NotNull
    @PluginProperty(group = "main")
    private Property<String> vmName;

    @Override
    public AbstractTask.Output run(RunContext runContext) throws Exception {
        var logger = runContext.logger();
        var rNode = runContext.render(node).as(String.class).orElseThrow();
        var rVmName = runContext.render(vmName).as(String.class).orElseThrow();

        try (var client = createClient(runContext)) {
            var vmid = client.resolveVmId(rVmName);
            logger.info("Starting container {} (vmid={})", rVmName, vmid);
            var upid = client.postAndWait("/nodes/" + rNode + "/lxc/" + vmid + "/status/start", null);
            logger.info("Container {} started", rVmName);
            return AbstractTask.Output.of(String.valueOf(vmid), rVmName, upid);
        }
    }
}
