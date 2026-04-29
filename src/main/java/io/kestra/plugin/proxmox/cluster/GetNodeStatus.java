package io.kestra.plugin.proxmox.cluster;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.kestra.core.models.annotations.Example;
import io.kestra.core.serializers.JacksonMapper;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.runners.RunContext;
import io.kestra.plugin.proxmox.AbstractTask;
import io.swagger.v3.oas.annotations.media.Schema;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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

    @Override
    public Output run(RunContext runContext) throws Exception {
        var logger = runContext.logger();
        var rNode = runContext.render(node).as(String.class).orElseThrow();

        try (var client = createClient(runContext)) {
            var data = client.get("/nodes/" + URLEncoder.encode(rNode, StandardCharsets.UTF_8) + "/status");
            var status = JacksonMapper.ofJson().treeToValue(data, NodeStatus.class);
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
        @Schema(title = "Memory usage") Memory memory,
        @Schema(title = "PVE version") String pveversion
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Memory(
        @Schema(title = "Used memory in bytes") long used,
        @Schema(title = "Free memory in bytes") long free,
        @Schema(title = "Available memory in bytes") long available,
        @Schema(title = "Total memory in bytes") long total
    ) {}
}
