import React, { useEffect } from 'react';

interface DataDisplayFastProps {
  data: Record<string, unknown>[];
  dataAreaRefresh?: unknown;
}

export default function DataDisplayFast(props: DataDisplayFastProps): React.JSX.Element {
  function printTableHead(): React.JSX.Element {
    if (props.data[0] !== undefined) {
      const data = props.data[0];
      return (
        <tr>
          {Object.keys(data).map((key) => (
            <th key={key}>{key}</th>
          ))}
        </tr>
      );
    }
    return (
      <tr><th></th></tr>
    );
  }

  function dataColumnShow(columnData: unknown): string {
    if (columnData === null) {
      return 'null';
    }
    switch (typeof columnData) {
      case 'boolean':
        return columnData ? 'true' : 'false';
      default:
        return String(columnData);
    }
  }

  function printTableData(): React.JSX.Element | React.JSX.Element[] {
    if (props.data !== undefined) {
      const data = props.data;
      return data.map((row, rowIndex) => (
        <tr key={`row-${rowIndex}`}>
          {Object.keys(row).map((col) => (
            <td key={`${rowIndex}-${col}`}>{dataColumnShow(row[col])}</td>
          ))}
        </tr>
      ));
    }
    return (
      <tr><td>无数据...</td></tr>
    );
  }

  useEffect(() => {
    console.log(props.data);
  }, [props.dataAreaRefresh]);

  return (
    <>
      <table className="table_results ajax pma_table workbench-results-table">
        <thead>
          {printTableHead()}
        </thead>
        <tbody>
          {printTableData()}
        </tbody>
      </table>
    </>
  );
}
