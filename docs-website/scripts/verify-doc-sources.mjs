import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath, pathToFileURL } from 'node:url';

const scriptDir = path.dirname(fileURLToPath(import.meta.url));
const docsWebsiteRoot = path.resolve(scriptDir, '..');
const docsModuleUrl = pathToFileURL(path.resolve(docsWebsiteRoot, 'src/content/docs.js')).href;
const docsModule = await import(docsModuleUrl);
const docPages = docsModule.docPages || [];

const missing = [];

for (const page of docPages) {
  for (const section of page.sections || []) {
    for (const source of section.sources || []) {
      if (typeof source !== 'string') {
        continue;
      }

      const rawSource = source.trim();
      if (!rawSource || rawSource.startsWith('http://') || rawSource.startsWith('https://')) {
        continue;
      }
      const sourcePath = rawSource.split(':')[0].trim();

      const absolutePath = path.resolve(docsWebsiteRoot, '..', sourcePath);
      if (!fs.existsSync(absolutePath)) {
        missing.push(`${page.path} [${section.id}] -> ${source}`);
      }
    }
  }
}

if (missing.length > 0) {
  console.error(`Found ${missing.length} missing source path(s):`);
  for (const item of missing) {
    console.error(`- ${item}`);
  }
  process.exit(1);
}

console.log(`Verified ${docPages.length} doc page(s), all source paths exist.`);
