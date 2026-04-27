package io.kestra.plugin.proxmox.vm;

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
    title = "Reboot a QEMU virtual machine on Proxmox VE",
    description = "Sends a graceful reboot request to a QEMU VM and waits for the Proxmox task to complete."
)
@Plugin(
    examples = {
        @Example(
            title = "Reboot a VM by name",
            full = true,
            code = """
                id: reboot_vm
                namespace: company.team

                tasks:
                  - id: reboot
                    type: io.kestra.plugin.proxmox.vm.Reboot
                    host: "{{ secret('PROXMOX_HOST') }}"
                    username: "{{ secret('PROXMOX_USERNAME') }}"
                    password: "{{ secret('PROXMOX_PASSWORD') }}"
                    node: pve
                    vmName: my-vm
                """
        )
    }
)
public class Reboot extends AbstractTask<AbstractTask.Output> {

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
            logger.info("Rebooting VM {} (vmid={})", rVmName, vmid);
            var upid = client.postAndWait("/nodes/" + URLEncoder.encode(rNode, StandardCharsets.UTF_8) + "/qemu/" + vmid + "/status/reboot", null);
            logger.info("VM {} rebooted", rVmName);
            return AbstractTask.Output.of(String.valueOf(vmid), rVmName, upid);
        }
    }
}
