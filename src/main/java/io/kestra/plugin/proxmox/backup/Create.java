package io.kestra.plugin.proxmox.backup;

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
    title = "Create a backup of a VM or container on Proxmox VE",
    description = """
        Creates a vzdump backup via POST /nodes/{node}/vzdump.
        Supports snapshot, suspend, and stop modes. Compress defaults to zstd.
        """
)
@Plugin(
    examples = {
        @Example(
            title = "Backup a VM to local storage",
            full = true,
            code = """
                id: create_backup
                namespace: company.team

                tasks:
                  - id: backup
                    type: io.kestra.plugin.proxmox.backup.Create
                    host: "{{ secret('PROXMOX_HOST') }}"
                    username: "{{ secret('PROXMOX_USERNAME') }}"
                    password: "{{ secret('PROXMOX_PASSWORD') }}"
                    node: pve
                    vmName: my-vm
                    storage: local
                    mode: snapshot
                    compress: zstd
                """
        )
    }
)
public class Create extends AbstractTask<AbstractTask.Output> {

    @Schema(title = "VM or container name or ID")
    @NotNull
    @PluginProperty(group = "main")
    private Property<String> vmName;

    @Schema(title = "Target storage", description = "Storage ID where the backup is written.")
    @NotNull
    @PluginProperty(group = "main")
    private Property<String> storage;

    @Schema(title = "Backup mode", description = "snapshot, suspend, or stop. Defaults to snapshot.")
    @Builder.Default
    @PluginProperty(group = "advanced")
    private Property<String> mode = Property.ofValue("snapshot");

    @Schema(title = "Compression", description = "Compression algorithm: 0 (none), lzo, gzip, or zstd. Defaults to zstd.")
    @Builder.Default
    @PluginProperty(group = "advanced")
    private Property<String> compress = Property.ofValue("zstd");

    @Override
    public AbstractTask.Output run(RunContext runContext) throws Exception {
        var logger = runContext.logger();
        var rNode = runContext.render(node).as(String.class).orElseThrow();
        var rVmName = runContext.render(vmName).as(String.class).orElseThrow();
        var rStorage = runContext.render(storage).as(String.class).orElseThrow();
        var rMode = runContext.render(mode).as(String.class).orElse("snapshot");
        var rCompress = runContext.render(compress).as(String.class).orElse("zstd");

        try (var client = createClient(runContext)) {
            var vmid = client.resolveVmId(rVmName);
            var params = new LinkedHashMap<String, String>();
            params.put("vmid", String.valueOf(vmid));
            params.put("storage", rStorage);
            params.put("mode", rMode);
            params.put("compress", rCompress);

            logger.info("Creating backup for vmid={} on storage '{}' (mode={}, compress={})", vmid, rStorage, rMode, rCompress);
            var result = client.post("/nodes/" + rNode + "/vzdump", params);
            var upid = result.isNull() ? "" : result.asText();
            if (!upid.isBlank()) {
                client.waitForTask(upid, 3600);
            }
            logger.info("Backup completed for vmid={}", vmid);
            return AbstractTask.Output.of(String.valueOf(vmid), rVmName, upid);
        }
    }
}
