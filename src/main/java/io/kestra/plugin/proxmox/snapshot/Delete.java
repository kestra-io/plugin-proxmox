package io.kestra.plugin.proxmox.snapshot;

import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.annotations.PluginProperty;
import io.kestra.core.models.property.Property;
import io.kestra.core.runners.RunContext;
import io.kestra.plugin.proxmox.AbstractTask;
import io.kestra.plugin.proxmox.ResourceType;
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
    title = "Delete a snapshot of a QEMU VM or LXC container on Proxmox VE",
    description = """
        Deletes a snapshot via DELETE /nodes/{node}/qemu|lxc/{vmid}/snapshot/{snapname}.
        Set resourceType to vm (default) or container.
        """
)
@Plugin(
    examples = {
        @Example(
            title = "Delete a VM snapshot",
            full = true,
            code = """
                id: delete_snapshot
                namespace: company.team

                tasks:
                  - id: delete
                    type: io.kestra.plugin.proxmox.snapshot.Delete
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
public class Delete extends AbstractTask<AbstractTask.Output> {

    @Schema(title = "VM or container name or ID")
    @NotNull
    @PluginProperty(group = "main")
    private Property<String> vmName;

    @Schema(title = "Snapshot name")
    @NotNull
    @PluginProperty(group = "main")
    private Property<String> snapName;

    @Schema(title = "Resource type", description = "Resource type: vm or container.")
    @Builder.Default
    @PluginProperty(group = "main")
    private Property<ResourceType> resourceType = Property.ofValue(ResourceType.VM);

    @Override
    public AbstractTask.Output run(RunContext runContext) throws Exception {
        var logger = runContext.logger();
        var rNode = runContext.render(node).as(String.class).orElseThrow();
        var rVmName = runContext.render(vmName).as(String.class).orElseThrow();
        var rSnapName = runContext.render(snapName).as(String.class).orElseThrow();
        var rResourceType = runContext.render(resourceType).as(ResourceType.class).orElse(ResourceType.VM);

        var apiSegment = ResourceType.CONTAINER == rResourceType ? "lxc" : "qemu";

        try (var client = createClient(runContext)) {
            var vmid = client.resolveVmId(rVmName);
            logger.info("Deleting snapshot '{}' for {} vmid={}", rSnapName, rResourceType, vmid);
            var upid = client.deleteAndWait("/nodes/" + URLEncoder.encode(rNode, StandardCharsets.UTF_8) + "/" + apiSegment + "/" + vmid + "/snapshot/" + URLEncoder.encode(rSnapName, StandardCharsets.UTF_8));
            logger.info("Snapshot '{}' deleted", rSnapName);
            return AbstractTask.Output.of(String.valueOf(vmid), rVmName, upid);
        }
    }
}
