package io.kestra.plugin.proxmox.network;

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
    title = "Get firewall rules on a Proxmox VE node",
    description = """
        Retrieves firewall rules from /nodes/{node}/firewall/rules.
        Optionally scope to a specific VM or container by providing a vmId.
        When vmId is set, reads /nodes/{node}/qemu/{vmid}/firewall/rules (VM)
        or /nodes/{node}/lxc/{vmid}/firewall/rules (container) based on resourceType.
        """
)
@Plugin(
    examples = {
        @Example(
            title = "Get node-level firewall rules",
            full = true,
            code = """
                id: get_firewall_rules
                namespace: company.team

                tasks:
                  - id: rules
                    type: io.kestra.plugin.proxmox.network.GetFirewallRules
                    host: "{{ secret('PROXMOX_HOST') }}"
                    username: "{{ secret('PROXMOX_USERNAME') }}"
                    password: "{{ secret('PROXMOX_PASSWORD') }}"
                    node: pve
                """
        ),
        @Example(
            title = "Get VM-level firewall rules",
            full = true,
            code = """
                id: get_vm_firewall_rules
                namespace: company.team

                tasks:
                  - id: rules
                    type: io.kestra.plugin.proxmox.network.GetFirewallRules
                    host: "{{ secret('PROXMOX_HOST') }}"
                    username: "{{ secret('PROXMOX_USERNAME') }}"
                    password: "{{ secret('PROXMOX_PASSWORD') }}"
                    node: pve
                    vmId: "100"
                    resourceType: vm
                """
        )
    }
)
public class GetFirewallRules extends AbstractTask<GetFirewallRules.Output> {

    @Schema(
        title = "VM or container ID",
        description = "When set, retrieves firewall rules scoped to this VM or container instead of the node."
    )
    @PluginProperty(group = "main")
    private Property<String> vmId;

    @Schema(
        title = "Resource type",
        description = "Required when vmId is set. Use 'vm' for QEMU VMs or 'container' for LXC containers."
    )
    @PluginProperty(group = "main")
    private Property<String> resourceType;

    @Override
    public Output run(RunContext runContext) throws Exception {
        var logger = runContext.logger();
        var rNode = runContext.render(node).as(String.class).orElseThrow();
        var rVmId = runContext.render(vmId).as(String.class).orElse(null);

        var encodedNode = URLEncoder.encode(rNode, StandardCharsets.UTF_8);
        String path;
        if (rVmId != null && !rVmId.isBlank()) {
            var rType = runContext.render(resourceType).as(String.class).orElse("vm");
            var apiType = "container".equalsIgnoreCase(rType) ? "lxc" : "qemu";
            path = "/nodes/" + encodedNode + "/" + apiType + "/" + URLEncoder.encode(rVmId, StandardCharsets.UTF_8) + "/firewall/rules";
        } else {
            path = "/nodes/" + encodedNode + "/firewall/rules";
        }

        try (var client = createClient(runContext)) {
            var data = client.get(path);
            var rules = new ArrayList<FirewallRule>();
            for (var item : data) {
                rules.add(JacksonMapper.ofJson().treeToValue(item, FirewallRule.class));
            }
            logger.info("Found {} firewall rules", rules.size());
            return new Output(rules);
        }
    }

    public record Output(
        @Schema(title = "Firewall rules") List<FirewallRule> rules
    ) implements io.kestra.core.models.tasks.Output {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record FirewallRule(
        @Schema(title = "Rule position") int pos,
        @Schema(title = "Rule type (in/out/forward/group)") String type,
        @Schema(title = "Action (ACCEPT/DROP/REJECT)") String action,
        @Schema(title = "Whether the rule is enabled") int enable,
        @Schema(title = "Source address or CIDR") String source,
        @Schema(title = "Destination address or CIDR") String dest,
        @Schema(title = "Protocol (tcp, udp, icmp, etc.)") String proto,
        @Schema(title = "Destination port or port range") String dport,
        @Schema(title = "Source port or port range") String sport,
        @Schema(title = "Rule comment") String comment
    ) {}
}
