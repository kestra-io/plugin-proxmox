package io.kestra.plugin.proxmox.backup;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.util.ArrayList;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
@Schema(
    title = "List backups on a Proxmox VE storage",
    description = "Retrieves backup files from /nodes/{node}/storage/{storage}/content?content=backup."
)
@Plugin(
    examples = {
        @Example(
            title = "List all backups on local storage",
            full = true,
            code = """
                id: list_backups
                namespace: company.team

                tasks:
                  - id: list
                    type: io.kestra.plugin.proxmox.backup.List
                    host: "{{ secret('PROXMOX_HOST') }}"
                    username: "{{ secret('PROXMOX_USERNAME') }}"
                    password: "{{ secret('PROXMOX_PASSWORD') }}"
                    node: pve
                    storage: local
                """
        )
    }
)
public class List extends AbstractTask<List.Output> {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Schema(title = "Storage ID")
    @NotNull
    @PluginProperty(group = "main")
    private Property<String> storage;

    @Override
    public Output run(RunContext runContext) throws Exception {
        var logger = runContext.logger();
        var rNode = runContext.render(node).as(String.class).orElseThrow();
        var rStorage = runContext.render(storage).as(String.class).orElseThrow();

        try (var client = createClient(runContext)) {
            var data = client.get("/nodes/" + rNode + "/storage/" + rStorage + "/content?content=backup");
            var backups = new ArrayList<BackupInfo>();
            for (var item : data) {
                backups.add(MAPPER.treeToValue(item, BackupInfo.class));
            }
            logger.info("Found {} backups on storage '{}'", backups.size(), rStorage);
            return new Output(backups);
        }
    }

    public record Output(
        @Schema(title = "List of backup files") java.util.List<BackupInfo> backups
    ) implements io.kestra.core.models.tasks.Output {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record BackupInfo(
        @Schema(title = "Backup volume ID") String volid,
        @Schema(title = "Backup format") String format,
        @Schema(title = "Size in bytes") long size,
        @Schema(title = "Creation time (epoch seconds)") long ctime
    ) {}
}
