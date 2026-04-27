package io.kestra.plugin.proxmox.container;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.annotations.Plugin;
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

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
@Schema(
    title = "List LXC containers on a Proxmox VE node",
    description = "Retrieves all LXC containers from /nodes/{node}/lxc and returns a list with their VMID, name, and status."
)
@Plugin(
    examples = {
        @Example(
            title = "List all containers on a node",
            full = true,
            code = """
                id: list_containers
                namespace: company.team

                tasks:
                  - id: list
                    type: io.kestra.plugin.proxmox.container.List
                    host: "{{ secret('PROXMOX_HOST') }}"
                    username: "{{ secret('PROXMOX_USERNAME') }}"
                    password: "{{ secret('PROXMOX_PASSWORD') }}"
                    node: pve
                """
        )
    }
)
public class List extends AbstractTask<List.Output> {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public Output run(RunContext runContext) throws Exception {
        var logger = runContext.logger();
        var rNode = runContext.render(node).as(String.class).orElseThrow();

        try (var client = createClient(runContext)) {
            var data = client.get("/nodes/" + URLEncoder.encode(rNode, StandardCharsets.UTF_8) + "/lxc");
            var containers = new ArrayList<ContainerInfo>();
            for (var item : data) {
                containers.add(MAPPER.treeToValue(item, ContainerInfo.class));
            }
            logger.info("Found {} containers on node '{}'", containers.size(), rNode);
            return new Output(containers);
        }
    }

    public record Output(
        @Schema(title = "List of LXC containers") java.util.List<ContainerInfo> containers
    ) implements io.kestra.core.models.tasks.Output {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ContainerInfo(
        @Schema(title = "Container ID") int vmid,
        @Schema(title = "Container hostname") String name,
        @Schema(title = "Container status") String status,
        @Schema(title = "Number of vCPUs") int cpus,
        @Schema(title = "Configured memory in MiB") long maxmem
    ) {}
}
