package io.github.kcbear.trading.web;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

/**
 * Purpose: shared implementation of the react-admin "simple REST" list contract used by all
 * generated controllers. It parses sort=["field","ASC"], range=[0,24] and filter={...},
 * builds a JPA Specification (free-text "q" search, "contains" for strings, equality for
 * enums/ids, IN for id lists), runs a paged query and returns the page with a
 * "Content-Range: resource 0-24/total" header. Only whitelisted fields can be sorted/filtered.
 */
public final class SimpleRestSupport {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final int MAX_PAGE_SIZE = 1000;

    private SimpleRestSupport() {
    }

    /** Per-resource whitelist generated from the schema. */
    public record Config(String resource, Set<String> sortable, Set<String> filterable,
                         Set<String> exact, Set<String> searchable) {
    }

    public static <T> ResponseEntity<List<T>> list(JpaSpecificationExecutor<T> repo, Config cfg,
                                                   String sortJson, String rangeJson, String filterJson,
                                                   String idField) {
        int start = 0;
        int end = 24;
        List<Integer> range = parse(rangeJson, new TypeReference<List<Integer>>() { });
        if (range != null && range.size() == 2) {
            start = Math.max(0, range.get(0));
            end = Math.max(start, range.get(1));
        }
        int size = Math.min(end - start + 1, MAX_PAGE_SIZE);
        int page = start / size;

        Sort sort = Sort.by(idField);
        List<String> sortParam = parse(sortJson, new TypeReference<List<String>>() { });
        if (sortParam != null && sortParam.size() == 2 && cfg.sortable().contains(sortParam.get(0))) {
            Sort.Direction dir = "DESC".equalsIgnoreCase(sortParam.get(1)) ? Sort.Direction.DESC : Sort.Direction.ASC;
            sort = Sort.by(dir, sortParam.get(0));
            if (!sortParam.get(0).equals(idField)) {
                sort = sort.and(Sort.by(idField)); // tie-breaker keeps paging stable
            }
        }

        Map<String, Object> filter = parse(filterJson, new TypeReference<Map<String, Object>>() { });
        Page<T> result = repo.findAll(buildSpec(cfg, filter), PageRequest.of(page, size, sort));

        int count = result.getNumberOfElements();
        int to = count == 0 ? start : start + count - 1;
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Range", cfg.resource() + " " + start + "-" + to + "/" + result.getTotalElements());
        headers.add("Access-Control-Expose-Headers", "Content-Range");
        return ResponseEntity.ok().headers(headers).body(result.getContent());
    }

    private static <T> Specification<T> buildSpec(Config cfg, Map<String, Object> filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (filter != null) {
                for (Map.Entry<String, Object> e : filter.entrySet()) {
                    String key = e.getKey();
                    Object value = e.getValue();
                    if (value == null || value.toString().isBlank()) {
                        continue;
                    }
                    if ("q".equals(key)) {
                        String like = "%" + value.toString().toLowerCase() + "%";
                        List<Predicate> ors = new ArrayList<>();
                        for (String f : cfg.searchable()) {
                            ors.add(cb.like(cb.lower(root.<String>get(f)), like));
                        }
                        if (!ors.isEmpty()) {
                            predicates.add(cb.or(ors.toArray(new Predicate[0])));
                        }
                    } else if (cfg.filterable().contains(key)) {
                        Path<Object> path = root.get(key);
                        Class<?> type = path.getJavaType();
                        try {
                            if (value instanceof Collection<?> items) {
                                List<Object> converted = new ArrayList<>();
                                for (Object item : items) {
                                    converted.add(MAPPER.convertValue(item, type));
                                }
                                predicates.add(path.in(converted));
                            } else if (type == String.class && !cfg.exact().contains(key)) {
                                predicates.add(cb.like(cb.lower(root.<String>get(key)),
                                        "%" + value.toString().toLowerCase() + "%"));
                            } else {
                                predicates.add(cb.equal(path, MAPPER.convertValue(value, type)));
                            }
                        } catch (IllegalArgumentException ex) {
                            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid filter value for " + key);
                        }
                    }
                }
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private static <R> R parse(String json, TypeReference<R> type) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return MAPPER.readValue(json, type);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid JSON query parameter");
        }
    }
}
