/**
 * Purpose: registers every generated entity as a react-admin <Resource> (list, edit, create).
 * GENERATED from all schemas/*.json files. Do not edit by hand.
 */
import { Resource } from 'react-admin';
<#list entities as e>
import { ${e.name}List } from './${e.name}List';
import { ${e.name}Edit, ${e.name}Create } from './${e.name}Form';
</#list>

export const generatedResources = [
<#list entities as e>
  <Resource key="${e.plural}" name="${e.plural}" options={{ label: '${e.label?js_string}' }} list={${e.name}List} edit={${e.name}Edit} create={${e.name}Create} />,
</#list>
];
