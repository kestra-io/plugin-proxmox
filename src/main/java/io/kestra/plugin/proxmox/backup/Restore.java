package io.kestra.plugin.proxmox.backup;

import io.kestra.core.models.annotations.Example;
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
import java.time.Duration;
import java.util.LinkedHashMap;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
@Schema(
    title = "Restore a backup to a QEMU VM or LXC container on Proxmox VE",
    description = """
        Restores a vzdump backup archive to a new VM or container.
        Set resourceType to vm (default) for QEMU or container for LXC.
        The archive parameter must be a valid volid, e.g. local:backup/vzdump-qemu-100-....vma.zst.
        """
)
@Plugin(
    examples = {
        @Example(
            title = "Restore a VM backup",
            full = true,
            code = """
                id: restore_backup
                namespace: company.team

                tasks:
                  - id: restore
                    type: io.kestra.plugin.proxmox.backup.Restore
                    host: "{{ secret('PROXMOX_HOST') }}"
                    username: "{{ secret('PROXMOX_USERNAME') }}"
                    password: "{{ secret('PROXMOX_PASSWORD') }}"
                    node: pve
                    vmId: 200
                    archive: "local:backup/vzdump-qemu-100-2024_01_01-00_00_00.vma.zst"
                    storage: local-lvm
                    resourceType: vm
                """
        )
    }
)
public class Restore extends AbstractTask<AbstractTask.Output> {

    @Schema(title = "New VM or container ID to assign")
    @NotNull
    @PluginProperty(group = "main")
    private Property<Integer> vmId;

    @Schema(title = "Backup archive volid", description = "Volume ID of the backup, e.g. local:backup/vzdump-....vma.zst.")
    @NotNull
    @PluginProperty(group = "main")
    private Property<String> archive;

    @Schema(title = "Target storage", description = "Storage where the restored disk images are placed.")
    @NotNull
    @PluginProperty(group = "main")
    private Property<String> storage;

    @Schema(title = "Resource type", description = "Resource type: vm or container.")
    @Builder.Default
    @PluginProperty(group = "main")
    private Property<ResourceType> resourceType = Property.ofValue(ResourceType.vm);

    @Schema(
        title = "Task timeout",
        description = "Maximum time to wait for the backup operation to complete. Defaults to 1 hour."
    )
    @Builder.Default
    @PluginProperty(group = "advanced")
    private Property<Duration> timeout = Property.ofValue(Duration.ofHours(1));

    @Override
    public AbstractTask.Output run(RunContext runContext) throws Exception {
        var logger = runContext.logger();
        var rNode = runContext.render(node).as(String.class).orElseThrow();
        var rVmId = runContext.render(vmId).as(Integer.class).orElseThrow();
        var rArchive = runContext.render(archive).as(String.class).orElseThrow();
        var rStorage = runContext.render(storage).as(String.class).orElseThrow();
        var rResourceType = runContext.render(resourceType).as(ResourceType.class).orElse(ResourceType.vm);

        var apiSegment = ResourceType.container == rResourceType ? "lxc" : "qemu";

        try (var client = createClient(runContext)) {
            var params = new LinkedHashMap<String, String>();
            params.put("vmid", String.valueOf(rVmId));
            params.put("archive", rArchive);
            params.put("storage", rStorage);

            var timeoutSeconds = (int) runContext.render(timeout).as(Duration.class)
                .orElse(Duration.ofHours(1)).toSeconds();
            logger.info("Restoring {} backup '{}' as vmid={}", rResourceType, rArchive, rVmId);
            var result = client.post("/nodes/" + URLEncoder.encode(rNode, StandardCharsets.UTF_8) + "/" + apiSegment, params);
            var upid = result.isNull() ? "" : result.asText();
            if (!upid.isBlank()) {
                client.waitForTask(upid, timeoutSeconds);
            }
            logger.info("Restore completed for vmid={}", rVmId);
            return AbstractTask.Output.of(String.valueOf(rVmId), null, upid);
        }
    }
}
