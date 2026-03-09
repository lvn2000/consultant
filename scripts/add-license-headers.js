#!/usr/bin/env node

/**
 * License Header Checker/Adder for Frontend Applications
 * Adds MIT license headers to Vue, TypeScript, and JavaScript files
 */

const fs = require('fs');
const path = require('path');

const COPYRIGHT_YEAR = '2026';
const COPYRIGHT_HOLDER = 'Volodymyr Lubenchenko';

const MIT_LICENSE_HEADER = `/*
 * Copyright (c) ${COPYRIGHT_YEAR} ${COPYRIGHT_HOLDER}
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

`;

const SUPPORTED_EXTENSIONS = ['.vue', '.ts', '.js', '.tsx', '.jsx'];

const args = process.argv.slice(2);
const mode = args.includes('--check') ? 'check' : 'add';
const targetDir = args.find(arg => !arg.startsWith('--')) || '.';

let filesChecked = 0;
let filesMissing = 0;
let filesFixed = 0;

function hasLicenseHeader(content) {
  return content.trim().startsWith('/*') &&
         content.includes('Copyright (c)') &&
         content.includes(COPYRIGHT_HOLDER);
}

function addLicenseHeader(content) {
  // Handle shebang if present
  if (content.startsWith('#!')) {
    const firstNewline = content.indexOf('\n');
    if (firstNewline === -1) return content;
    return content.substring(0, firstNewline + 1) + MIT_LICENSE_HEADER + content.substring(firstNewline + 1);
  }
  return MIT_LICENSE_HEADER + content;
}

function processFile(filePath) {
  const relativePath = path.relative(targetDir, filePath);

  try {
    const content = fs.readFileSync(filePath, 'utf8');
    filesChecked++;

    if (hasLicenseHeader(content)) {
      console.log(`✓ ${relativePath}`);
      return;
    }

    filesMissing++;

    if (mode === 'check') {
      console.log(`✗ ${relativePath} - missing license header`);
    } else {
      const newContent = addLicenseHeader(content);
      fs.writeFileSync(filePath, newContent, 'utf8');
      filesFixed++;
      console.log(`+ ${relativePath} - added license header`);
    }
  } catch(error) {
    console.error(`Error processing ${filePath}:`, error.message);
  }
}

function walkDirectory(dir) {
  // Skip node_modules, dist, build, .git, and other common directories
  const skipDirs = ['node_modules', 'dist', 'build', '.git', '.nuxt', '.output', 'coverage', '.vuepress'];

  const files = fs.readdirSync(dir);

  for (const file of files) {
    const filePath = path.join(dir, file);
    const stat = fs.statSync(filePath);

    if (stat.isDirectory()) {
      const dirName = path.basename(filePath);
      if (!skipDirs.includes(dirName)) {
        walkDirectory(filePath);
      }
    } else if (stat.isFile()) {
      const ext = path.extname(file);
      if (SUPPORTED_EXTENSIONS.includes(ext)) {
        processFile(filePath);
      }
    }
  }
}

console.log(`\n📝 License Header ${mode === 'check' ? 'Checker' : 'Adder'}`);
console.log(`━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━`);
console.log(`Target directory: ${path.resolve(targetDir)}`);
console.log(`Extensions: ${SUPPORTED_EXTENSIONS.join(', ')}`);
console.log(`━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n`);

walkDirectory(path.resolve(targetDir));

console.log(`\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━`);
console.log(`Files checked: ${filesChecked}`);
if (mode === 'check') {
  console.log(`Missing headers: ${filesMissing}`);
} else {
  console.log(`Headers added: ${filesFixed}`);
}
console.log(`━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n`);

// Exit with error code if checking and files are missing headers
if (mode === 'check' && filesMissing> 0) {
  console.log(`❌ Run with '--add' flag to automatically add missing headers\n`);
  process.exit(1);
}

if (mode === 'add') {
  console.log(`✅ All files now have license headers!\n`);
}
