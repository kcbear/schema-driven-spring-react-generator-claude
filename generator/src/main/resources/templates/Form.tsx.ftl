/**
 * Purpose: react-admin create and edit forms for "${entity.label}". Row clicks in the
 * TanStack Table list open the edit form; the inputs are derived from the schema field types.
 * GENERATED from schemas/${entity.schemaFile}. Do not edit by hand.
 */
import {
  Create,
  Edit,
  SimpleForm,
  TextInput,
  NumberInput,
  BooleanInput,
  DateInput,
  DateTimeInput,
  SelectInput,
  required,
} from 'react-admin';

const ${entity.name}Inputs = () => (
  <>
<#list entity.fields?filter(f -> !f.primaryKey) as f>
<#if f.type == "enum">
    <SelectInput source="${f.name}" label="${f.displayLabel}" choices={[<#list f.values as v>{ id: '${v}', name: '${v}' }<#sep>, </#list>]}<#if f.required> validate={required()}</#if> />
<#else>
    <${f.raInput} source="${f.name}" label="${f.displayLabel}"<#if f.required> validate={required()}</#if> />
</#if>
</#list>
  </>
);

export const ${entity.name}Edit = () => (
  <Edit>
    <SimpleForm>
      <TextInput source="${entity.idField.name}" disabled />
      <${entity.name}Inputs />
    </SimpleForm>
  </Edit>
);

export const ${entity.name}Create = () => (
  <Create>
    <SimpleForm>
      <${entity.name}Inputs />
    </SimpleForm>
  </Create>
);
