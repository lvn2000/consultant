# Frontend License Header Scripts

Scripts for managing MIT license headers in frontend applications (Vue, TypeScript, JavaScript).

## Usage

### Check for Missing License Headers

```bash
# From any frontend app directory (admin-app, client-app, specialist-app)
npm run license:check

# Or run directly
node ../scripts/add-license-headers.js --check.
```

### Add Missing License Headers

```bash
# From any frontend app directory
npm run license:add

# Or run directly
node ../scripts/add-license-headers.js --add .
```

### Check Specific Directory

```bash
node ../scripts/add-license-headers.js --check ./components
node ../scripts/add-license-headers.js --add ./composables
```

## Supported File Types

- `.vue` - Vue components
- `.ts` - TypeScript files
- `.js` - JavaScript files
- `.tsx` - TypeScript JSX files
- `.jsx` - JavaScript JSX files

## Automatically Skipped Directories

- `node_modules`
- `dist`
- `build`
- `.git`
- `.nuxt`
- `.output`
- `coverage`
- `.vuepress`

## License Format

The script adds the following MIT license header:

```javascript
/*
 * Copyright (c) 2026 Volodymyr Lubenchenko
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

```

## CI/CD Integration

Add to your CI pipeline to ensure all files have license headers:

```yaml
# Example GitHub Actions step
- name: Check license headers
  run: |
   cd admin-app && npm run license:check
   cd ../client-app && npm run license:check
   cd ../specialist-app && npm run license:check
```

## Exit Codes

- `0` - All files have license headers (or successfully added them)
- `1` - Some files are missing license headers (when using `--check`)
