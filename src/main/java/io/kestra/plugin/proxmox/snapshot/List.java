package io.kestra.plugin.proxmox.snapshot;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.kestra.core.models.annotations.Example;
import io.kestra.core.serializers.JacksonMapper;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.annotations.PluginProperty;
import io.kestra.core.models.property.Property;
import io.kestra.core.runners.RunContext;
import io.kestra.plugin.proxmox.AbstractTask;
import io.kestra.plugin.proxmox.ResourceType;
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
import java.util.ArrayList;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
@Schema(
    title = "List snapshots of a QEMU VM or LXC container on Proxmox VE",
    description = """
        Retrieves all snapshots for the given VM or container.
        Set resourceType to vm (default) for QEMU VMs or container for LXC containers.
        """
)
@Plugin(
    examples = {
        @Example(
            title = "List VM snapshots",
            full = true,
            code = """
                id: list_snapshots
                namespace: company.team

                tasks:
                  - id: list
                    type: io.kestra.plugin.proxmox.snapshot.List
                    host: "{{ secret('PROXMOX_HOST') }}"
                    username: "{{ secret('PROXMOX_USERNAME') }}"
                    password: "{{ secret('PROXMOX_PASSWORD') }}"
                    node: pve
                    vmName: my-vm
                    resourceType: vm
                """
        )
    }
)
public class List extends AbstractTask<List.Output> {

    @Schema(title = "VM or container name or ID", description = "VM or container name (resolved at runtime) or integer VMID.")
    @NotNull
    @PluginProperty(group = "main")
    private Property<String> vmName;

    @Schema(title = "Resource type", description = "Resource type: vm or container.")
    @Builder.Default
    @PluginProperty(group = "main")
    private Property<ResourceType> resourceType = Property.ofValue(ResourceType.VM);

    @Override
    public Output run(RunContext runContext) throws Exception {
        var logger = runContext.logger();
        var rNode = runContext.render(node).as(String.class).orElseThrow();
        var rVmName = runContext.render(vmName).as(String.class).orElseThrow();
        var rResourceType = runContext.render(resourceType).as(ResourceType.class).orElse(ResourceType.VM);

        var apiSegment = ResourceType.CONTAINER == rResourceType ? "lxc" : "qemu";

        try (var client = createClient(runContext)) {
            var vmid = client.resolveVmId(rVmName);
            var data = client.get("/nodes/" + URLEncoder.encode(rNode, StandardCharsets.UTF_8) + "/" + apiSegment + "/" + vmid + "/snapshot");
            var snapshots = new ArrayList<SnapshotInfo>();
            for (var item : data) {
                snapshots.add(JacksonMapper.ofJson().treeToValue(item, SnapshotInfo.class));
            }
            logger.info("Found {} snapshots for {} vmid={}", snapshots.size(), rResourceType, vmid);
            return new Output(snapshots);
        }
    }

    public record Output(
        @Schema(title = "List of snapshots") java.util.List<SnapshotInfo> snapshots
    ) implements io.kestra.core.models.tasks.Output {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SnapshotInfo(
        @Schema(title = "Snapshot name") String name,
        @Schema(title = "Snapshot description") String description,
        @Schema(title = "Snapshot creation time (epoch seconds)") long snaptime
    ) {}
}
