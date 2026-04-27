package io.kestra.plugin.proxmox.vm;

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
    title = "Delete a QEMU virtual machine on Proxmox VE",
    description = """
        Permanently deletes a QEMU VM and all its associated disk images.
        The VM must be stopped before deletion.
        """
)
@Plugin(
    examples = {
        @Example(
            title = "Delete a VM by name",
            full = true,
            code = """
                id: delete_vm
                namespace: company.team

                tasks:
                  - id: delete
                    type: io.kestra.plugin.proxmox.vm.Delete
                    host: "{{ secret('PROXMOX_HOST') }}"
                    username: "{{ secret('PROXMOX_USERNAME') }}"
                    password: "{{ secret('PROXMOX_PASSWORD') }}"
                    node: pve
                    vmName: my-vm
                """
        )
    }
)
public class Delete extends AbstractTask<AbstractTask.Output> {

    @Schema(title = "VM name or ID", description = "Either a VM name (resolved at runtime) or an integer VMID.")
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
            logger.info("Deleting VM {} (vmid={})", rVmName, vmid);
            var result = client.delete("/nodes/" + rNode + "/qemu/" + vmid);
            var upid = result.isNull() ? "" : result.asText();
            if (!upid.isBlank()) {
                client.waitForTask(upid);
            }
            logger.info("VM {} deleted", rVmName);
            return AbstractTask.Output.of(String.valueOf(vmid), rVmName, upid);
        }
    }
}
