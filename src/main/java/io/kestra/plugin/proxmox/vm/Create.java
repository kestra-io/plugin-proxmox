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
import java.util.Map;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
@Schema(
    title = "Create a QEMU virtual machine on Proxmox VE",
    description = """
        Creates a new QEMU VM via POST /nodes/{node}/qemu.
        Defaults: 1 vCPU, 1024 MiB RAM, 8 GiB disk (virtio on local-lvm), bridge net0 on vmbr0.
        Set powerOn to true to start the VM immediately after creation.
        """
)
@Plugin(
    examples = {
        @Example(
            title = "Create a VM and start it",
            full = true,
            code = """
                id: create_vm
                namespace: company.team

                tasks:
                  - id: create
                    type: io.kestra.plugin.proxmox.vm.Create
                    host: "{{ secret('PROXMOX_HOST') }}"
                    username: "{{ secret('PROXMOX_USERNAME') }}"
                    password: "{{ secret('PROXMOX_PASSWORD') }}"
                    node: pve
                    vmId: 200
                    vmName: my-new-vm
                    cores: 2
                    memory: 2048
                    disk: "local-lvm:16"
                    net: "virtio,bridge=vmbr0"
                    powerOn: true
                """
        )
    }
)
public class Create extends AbstractTask<AbstractTask.Output> {

    @Schema(title = "VMID", description = "Integer ID to assign to the new VM. Must be unique in the cluster.")
    @NotNull
    @PluginProperty(group = "main")
    private Property<Integer> vmId;

    @Schema(title = "VM name", description = "Either a VM name (resolved at runtime) or an integer VMID.")
    @NotNull
    @PluginProperty(group = "main")
    private Property<String> vmName;

    @Schema(title = "Number of CPU cores", description = "Defaults to 1.")
    @Builder.Default
    @PluginProperty(group = "advanced")
    private Property<Integer> cores = Property.ofValue(1);

    @Schema(title = "Memory in MiB", description = "Defaults to 1024.")
    @Builder.Default
    @PluginProperty(group = "advanced")
    private Property<Integer> memory = Property.ofValue(1024);

    @Schema(
        title = "Disk specification",
        description = "Proxmox disk string, e.g. local-lvm:8 for an 8 GiB virtio disk on local-lvm storage."
    )
    @Builder.Default
    @PluginProperty(group = "advanced")
    private Property<String> disk = Property.ofValue("local-lvm:8");

    @Schema(
        title = "Network interface specification",
        description = "Proxmox net string, e.g. virtio,bridge=vmbr0."
    )
    @Builder.Default
    @PluginProperty(group = "advanced")
    private Property<String> net = Property.ofValue("virtio,bridge=vmbr0");

    @Schema(
        title = "OS template / ISO",
        description = "Optional path to a cloud-init image or ISO on the node's storage, e.g. local:iso/ubuntu-24.04.iso."
    )
    @PluginProperty(group = "advanced")
    private Property<String> osTemplate;

    @Schema(title = "Start after creation", description = "Defaults to false.")
    @Builder.Default
    @PluginProperty(group = "advanced")
    private Property<Boolean> powerOn = Property.ofValue(false);

    @Override
    public AbstractTask.Output run(RunContext runContext) throws Exception {
        var logger = runContext.logger();
        var rNode = runContext.render(node).as(String.class).orElseThrow();
        var rVmId = runContext.render(vmId).as(Integer.class).orElseThrow();
        var rVmName = runContext.render(vmName).as(String.class).orElseThrow();
        var rCores = runContext.render(cores).as(Integer.class).orElse(1);
        var rMemory = runContext.render(memory).as(Integer.class).orElse(1024);
        var rDisk = runContext.render(disk).as(String.class).orElse("local-lvm:8");
        var rNet = runContext.render(net).as(String.class).orElse("virtio,bridge=vmbr0");
        var rOsTemplate = runContext.render(osTemplate).as(String.class).orElse(null);
        var rPowerOn = runContext.render(powerOn).as(Boolean.class).orElse(false);

        try (var client = createClient(runContext)) {
            var params = new LinkedHashMap<String, String>();
            params.put("vmid", String.valueOf(rVmId));
            params.put("name", rVmName);
            params.put("cores", String.valueOf(rCores));
            params.put("memory", String.valueOf(rMemory));
            params.put("virtio0", rDisk);
            params.put("net0", rNet);
            if (rOsTemplate != null) {
                params.put("cdrom", rOsTemplate);
            }

            logger.info("Creating VM '{}' (vmid={}) on node '{}'", rVmName, rVmId, rNode);
            var encodedNode = URLEncoder.encode(rNode, StandardCharsets.UTF_8);
            var upid = client.postAndWait("/nodes/" + encodedNode + "/qemu", params);
            logger.info("VM '{}' created", rVmName);

            if (rPowerOn) {
                logger.info("Starting VM '{}' (vmid={})", rVmName, rVmId);
                client.postAndWait("/nodes/" + encodedNode + "/qemu/" + rVmId + "/status/start", null);
                logger.info("VM '{}' started", rVmName);
            }

            return AbstractTask.Output.of(String.valueOf(rVmId), rVmName, upid);
        }
    }
}
