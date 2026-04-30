import React from 'react';
import { Tabs } from 'antd';

interface QueryPane {
  title: string;
  sql: string;
  data: unknown[];
  key: string;
}

const initialPanes: QueryPane[] = [
  { title: 'Tab 1', sql: '', data: [], key: '1' },
];

function QueryTabs(): React.JSX.Element {
  const [activeKey, setActiveKey] = React.useState<string>(initialPanes[0].key);

  return (
    <Tabs
      activeKey={activeKey}
      items={initialPanes.map((pane) => ({
        key: pane.key,
        label: pane.title,
        children: null,
      }))}
      onChange={setActiveKey}
      type="editable-card"
    />
  );
}

export default QueryTabs;
