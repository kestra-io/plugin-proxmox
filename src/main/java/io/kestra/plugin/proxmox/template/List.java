package io.kestra.plugin.proxmox.template;

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

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
@Schema(
    title = "List QEMU VM templates in a Proxmox VE cluster",
    description = """
        Retrieves cluster resources of type vm from /cluster/resources and filters for entries where template=1.
        """
)
@Plugin(
    examples = {
        @Example(
            title = "List all VM templates in the cluster",
            full = true,
            code = """
                id: list_templates
                namespace: company.team

                tasks:
                  - id: list
                    type: io.kestra.plugin.proxmox.template.List
                    host: "{{ secret('PROXMOX_HOST') }}"
                    username: "{{ secret('PROXMOX_USERNAME') }}"
                    password: "{{ secret('PROXMOX_PASSWORD') }}"
                    node: pve
                """
        )
    }
)
public class List extends AbstractTask<List.Output> {

    @Override
    public Output run(RunContext runContext) throws Exception {
        var logger = runContext.logger();

        try (var client = createClient(runContext)) {
            var data = client.get("/cluster/resources?type=vm");
            var templates = new ArrayList<TemplateInfo>();
            for (var item : data) {
                if (item.path("template").asInt(0) == 1) {
                    templates.add(JacksonMapper.ofJson().treeToValue(item, TemplateInfo.class));
                }
            }
            logger.info("Found {} templates in cluster", templates.size());
            return new Output(templates);
        }
    }

    public record Output(
        @Schema(title = "List of VM templates") java.util.List<TemplateInfo> templates
    ) implements io.kestra.core.models.tasks.Output {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TemplateInfo(
        @Schema(title = "Template VM ID") int vmid,
        @Schema(title = "Template name") String name,
        @Schema(title = "Node where the template resides") String node,
        @Schema(title = "Template flag") int template
    ) {}
}
