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
    title = "Reset a QEMU virtual machine on Proxmox VE",
    description = "Performs a hard reset (equivalent to pressing the physical reset button) on a QEMU VM."
)
@Plugin(
    examples = {
        @Example(
            title = "Hard-reset a VM by name",
            full = true,
            code = """
                id: reset_vm
                namespace: company.team

                tasks:
                  - id: reset
                    type: io.kestra.plugin.proxmox.vm.Reset
                    host: "{{ secret('PROXMOX_HOST') }}"
                    username: "{{ secret('PROXMOX_USERNAME') }}"
                    password: "{{ secret('PROXMOX_PASSWORD') }}"
                    node: pve
                    vmName: my-vm
                """
        )
    }
)
public class Reset extends AbstractTask<AbstractTask.Output> {

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
            logger.info("Resetting VM {} (vmid={})", rVmName, vmid);
            var upid = client.postAndWait("/nodes/" + rNode + "/qemu/" + vmid + "/status/reset", null);
            logger.info("VM {} reset", rVmName);
            return AbstractTask.Output.of(String.valueOf(vmid), rVmName, upid);
        }
    }
}
