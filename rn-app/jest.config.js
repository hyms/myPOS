/** @type {import('ts-jest').JestConfigWithTsJest} */
module.exports = {
  preset: 'ts-jest',
  testEnvironment: 'node',
  roots: ['<rootDir>/src'],
  testMatch: ['**/__tests__/**/*.test.ts', '**/__tests__/**/*.test.tsx'],
  moduleNameMapper: {
    '^@/(.*)$': '<rootDir>/src/$1',
    '^@app/(.*)$': '<rootDir>/app/$1',
    '^expo-file-system$': '<rootDir>/src/__tests__/__mocks__/expo-file-system.ts',
    '^expo-sqlite$': '<rootDir>/src/__tests__/__mocks__/expo-sqlite.ts',
    '^expo-secure-store$': '<rootDir>/src/__tests__/__mocks__/expo-secure-store.ts',
    '^expo-image-manipulator$': '<rootDir>/src/__tests__/__mocks__/expo-image-manipulator.ts',
    '^expo-sharing$': '<rootDir>/src/__tests__/__mocks__/expo-sharing.ts',
    '^expo-document-picker$': '<rootDir>/src/__tests__/__mocks__/expo-document-picker.ts',
    '^@react-native-async-storage/async-storage$': '<rootDir>/src/__tests__/__mocks__/async-storage.ts',
    '^react-native-toast-message$': '<rootDir>/src/__tests__/__mocks__/react-native-toast-message.ts',
    '^expo-local-authentication$': '<rootDir>/src/__tests__/__mocks__/expo-local-authentication.ts',
    '^react-native$': '<rootDir>/src/__tests__/__mocks__/react-native.ts',
  },
  transform: {
    '^.+\\.tsx?$': [
      'ts-jest',
      {
        tsconfig: {
          jsx: 'react-jsx',
          module: 'commonjs',
          moduleResolution: 'node',
          strict: true,
          esModuleInterop: true,
          skipLibCheck: true,
          types: ['jest'],
          baseUrl: '.',
          paths: {
            '@/*': ['./src/*'],
            '@app/*': ['./app/*'],
          },
        },
        diagnostics: false,
      },
    ],
  },
};
