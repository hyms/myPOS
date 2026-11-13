module.exports = {
  root: true,
  extends: ['expo', 'plugin:tailwindcss/recommended'],
  plugins: ['tailwindcss'],
  settings: {
    tailwindcss: {
      callees: ['cn', 'tw'],
      config: 'tailwind.config.js',
    },
  },
  ignorePatterns: ['node_modules/', '.expo/', 'dist/'],
};
