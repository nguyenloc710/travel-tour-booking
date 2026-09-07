import nextConfig from 'eslint-config-next/core-web-vitals';

// eslint-config-next 16 xuất thẳng mảng flat config — không bọc qua FlatCompat.
const config = [
  { ignores: ['.next/**', 'node_modules/**', 'next-env.d.ts'] },
  ...nextConfig,
];

export default config;
