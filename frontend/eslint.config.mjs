// MVP-01, REQ-QUALITY
import globals from 'globals';
import typescriptEslint from 'typescript-eslint';
import { javascript, typescript } from '@apocarteres/project-conventions/configs/eslint';

export default typescriptEslint.config(
  { ignores: ['dist/**', 'out-tsc/**', '.angular/**', 'coverage/**'] },
  ...javascript({ globals: globals.node }),
  ...typescript(typescriptEslint, { globals: globals.browser }),
  {
    files: ['**/*.ts'],
    languageOptions: {
      parserOptions: {
        projectService: false,
        project: ['./projects/admin/tsconfig.app.json', './projects/bot/tsconfig.app.json'],
        tsconfigRootDir: import.meta.dirname,
      },
    },
  },
);
