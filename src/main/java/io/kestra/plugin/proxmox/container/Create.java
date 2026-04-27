package io.kestra.plugin.proxmox.container;

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

import java.util.LinkedHashMap;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
@Schema(
    title = "Create an LXC container on Proxmox VE",
    description = """
        Creates a new LXC container via POST /nodes/{node}/lxc.
        Defaults: 1 vCPU, 512 MiB RAM, rootfs on local-lvm:4, bridge on vmbr0.
        Set powerOn to true to start the container after creation.
        """
)
@Plugin(
    examples = {
        @Example(
            title = "Create a container from a template",
            full = true,
            code = """
                id: create_container
                namespace: company.team

                tasks:
                  - id: create
                    type: io.kestra.plugin.proxmox.container.Create
                    host: "{{ secret('PROXMOX_HOST') }}"
                    username: "{{ secret('PROXMOX_USERNAME') }}"
                    password: "{{ secret('PROXMOX_PASSWORD') }}"
                    node: pve
                    vmId: 300
                    osTemplate: "local:vztmpl/ubuntu-22.04-standard_22.04-1_amd64.tar.zst"
                    hostname: my-container
                    cores: 2
                    memory: 1024
                    rootfs: "local-lvm:8"
                    powerOn: true
                """
        )
    }
)
public class Create extends AbstractTask<AbstractTask.Output> {

    @Schema(title = "Container ID", description = "Integer VMID to assign. Must be unique in the cluster.")
    @NotNull
    @PluginProperty(group = "main")
    private Property<Integer> vmId;

    @Schema(title = "OS template", description = "Storage path to the LXC template, e.g. local:vztmpl/ubuntu-22.04.tar.zst.")
    @NotNull
    @PluginProperty(group = "main")
    private Property<String> osTemplate;

    @Schema(title = "Container hostname")
    @PluginProperty(group = "main")
    private Property<String> hostname;

    @Schema(title = "Number of CPU cores", description = "Defaults to 1.")
    @Builder.Default
    @PluginProperty(group = "resources")
    private Property<Integer> cores = Property.ofValue(1);

    @Schema(title = "Memory in MiB", description = "Defaults to 512.")
    @Builder.Default
    @PluginProperty(group = "resources")
    private Property<Integer> memory = Property.ofValue(512);

    @Schema(title = "Root filesystem", description = "Storage and size, e.g. local-lvm:4.")
    @Builder.Default
    @PluginProperty(group = "resources")
    private Property<String> rootfs = Property.ofValue("local-lvm:4");

    @Schema(title = "Unprivileged container", description = "Create an unprivileged container. Defaults to true.")
    @Builder.Default
    @PluginProperty(group = "advanced")
    private Property<Boolean> unprivileged = Property.ofValue(true);

    @Schema(title = "Network interface", description = "LXC net string, e.g. name=eth0,bridge=vmbr0,ip=dhcp.")
    @Builder.Default
    @PluginProperty(group = "resources")
    private Property<String> net = Property.ofValue("name=eth0,bridge=vmbr0,ip=dhcp");

    @Schema(title = "Start after creation", description = "Defaults to false.")
    @Builder.Default
    @PluginProperty(group = "advanced")
    private Property<Boolean> powerOn = Property.ofValue(false);

    @Override
    public AbstractTask.Output run(RunContext runContext) throws Exception {
        var logger = runContext.logger();
        var rNode = runContext.render(node).as(String.class).orElseThrow();
        var rVmId = runContext.render(vmId).as(Integer.class).orElseThrow();
        var rOsTemplate = runContext.render(osTemplate).as(String.class).orElseThrow();
        var rHostname = runContext.render(hostname).as(String.class).orElse(null);
        var rCores = runContext.render(cores).as(Integer.class).orElse(1);
        var rMemory = runContext.render(memory).as(Integer.class).orElse(512);
        var rRootfs = runContext.render(rootfs).as(String.class).orElse("local-lvm:4");
        var rUnprivileged = runContext.render(unprivileged).as(Boolean.class).orElse(true);
        var rNet = runContext.render(net).as(String.class).orElse("name=eth0,bridge=vmbr0,ip=dhcp");
        var rPowerOn = runContext.render(powerOn).as(Boolean.class).orElse(false);

        try (var client = createClient(runContext)) {
            var params = new LinkedHashMap<String, String>();
            params.put("vmid", String.valueOf(rVmId));
            params.put("ostemplate", rOsTemplate);
            params.put("cores", String.valueOf(rCores));
            params.put("memory", String.valueOf(rMemory));
            params.put("rootfs", rRootfs);
            params.put("unprivileged", rUnprivileged ? "1" : "0");
            params.put("net0", rNet);
            if (rHostname != null) {
                params.put("hostname", rHostname);
            }

            logger.info("Creating LXC container vmid={} on node '{}'", rVmId, rNode);
            var upid = client.postAndWait("/nodes/" + rNode + "/lxc", params);
            logger.info("Container vmid={} created", rVmId);

            if (rPowerOn) {
                logger.info("Starting container vmid={}", rVmId);
                client.postAndWait("/nodes/" + rNode + "/lxc/" + rVmId + "/status/start", null);
            }

            return AbstractTask.Output.of(String.valueOf(rVmId), rHostname, upid);
        }
    }
}
