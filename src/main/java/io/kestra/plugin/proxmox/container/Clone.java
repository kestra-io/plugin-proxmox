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
    title = "Clone an LXC container on Proxmox VE",
    description = "Clones an existing LXC container to a new VMID via POST /nodes/{node}/lxc/{vmid}/clone."
)
@Plugin(
    examples = {
        @Example(
            title = "Clone a container",
            full = true,
            code = """
                id: clone_container
                namespace: company.team

                tasks:
                  - id: clone
                    type: io.kestra.plugin.proxmox.container.Clone
                    host: "{{ secret('PROXMOX_HOST') }}"
                    username: "{{ secret('PROXMOX_USERNAME') }}"
                    password: "{{ secret('PROXMOX_PASSWORD') }}"
                    node: pve
                    vmName: "300"
                    newId: 301
                    newName: cloned-container
                """
        )
    }
)
public class Clone extends AbstractTask<AbstractTask.Output> {

    @Schema(title = "Source container name or ID", description = "Container name (resolved at runtime) or integer VMID.")
    @NotNull
    @PluginProperty(group = "main")
    private Property<String> vmName;

    @Schema(title = "New container ID", description = "Integer VMID to assign to the clone. Must be unique in the cluster.")
    @NotNull
    @PluginProperty(group = "main")
    private Property<Integer> newId;

    @Schema(title = "New container hostname")
    @PluginProperty(group = "main")
    private Property<String> newName;

    @Override
    public AbstractTask.Output run(RunContext runContext) throws Exception {
        var logger = runContext.logger();
        var rNode = runContext.render(node).as(String.class).orElseThrow();
        var rVmName = runContext.render(vmName).as(String.class).orElseThrow();
        var rNewId = runContext.render(newId).as(Integer.class).orElseThrow();
        var rNewName = runContext.render(newName).as(String.class).orElse(null);

        try (var client = createClient(runContext)) {
            var vmid = client.resolveVmId(rVmName);
            var params = new LinkedHashMap<String, String>();
            params.put("newid", String.valueOf(rNewId));
            if (rNewName != null) {
                params.put("hostname", rNewName);
            }

            logger.info("Cloning container {} (vmid={}) to vmid={}", rVmName, vmid, rNewId);
            var upid = client.postAndWait("/nodes/" + URLEncoder.encode(rNode, StandardCharsets.UTF_8) + "/lxc/" + vmid + "/clone", params);
            logger.info("Container cloned to vmid={}", rNewId);
            return AbstractTask.Output.of(String.valueOf(rNewId), rNewName, upid);
        }
    }
}
