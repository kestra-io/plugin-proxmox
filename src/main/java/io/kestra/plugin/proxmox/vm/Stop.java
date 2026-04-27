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
    title = "Stop a QEMU virtual machine on Proxmox VE",
    description = """
        Issues a power-off (hard stop) command to a QEMU VM and waits for the Proxmox task to complete.
        """
)
@Plugin(
    examples = {
        @Example(
            title = "Stop a VM by name",
            full = true,
            code = """
                id: stop_vm
                namespace: company.team

                tasks:
                  - id: stop
                    type: io.kestra.plugin.proxmox.vm.Stop
                    host: "{{ secret('PROXMOX_HOST') }}"
                    username: "{{ secret('PROXMOX_USERNAME') }}"
                    password: "{{ secret('PROXMOX_PASSWORD') }}"
                    node: pve
                    vmName: my-vm
                """
        )
    }
)
public class Stop extends AbstractTask<AbstractTask.Output> {

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
            logger.info("Stopping VM {} (vmid={})", rVmName, vmid);
            var upid = client.postAndWait("/nodes/" + rNode + "/qemu/" + vmid + "/status/stop", null);
            logger.info("VM {} stopped", rVmName);
            return AbstractTask.Output.of(String.valueOf(vmid), rVmName, upid);
        }
    }
}
