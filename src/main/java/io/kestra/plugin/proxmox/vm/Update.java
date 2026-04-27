package io.kestra.plugin.proxmox.vm;

import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.annotations.PluginProperty;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.VoidOutput;
import io.kestra.core.runners.RunContext;
import io.kestra.plugin.proxmox.AbstractTask;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.util.LinkedHashMap;
import java.util.Map;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
@Schema(
    title = "Update configuration of a QEMU virtual machine on Proxmox VE",
    description = """
        Sends a PUT to /nodes/{node}/qemu/{vmid}/config to update VM configuration parameters such as cores or memory.
        Additional raw config key/value pairs can be supplied via the config map.
        """
)
@Plugin(
    examples = {
        @Example(
            title = "Update VM cores and memory",
            full = true,
            code = """
                id: update_vm
                namespace: company.team

                tasks:
                  - id: update
                    type: io.kestra.plugin.proxmox.vm.Update
                    host: "{{ secret('PROXMOX_HOST') }}"
                    username: "{{ secret('PROXMOX_USERNAME') }}"
                    password: "{{ secret('PROXMOX_PASSWORD') }}"
                    node: pve
                    vmName: my-vm
                    cores: 4
                    memory: 8192
                """
        )
    }
)
public class Update extends AbstractTask<VoidOutput> {

    @Schema(title = "VM name or ID")
    @NotNull
    @PluginProperty(group = "main")
    private Property<String> vmName;

    @Schema(title = "Number of CPU cores")
    @PluginProperty(group = "resources")
    private Property<Integer> cores;

    @Schema(title = "Memory in MiB")
    @PluginProperty(group = "resources")
    private Property<Integer> memory;

    @Schema(
        title = "Additional config parameters",
        description = "Raw Proxmox config key/value pairs to pass directly to the API (e.g. description, balloon, etc.)."
    )
    @PluginProperty(group = "advanced")
    private Property<Map<String, Object>> config;

    @Override
    public VoidOutput run(RunContext runContext) throws Exception {
        var logger = runContext.logger();
        var rNode = runContext.render(node).as(String.class).orElseThrow();
        var rVmName = runContext.render(vmName).as(String.class).orElseThrow();

        try (var client = createClient(runContext)) {
            var vmid = client.resolveVmId(rVmName);
            var params = new LinkedHashMap<String, String>();

            runContext.render(cores).as(Integer.class).ifPresent(v -> params.put("cores", String.valueOf(v)));
            runContext.render(memory).as(Integer.class).ifPresent(v -> params.put("memory", String.valueOf(v)));

            var rConfig = runContext.render(config).asMap(String.class, Object.class);
            rConfig.forEach((k, v) -> params.put(k, String.valueOf(v)));

            logger.info("Updating VM {} (vmid={}) config: {}", rVmName, vmid, params.keySet());
            client.put("/nodes/" + rNode + "/qemu/" + vmid + "/config", params);
            logger.info("VM {} config updated", rVmName);
        }

        return new VoidOutput();
    }
}
