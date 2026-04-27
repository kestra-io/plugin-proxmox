package io.kestra.plugin.proxmox.snapshot;

import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.annotations.PluginProperty;
import io.kestra.core.models.property.Property;
import io.kestra.core.runners.RunContext;
import io.kestra.plugin.proxmox.AbstractTask;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
@Schema(
    title = "Create a snapshot of a QEMU VM or LXC container on Proxmox VE",
    description = """
        Creates a snapshot via POST /nodes/{node}/qemu|lxc/{vmid}/snapshot.
        Set resourceType to vm (default) for QEMU VMs or container for LXC containers.
        """
)
@Plugin(
    examples = {
        @Example(
            title = "Create a VM snapshot",
            full = true,
            code = """
                id: create_snapshot
                namespace: company.team

                tasks:
                  - id: snap
                    type: io.kestra.plugin.proxmox.snapshot.Create
                    host: "{{ secret('PROXMOX_HOST') }}"
                    username: "{{ secret('PROXMOX_USERNAME') }}"
                    password: "{{ secret('PROXMOX_PASSWORD') }}"
                    node: pve
                    vmName: my-vm
                    snapName: before-upgrade
                    description: "Snapshot before OS upgrade"
                """
        )
    }
)
public class Create extends AbstractTask<AbstractTask.Output> {

    @Schema(title = "VM or container name or ID")
    @NotNull
    @PluginProperty(group = "main")
    private Property<String> vmName;

    @Schema(title = "Snapshot name", description = "Alphanumeric identifier for the snapshot.")
    @NotNull
    @PluginProperty(group = "main")
    private Property<String> snapName;

    @Schema(title = "Snapshot description")
    @PluginProperty(group = "advanced")
    private Property<String> snapDescription;

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
        var rDescription = runContext.render(snapDescription).as(String.class).orElse(null);
        var rResourceType = runContext.render(resourceType).as(String.class).orElse("vm");

        var apiSegment = "container".equalsIgnoreCase(rResourceType) ? "lxc" : "qemu";

        try (var client = createClient(runContext)) {
            var vmid = client.resolveVmId(rVmName);
            var params = new LinkedHashMap<String, String>();
            params.put("snapname", rSnapName);
            if (rDescription != null) {
                params.put("description", rDescription);
            }

            logger.info("Creating snapshot '{}' for {} vmid={}", rSnapName, rResourceType, vmid);
            var upid = client.postAndWait("/nodes/" + URLEncoder.encode(rNode, StandardCharsets.UTF_8) + "/" + apiSegment + "/" + vmid + "/snapshot", params);
            logger.info("Snapshot '{}' created", rSnapName);
            return AbstractTask.Output.of(String.valueOf(vmid), rVmName, upid);
        }
    }
}
