package io.kestra.plugin.proxmox.cluster;

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

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
@Schema(
    title = "Get the status of a Proxmox VE node",
    description = "Retrieves node metrics from /nodes/{node}/status including CPU, memory, and uptime."
)
@Plugin(
    examples = {
        @Example(
            title = "Get node status",
            full = true,
            code = """
                id: node_status
                namespace: company.team

                tasks:
                  - id: status
                    type: io.kestra.plugin.proxmox.cluster.GetNodeStatus
                    host: "{{ secret('PROXMOX_HOST') }}"
                    username: "{{ secret('PROXMOX_USERNAME') }}"
                    password: "{{ secret('PROXMOX_PASSWORD') }}"
                    node: pve
                """
        )
    }
)
public class GetNodeStatus extends AbstractTask<GetNodeStatus.Output> {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public Output run(RunContext runContext) throws Exception {
        var logger = runContext.logger();
        var rNode = runContext.render(node).as(String.class).orElseThrow();

        try (var client = createClient(runContext)) {
            var data = client.get("/nodes/" + rNode + "/status");
            var status = MAPPER.treeToValue(data, NodeStatus.class);
            logger.info("Node '{}' status: uptime={}s", rNode, status.uptime());
            return new Output(status);
        }
    }

    public record Output(
        @Schema(title = "Node status details") NodeStatus status
    ) implements io.kestra.core.models.tasks.Output {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record NodeStatus(
        @Schema(title = "Node uptime in seconds") long uptime,
        @Schema(title = "CPU usage ratio (0..1)") double cpu,
        @Schema(title = "Used memory in bytes") long memory,
        @Schema(title = "Total memory in bytes") long maxmem,
        @Schema(title = "PVE version") String pveversion
    ) {}
}
