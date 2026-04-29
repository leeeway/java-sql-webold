export interface Pane {
  key: string;
  title?: string;
  sql?: string;
  data?: unknown[];
  [prop: string]: unknown;
}

export function getArray(panes: Pane[], key: string): Pane | undefined {
  let lastIndex: number | undefined;

  panes.forEach((pane, i) => {
    if (pane.key === key) {
      lastIndex = i;
    }
  });

  return lastIndex !== undefined ? panes[lastIndex] : undefined;
}

export function editArray(panes: Pane[], key: string, data: Pane): Pane[] {
  let lastIndex = -1;
  panes.forEach((pane, i) => {
    if (pane.key === key) {
      lastIndex = i;
    }
  });
  if (lastIndex >= 0) {
    panes[lastIndex] = data;
  }
  return panes;
}
