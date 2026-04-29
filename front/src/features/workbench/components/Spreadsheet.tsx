import React, { useEffect, useRef } from 'react';
import XSpreadsheet from 'x-data-spreadsheet';

import 'x-data-spreadsheet/dist/xspreadsheet.css';

interface SpreadsheetProps {
  data: Record<string, unknown>[];
  dataId: string;
  dataAreaRefresh?: unknown;
}

interface SheetTransferResult {
  sheetStyle: Record<string, unknown>;
  sheetData: Record<string, unknown>;
}

export default function Spreadsheet(props: SpreadsheetProps): React.JSX.Element {
  const sheetEl = useRef<HTMLDivElement>(null);
  const sheetId = `x-spreadsheet-${props.dataId}`;
  const styles = [
    {
      bgcolor: '#93d051',
    },
  ];

  function dataTransfer(param: Record<string, unknown>[]): SheetTransferResult {
    if (param[0] !== undefined) {
      const rows10: Record<number, { cells: Record<number, { text: string; style?: number }> }> = {};
      const cells: Record<number, { text: string; style: number }> = {};
      let colNum = 0;

      Object.keys(param[0]).forEach((col, cindex) => {
        colNum += 1;
        cells[cindex] = { text: col, style: 0 };
      });

      const sheetStyle = {
        showToolbar: false,
        showBottomBar: false,
        mode: 'read',
        showContextmenu: false,
        row: { len: param.length + 1 },
        col: { len: colNum },
        view: {
          height: () => (param.length + 4) * 25,
          width: () => colNum * 110,
        },
      };

      rows10[0] = { cells };

      param.forEach((row, rindex) => {
        const rowCells: Record<number, { text: string }> = {};
        Object.keys(row).forEach((col, cindex) => {
          rowCells[cindex] = {
            text: row[col] === null ? 'null' : String(row[col]),
          };
        });
        rows10[rindex + 1] = {
          cells: rowCells,
        };
      });

      return {
        sheetStyle,
        sheetData: { name: 't', rows: rows10, styles },
      };
    }

    return {
      sheetStyle: {
        showToolbar: false,
        showBottomBar: false,
        mode: 'read',
        showContextmenu: false,
      },
      sheetData: {},
    };
  }

  useEffect(() => {
    const element = sheetEl.current;
    if (!element) {
      return undefined;
    }

    element.innerHTML = '';
    const { sheetData, sheetStyle } = dataTransfer(props.data);
    const sheet = new XSpreadsheet(`#${sheetId}`, sheetStyle);
    sheet.loadData(sheetData);

    return () => {
      element.innerHTML = '';
    };
  }, [props.data, props.dataAreaRefresh, sheetId]);

  return <div id={sheetId} ref={sheetEl}></div>;
}
