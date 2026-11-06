package ${basePackage}.generated.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Purpose: JPA entity for "${entity.label}" - ${entity.description!''}
 * <p>GENERATED from schemas/${entity.schemaFile} by schema-driven-spring-react-generator.
 * Do not edit by hand: change the schema and re-run "gradle :backend:generateCode".
 */
@Entity
@Table(name = "${entity.tableName}")
public class ${entity.name} {
<#list entity.fields as f>

<#if f.description?has_content>
    /** ${f.description} */
</#if>
<#list f.annotations as a>
    ${a}
</#list>
    private ${f.javaType} ${f.name};
</#list>

<#list entity.fields as f>
    public ${f.javaType} get${f.capName}() {
        return ${f.name};
    }

    public void set${f.capName}(${f.javaType} ${f.name}) {
        this.${f.name} = ${f.name};
    }

</#list>
}
