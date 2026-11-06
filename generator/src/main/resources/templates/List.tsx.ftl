/**
 * Purpose: react-admin list view for "${entity.label}" (REST resource "${entity.plural}").
 * Renders the records with the generic TanStack Table component using server-side
 * paging, sorting, filtering and free-text search.
 * GENERATED from schemas/${entity.schemaFile}. Do not edit by hand.
 */
import { List, TextInput, SelectInput, BooleanInput } from 'react-admin';
import { TanStackTable } from '../components/TanStackTable';
import type { ColumnSpec } from '../components/TanStackTable';

const columns: ColumnSpec[] = [
<#list entity.fields?filter(f -> f.listVisible) as f>
  { key: '${f.name}', label: '${f.displayLabel?js_string}', kind: '${f.tsKind}', sortable: ${f.sortable?c} },
</#list>
];

const filters = [
<#if entity.fields?filter(f -> f.searchable)?has_content>
  <TextInput key="q" source="q" label="Search" alwaysOn resettable />,
</#if>
<#list entity.fields?filter(f -> f.filterable && !f.primaryKey) as f>
<#if f.type == "enum">
  <SelectInput key="${f.name}" source="${f.name}" label="${f.displayLabel}" alwaysOn emptyText="All" choices={[<#list f.values as v>{ id: '${v}', name: '${v}' }<#sep>, </#list>]} />,
<#elseif f.type == "boolean">
  <BooleanInput key="${f.name}" source="${f.name}" label="${f.displayLabel}" />,
<#else>
  <TextInput key="${f.name}" source="${f.name}" label="${f.displayLabel}" />,
</#if>
</#list>
];

export const ${entity.name}List = () => (
  <List
    resource="${entity.plural}"
    title="${entity.label}"
    perPage={25}
    pagination={false}
    filters={filters}
    sort={{ field: '${entity.defaultSortField}', order: 'ASC' }}
  >
    <TanStackTable columns={columns} resource="${entity.plural}" />
  </List>
);
