package ${basePackage}.generated.web;

import ${basePackage}.generated.model.${entity.name};
import ${basePackage}.generated.repository.${entity.name}Repository;
import ${basePackage}.web.SimpleRestSupport;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

/**
 * Purpose: Spring MVC REST controller exposing "${entity.label}" at /api/${entity.plural} using the
 * react-admin "simple REST" contract (sort/range/filter query parameters, Content-Range response
 * header). It is the server side of the generated TanStack Table list view.
 * <p>GENERATED from schemas/${entity.schemaFile}. Do not edit by hand.
 */
@RestController
@RequestMapping("/api/${entity.plural}")
public class ${entity.name}Controller {

    private static final SimpleRestSupport.Config CONFIG = new SimpleRestSupport.Config(
            "${entity.plural}",
            Set.of(<#list entity.fields?filter(f -> f.sortable) as f>"${f.name}"<#sep>, </#list>),
            Set.of(<#list entity.fields?filter(f -> f.filterable || f.primaryKey) as f>"${f.name}"<#sep>, </#list>),
            Set.of(<#list entity.fields?filter(f -> f.type == "enum" || f.primaryKey) as f>"${f.name}"<#sep>, </#list>),
            Set.of(<#list entity.fields?filter(f -> f.searchable) as f>"${f.name}"<#sep>, </#list>));

    private final ${entity.name}Repository repository;

    public ${entity.name}Controller(${entity.name}Repository repository) {
        this.repository = repository;
    }

    /** Paged, sorted and filtered list; total count is returned in the Content-Range header. */
    @GetMapping
    public ResponseEntity<List<${entity.name}>> list(
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String range,
            @RequestParam(required = false) String filter) {
        return SimpleRestSupport.list(repository, CONFIG, sort, range, filter, "${entity.idField.name}");
    }

    @GetMapping("/{id}")
    public ${entity.name} get(@PathVariable ${entity.idType} id) {
        return repository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @PostMapping
    public ${entity.name} create(@Valid @RequestBody ${entity.name} body) {
<#if entity.idField.generated>
        body.set${entity.idField.capName}(null);
</#if>
        return repository.save(body);
    }

    @PutMapping("/{id}")
    public ${entity.name} update(@PathVariable ${entity.idType} id, @Valid @RequestBody ${entity.name} body) {
        if (!repository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        body.set${entity.idField.capName}(id);
        return repository.save(body);
    }

    @DeleteMapping("/{id}")
    public ${entity.name} delete(@PathVariable ${entity.idType} id) {
        ${entity.name} existing = get(id);
        repository.delete(existing);
        return existing;
    }
}
