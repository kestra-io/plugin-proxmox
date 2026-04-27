package io.kestra.plugin.proxmox.vm;

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

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
@Schema(
    title = "List QEMU virtual machines on a Proxmox VE node",
    description = """
        Retrieves all QEMU VMs from /nodes/{node}/qemu and returns a list with their VMID, name, and status.
        """
)
@Plugin(
    examples = {
        @Example(
            title = "List all VMs on a Proxmox node",
            full = true,
            code = """
                id: list_vms
                namespace: company.team

                tasks:
                  - id: list
                    type: io.kestra.plugin.proxmox.vm.List
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
            var data = client.get("/nodes/" + rNode + "/qemu");
            var vms = new ArrayList<VmInfo>();
            for (var item : data) {
                vms.add(MAPPER.treeToValue(item, VmInfo.class));
            }
            logger.info("Found {} VMs on node '{}'", vms.size(), rNode);
            return new Output(vms);
        }
    }

    public record Output(
        @Schema(title = "List of virtual machines") java.util.List<VmInfo> vms
    ) implements io.kestra.core.models.tasks.Output {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record VmInfo(
        @Schema(title = "VM ID") int vmid,
        @Schema(title = "VM name") String name,
        @Schema(title = "VM status", description = "running, stopped, etc.") String status,
        @Schema(title = "Number of vCPUs") int cpus,
        @Schema(title = "Configured memory in MiB") long maxmem
    ) {}
}
