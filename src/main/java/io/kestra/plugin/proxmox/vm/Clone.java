package io.kestra.plugin.proxmox.vm;

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
    title = "Clone a QEMU virtual machine on Proxmox VE",
    description = """
        Clones an existing QEMU VM to a new VMID via POST /nodes/{node}/qemu/{vmid}/clone.
        Set full=true to create a full independent clone instead of a linked clone.
        """
)
@Plugin(
    examples = {
        @Example(
            title = "Clone a VM",
            full = true,
            code = """
                id: clone_vm
                namespace: company.team

                tasks:
                  - id: clone
                    type: io.kestra.plugin.proxmox.vm.Clone
                    host: "{{ secret('PROXMOX_HOST') }}"
                    username: "{{ secret('PROXMOX_USERNAME') }}"
                    password: "{{ secret('PROXMOX_PASSWORD') }}"
                    node: pve
                    vmName: template-vm
                    newId: 201
                    newName: cloned-vm
                    full: true
                """
        )
    }
)
public class Clone extends AbstractTask<AbstractTask.Output> {

    @Schema(title = "Source VM name or ID")
    @NotNull
    @PluginProperty(group = "main")
    private Property<String> vmName;

    @Schema(title = "New VM ID", description = "VMID to assign to the clone.")
    @NotNull
    @PluginProperty(group = "main")
    private Property<Integer> newId;

    @Schema(title = "New VM name")
    @PluginProperty(group = "main")
    private Property<String> newName;

    @Schema(title = "Target node", description = "Destination node for the clone. Defaults to the same node as the source.")
    @PluginProperty(group = "advanced")
    private Property<String> targetNode;

    @Schema(title = "Full clone", description = "Set true to create a full independent clone. Defaults to false (linked clone).")
    @Builder.Default
    @PluginProperty(group = "advanced")
    private Property<Boolean> full = Property.ofValue(false);

    @Override
    public AbstractTask.Output run(RunContext runContext) throws Exception {
        var logger = runContext.logger();
        var rNode = runContext.render(node).as(String.class).orElseThrow();
        var rVmName = runContext.render(vmName).as(String.class).orElseThrow();
        var rNewId = runContext.render(newId).as(Integer.class).orElseThrow();
        var rNewName = runContext.render(newName).as(String.class).orElse(null);
        var rTargetNode = runContext.render(targetNode).as(String.class).orElse(null);
        var rFull = runContext.render(full).as(Boolean.class).orElse(false);

        try (var client = createClient(runContext)) {
            var vmid = client.resolveVmId(rVmName);
            var params = new LinkedHashMap<String, String>();
            params.put("newid", String.valueOf(rNewId));
            if (rNewName != null) {
                params.put("name", rNewName);
            }
            if (rTargetNode != null) {
                params.put("target", rTargetNode);
            }
            params.put("full", rFull ? "1" : "0");

            logger.info("Cloning VM {} (vmid={}) to vmid={}", rVmName, vmid, rNewId);
            var upid = client.postAndWait("/nodes/" + URLEncoder.encode(rNode, StandardCharsets.UTF_8) + "/qemu/" + vmid + "/clone", params);
            logger.info("Clone completed: vmid={}", rNewId);
            return AbstractTask.Output.of(String.valueOf(rNewId), rNewName != null ? rNewName : rVmName + "-clone", upid);
        }
    }
}
