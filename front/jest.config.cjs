module.exports = {
  rootDir: '.',
  testEnvironment: 'jsdom',
  setupFilesAfterEnv: ['<rootDir>/src/app/setupTests.ts'],
  transform: {
    '^.+\\.[jt]sx?$': [
      'babel-jest',
      {
        presets: [
          ['@babel/preset-env', { targets: { node: 'current' } }],
          ['@babel/preset-react', { runtime: 'automatic' }],
          '@babel/preset-typescript',
        ],
      },
    ],
  },
  moduleNameMapper: {
    '^.+\\.(css|less|sass|scss)$': 'identity-obj-proxy',
    '^.+\\.(gif|ttf|eot|svg|png|jpg|jpeg|webp)$': '<rootDir>/test/fileMock.cjs',
    '^@/(.*)$': '<rootDir>/src/$1',
  },
};
