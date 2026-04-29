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

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
@Schema(
    title = "Migrate an LXC container to another Proxmox VE node",
    description = """
        Migrates an LXC container to a target node via POST /nodes/{node}/lxc/{vmid}/migrate
        and waits for the task to complete.
        """
)
@Plugin(
    examples = {
        @Example(
            title = "Migrate a container to another node",
            full = true,
            code = """
                id: migrate_container
                namespace: company.team

                tasks:
                  - id: migrate
                    type: io.kestra.plugin.proxmox.container.Migrate
                    host: "{{ secret('PROXMOX_HOST') }}"
                    username: "{{ secret('PROXMOX_USERNAME') }}"
                    password: "{{ secret('PROXMOX_PASSWORD') }}"
                    node: pve
                    vmName: "300"
                    targetNode: pve2
                """
        )
    }
)
public class Migrate extends AbstractTask<AbstractTask.Output> {

    @Schema(title = "Container name or ID", description = "Container name (resolved at runtime) or integer VMID.")
    @NotNull
    @PluginProperty(group = "main")
    private Property<String> vmName;

    @Schema(title = "Target node", description = "Name of the destination Proxmox node.")
    @NotNull
    @PluginProperty(group = "main")
    private Property<String> targetNode;

    @Override
    public AbstractTask.Output run(RunContext runContext) throws Exception {
        var logger = runContext.logger();
        var rNode = runContext.render(node).as(String.class).orElseThrow();
        var rVmName = runContext.render(vmName).as(String.class).orElseThrow();
        var rTargetNode = runContext.render(targetNode).as(String.class).orElseThrow();

        try (var client = createClient(runContext)) {
            var vmid = client.resolveVmId(rVmName);
            var params = new LinkedHashMap<String, String>();
            params.put("target", rTargetNode);

            logger.info("Migrating container {} (vmid={}) to node '{}'", rVmName, vmid, rTargetNode);
            var upid = client.postAndWait("/nodes/" + URLEncoder.encode(rNode, StandardCharsets.UTF_8) + "/lxc/" + vmid + "/migrate", params);
            logger.info("Container {} migrated to '{}'", rVmName, rTargetNode);
            return AbstractTask.Output.of(String.valueOf(vmid), rVmName, upid);
        }
    }
}
