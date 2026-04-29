export function normalizeServerType(serverType: string | undefined | null): string {
  const normalized = String(serverType || '').trim().toLowerCase();

  switch (normalized) {
    case 'mssql':
    case 'mssql_druid':
    case 'sqlserver':
      return 'mssql';
    case 'mysql':
      return 'mysql';
    case 'mariadb':
      return 'mariadb';
    case 'pgsql':
    case 'postgres':
    case 'postgresql':
      return 'postgresql';
    case 'clickhouce':
    case 'clickhouse':
    case 'ck':
      return 'clickhouse';
    default:
      return normalized;
  }
}

export function getServerTypeLabel(serverType: string | undefined | null): string {
  const normalized = normalizeServerType(serverType);

  switch (normalized) {
    case 'postgresql':
      return 'pgsql';
    case 'clickhouse':
      return 'clickhouse';
    default:
      return normalized || '--';
  }
}

export function getEditorMode(serverType: string | undefined | null): string {
  const normalized = normalizeServerType(serverType);

  switch (normalized) {
    case 'postgresql':
      return 'text/x-pgsql';
    case 'mssql':
      return 'text/x-mssql';
    default:
      return 'text/x-mysql';
  }
}

export function isMysqlFamily(serverType: string | undefined | null): boolean {
  const normalized = normalizeServerType(serverType);
  return normalized === 'mysql' || normalized === 'mariadb';
}
