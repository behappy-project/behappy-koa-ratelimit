import resolve from '@rollup/plugin-node-resolve';
import commonjs from '@rollup/plugin-commonjs';
import autoExternal from 'rollup-plugin-auto-external';


export default {
  input: './lib/core.js',
  output: {
    file: `./dist/core.cjs`,
    format: 'cjs',
    generatedCode: {
      constBindings: true,
      objectShorthand: true,
    },
  },
  plugins: [
    autoExternal(),
    resolve(),
    commonjs(),
  ],
};
