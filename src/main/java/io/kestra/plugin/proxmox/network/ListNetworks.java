package io.kestra.plugin.proxmox.network;

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

import java.util.ArrayList;
import java.util.List;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
@Schema(
    title = "List network interfaces on a Proxmox VE node",
    description = "Retrieves all network interfaces from /nodes/{node}/network."
)
@Plugin(
    examples = {
        @Example(
            title = "List network interfaces",
            full = true,
            code = """
                id: list_networks
                namespace: company.team

                tasks:
                  - id: networks
                    type: io.kestra.plugin.proxmox.network.ListNetworks
                    host: "{{ secret('PROXMOX_HOST') }}"
                    username: "{{ secret('PROXMOX_USERNAME') }}"
                    password: "{{ secret('PROXMOX_PASSWORD') }}"
                    node: pve
                """
        )
    }
)
public class ListNetworks extends AbstractTask<ListNetworks.Output> {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public Output run(RunContext runContext) throws Exception {
        var logger = runContext.logger();
        var rNode = runContext.render(node).as(String.class).orElseThrow();

        try (var client = createClient(runContext)) {
            var data = client.get("/nodes/" + rNode + "/network");
            var interfaces = new ArrayList<NetworkInterface>();
            for (var item : data) {
                interfaces.add(MAPPER.treeToValue(item, NetworkInterface.class));
            }
            logger.info("Found {} network interfaces on node {}", interfaces.size(), rNode);
            return new Output(interfaces);
        }
    }

    public record Output(
        @Schema(title = "Network interfaces on the node") List<NetworkInterface> interfaces
    ) implements io.kestra.core.models.tasks.Output {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record NetworkInterface(
        @Schema(title = "Interface name") String iface,
        @Schema(title = "Interface type (bridge, bond, eth, vlan, etc.)") String type,
        @Schema(title = "Whether the interface is active") boolean active,
        @Schema(title = "IP address with prefix length (CIDR)") String cidr,
        @Schema(title = "IPv6 address with prefix length (CIDR)") String cidr6,
        @Schema(title = "Gateway address") String gateway,
        @Schema(title = "IPv6 gateway address") String gateway6,
        @Schema(title = "Bridge ports") String bridge_ports,
        @Schema(title = "Interface comment") String comments
    ) {}
}
