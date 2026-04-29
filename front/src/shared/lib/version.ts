export function normalizeVersion(version: string | undefined | null): string {
  let normalized = String(version || '').trim();

  while (normalized.startsWith('v') || normalized.startsWith('V')) {
    normalized = normalized.slice(1).trim();
  }

  return normalized;
}

export function formatVersionLabel(version: string | undefined | null): string {
  const normalized = normalizeVersion(version);
  return normalized ? `v${normalized}` : '--';
}
