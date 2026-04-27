package io.kestra.plugin.proxmox.snapshot;

import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.annotations.PluginProperty;
import io.kestra.core.models.property.Property;
import io.kestra.core.runners.RunContext;
import io.kestra.plugin.proxmox.AbstractTask;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import lombok.Builder;
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
    title = "Roll back a QEMU VM or LXC container to a snapshot on Proxmox VE",
    description = """
        Issues a rollback via POST /nodes/{node}/qemu|lxc/{vmid}/snapshot/{snapname}/rollback
        and waits for the task to complete.
        """
)
@Plugin(
    examples = {
        @Example(
            title = "Roll back a VM to a snapshot",
            full = true,
            code = """
                id: rollback_snapshot
                namespace: company.team

                tasks:
                  - id: rollback
                    type: io.kestra.plugin.proxmox.snapshot.Rollback
                    host: "{{ secret('PROXMOX_HOST') }}"
                    username: "{{ secret('PROXMOX_USERNAME') }}"
                    password: "{{ secret('PROXMOX_PASSWORD') }}"
                    node: pve
                    vmName: my-vm
                    snapName: before-upgrade
                """
        )
    }
)
public class Rollback extends AbstractTask<AbstractTask.Output> {

    @Schema(title = "VM or container name or ID")
    @NotNull
    @PluginProperty(group = "main")
    private Property<String> vmName;

    @Schema(title = "Snapshot name to roll back to")
    @NotNull
    @PluginProperty(group = "main")
    private Property<String> snapName;

    @Schema(title = "Resource type", description = "vm for QEMU VMs, container for LXC containers. Defaults to vm.")
    @Builder.Default
    @PluginProperty(group = "main")
    private Property<String> resourceType = Property.ofValue("vm");

    @Override
    public AbstractTask.Output run(RunContext runContext) throws Exception {
        var logger = runContext.logger();
        var rNode = runContext.render(node).as(String.class).orElseThrow();
        var rVmName = runContext.render(vmName).as(String.class).orElseThrow();
        var rSnapName = runContext.render(snapName).as(String.class).orElseThrow();
        var rResourceType = runContext.render(resourceType).as(String.class).orElse("vm");

        var apiSegment = "container".equalsIgnoreCase(rResourceType) ? "lxc" : "qemu";

        try (var client = createClient(runContext)) {
            var vmid = client.resolveVmId(rVmName);
            logger.info("Rolling back {} vmid={} to snapshot '{}'", rResourceType, vmid, rSnapName);
            var upid = client.postAndWait("/nodes/" + URLEncoder.encode(rNode, StandardCharsets.UTF_8) + "/" + apiSegment + "/" + vmid + "/snapshot/" + URLEncoder.encode(rSnapName, StandardCharsets.UTF_8) + "/rollback", null);
            logger.info("Rollback to snapshot '{}' completed", rSnapName);
            return AbstractTask.Output.of(String.valueOf(vmid), rVmName, upid);
        }
    }
}
