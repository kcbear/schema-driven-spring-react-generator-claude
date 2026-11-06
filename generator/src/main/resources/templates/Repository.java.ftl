package ${basePackage}.generated.repository;

import ${basePackage}.generated.model.${entity.name};
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 * Purpose: Spring Data repository for {@link ${entity.name}}. Extends JpaSpecificationExecutor so the
 * REST controller can apply dynamic filters, search, sorting and paging.
 * <p>GENERATED from schemas/${entity.schemaFile}. Do not edit by hand.
 */
public interface ${entity.name}Repository
        extends JpaRepository<${entity.name}, ${entity.idType}>, JpaSpecificationExecutor<${entity.name}> {
}
