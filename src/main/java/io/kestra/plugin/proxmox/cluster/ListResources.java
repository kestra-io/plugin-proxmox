package io.kestra.plugin.proxmox.cluster;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.kestra.core.models.annotations.Example;
import io.kestra.core.serializers.JacksonMapper;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.annotations.PluginProperty;
import io.kestra.core.models.property.Property;
import io.kestra.core.runners.RunContext;
import io.kestra.plugin.proxmox.AbstractTask;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
@Schema(
    title = "List cluster resources on Proxmox VE",
    description = """
        Retrieves resources from /cluster/resources.
        Optionally filter by type: vm, node, storage, or pool.
        """
)
@Plugin(
    examples = {
        @Example(
            title = "List all cluster resources",
            full = true,
            code = """
                id: list_cluster_resources
                namespace: company.team

                tasks:
                  - id: resources
                    type: io.kestra.plugin.proxmox.cluster.ListResources
                    host: "{{ secret('PROXMOX_HOST') }}"
                    username: "{{ secret('PROXMOX_USERNAME') }}"
                    password: "{{ secret('PROXMOX_PASSWORD') }}"
                    node: pve
                    typeFilter: vm
                """
        )
    }
)
public class ListResources extends AbstractTask<ListResources.Output> {

    @Schema(
        title = "Resource type filter",
        description = "Optional filter: vm, node, storage, or pool. Omit to return all types."
    )
    @PluginProperty(group = "advanced")
    private Property<String> typeFilter;

    @Override
    public Output run(RunContext runContext) throws Exception {
        var logger = runContext.logger();
        var rTypeFilter = runContext.render(typeFilter).as(String.class).orElse(null);

        var path = "/cluster/resources";
        if (rTypeFilter != null && !rTypeFilter.isBlank()) {
            path += "?type=" + URLEncoder.encode(rTypeFilter, StandardCharsets.UTF_8);
        }

        try (var client = createClient(runContext)) {
            var data = client.get(path);
            var resources = new ArrayList<ResourceInfo>();
            for (var item : data) {
                resources.add(JacksonMapper.ofJson().treeToValue(item, ResourceInfo.class));
            }
            logger.info("Found {} cluster resources", resources.size());
            return new Output(resources);
        }
    }

    public record Output(
        @Schema(title = "List of cluster resources") List<ResourceInfo> resources
    ) implements io.kestra.core.models.tasks.Output {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ResourceInfo(
        @Schema(title = "Resource type") String type,
        @Schema(title = "Resource ID or name") String id,
        @Schema(title = "Node the resource is on") String node,
        @Schema(title = "Resource status") String status,
        @Schema(title = "VMID (for VM/container resources)") int vmid,
        @Schema(title = "Resource name") String name
    ) {}
}
