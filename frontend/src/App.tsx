/**
 * Purpose: the react-admin shell. It wires the "simple REST" data provider to the Spring
 * backend (/api) and mounts every resource generated from schemas/*.json
 * (see src/generated/resources.tsx, produced by "gradle :backend:generateCode").
 */
import { Admin } from 'react-admin';
import simpleRestProvider from 'ra-data-simple-rest';
import { generatedResources } from './generated/resources';

const dataProvider = simpleRestProvider('/api');

export const App = () => (
  <Admin dataProvider={dataProvider} title="Global Trading Admin">
    {generatedResources}
  </Admin>
);
