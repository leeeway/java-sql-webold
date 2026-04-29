// fetch-http-client
declare module 'fetch-http-client' {
  export function json(): (next: unknown) => unknown;

  export default class FetchHttpClient {
    constructor(baseUrl?: string);
    addMiddleware(middleware: unknown): void;
    get(url: string, options?: Record<string, unknown>): Promise<{
      status: number;
      jsonData: { status: boolean; message: string; data: any };
    }>;
    post(url: string, options?: Record<string, unknown>): Promise<{
      status: number;
      jsonData: { status: boolean; message: string; data: any };
    }>;
    put(url: string, options?: Record<string, unknown>): Promise<{
      status: number;
      jsonData: { status: boolean; message: string; data: any };
    }>;
    delete(url: string, options?: Record<string, unknown>): Promise<{
      status: number;
      jsonData: { status: boolean; message: string; data: any };
    }>;
  }
}

// x-data-spreadsheet
declare module 'x-data-spreadsheet' {
  interface SpreadsheetOptions {
    showToolbar?: boolean;
    showBottomBar?: boolean;
    mode?: string;
    showContextmenu?: boolean;
    row?: { len: number };
    col?: { len: number };
    view?: {
      height: () => number;
      width: () => number;
    };
  }

  export default class XSpreadsheet {
    constructor(selector: string, options?: SpreadsheetOptions);
    loadData(data: unknown): void;
  }
}

declare module 'x-data-spreadsheet/dist/xspreadsheet.css';

// react-cookies
declare module 'react-cookies' {
  const cookie: {
    load(name: string): string | undefined;
    save(name: string, value: string, options?: Record<string, unknown>): void;
    remove(name: string, options?: Record<string, unknown>): void;
  };
  export default cookie;
}

// qrcode.react (v1)
declare module 'qrcode.react' {
  import { Component } from 'react';

  interface QRCodeProps {
    value: string;
    size?: number;
    bgColor?: string;
    fgColor?: string;
    level?: 'L' | 'M' | 'Q' | 'H';
    renderAs?: 'canvas' | 'svg';
    includeMargin?: boolean;
  }

  export default class QRCode extends Component<QRCodeProps> {}
}

// copy-to-clipboard
declare module 'copy-to-clipboard' {
  function copy(text: string, options?: { debug?: boolean; message?: string; format?: string }): boolean;
  export default copy;
}

// pubsub-js
declare module 'pubsub-js' {
  function subscribe(topic: string, callback: (topic: string, data: unknown) => void): string;
  function publish(topic: string, data?: unknown): boolean;
  function unsubscribe(tokenOrTopic: string): boolean | string;
  export { subscribe, publish, unsubscribe };
}

// react-csv
declare module 'react-csv' {
  import { Component } from 'react';

  interface CSVLinkProps {
    data: unknown[];
    headers?: Array<{ label: string; key: string }>;
    filename?: string;
    separator?: string;
    className?: string;
    target?: string;
    children?: React.ReactNode;
  }

  export class CSVLink extends Component<CSVLinkProps> {}
}

// @github/webauthn-json
declare module '@github/webauthn-json' {
  export function supported(): boolean;
  export function get(options: unknown): Promise<unknown>;
  export function create(options: unknown): Promise<unknown>;
}

// react-syntax-highlighter
declare module 'react-syntax-highlighter' {
  import { Component } from 'react';

  interface SyntaxHighlighterProps {
    language?: string;
    style?: Record<string, unknown>;
    children: string;
    [key: string]: unknown;
  }

  export default class SyntaxHighlighter extends Component<SyntaxHighlighterProps> {}
}

declare module 'react-syntax-highlighter/dist/esm/styles/hljs' {
  export const docco: Record<string, unknown>;
  export const dark: Record<string, unknown>;
}

// SVG / image imports
declare module '*.svg' {
  const content: string;
  export default content;
}

declare module '*.png' {
  const content: string;
  export default content;
}

declare module '*.css' {
  const content: Record<string, string>;
  export default content;
}

declare module '*.gif' {
  const content: string;
  export default content;
}
