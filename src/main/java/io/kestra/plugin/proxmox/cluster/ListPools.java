package io.kestra.plugin.proxmox.cluster;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.kestra.core.models.annotations.Example;
import io.kestra.core.serializers.JacksonMapper;
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
    title = "List resource pools in a Proxmox VE cluster",
    description = "Retrieves all resource pools from /pools."
)
@Plugin(
    examples = {
        @Example(
            title = "List all resource pools",
            full = true,
            code = """
                id: list_pools
                namespace: company.team

                tasks:
                  - id: pools
                    type: io.kestra.plugin.proxmox.cluster.ListPools
                    host: "{{ secret('PROXMOX_HOST') }}"
                    username: "{{ secret('PROXMOX_USERNAME') }}"
                    password: "{{ secret('PROXMOX_PASSWORD') }}"
                    node: pve
                """
        )
    }
)
public class ListPools extends AbstractTask<ListPools.Output> {

    @Override
    public Output run(RunContext runContext) throws Exception {
        var logger = runContext.logger();

        try (var client = createClient(runContext)) {
            var data = client.get("/pools");
            var pools = new ArrayList<PoolInfo>();
            for (var item : data) {
                pools.add(JacksonMapper.ofJson().treeToValue(item, PoolInfo.class));
            }
            logger.info("Found {} pools", pools.size());
            return new Output(pools);
        }
    }

    public record Output(
        @Schema(title = "List of resource pools") List<PoolInfo> pools
    ) implements io.kestra.core.models.tasks.Output {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PoolInfo(
        @Schema(title = "Pool ID") String poolid,
        @Schema(title = "Pool comment") String comment
    ) {}
}
