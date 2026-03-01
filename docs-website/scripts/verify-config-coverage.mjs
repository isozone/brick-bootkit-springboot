import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { extractConfigKeys } from './config-key-extractor.mjs';

const scriptDir = path.dirname(fileURLToPath(import.meta.url));
const docsWebsiteRoot = path.resolve(scriptDir, '..');
const repoRoot = path.resolve(docsWebsiteRoot, '..');
const docsContentPath = path.resolve(docsWebsiteRoot, 'src/content/docs.js');
const generatedIndexPath = path.resolve(docsWebsiteRoot, 'src/content/generated-config-keys.json');

const docsContent = fs.readFileSync(docsContentPath, 'utf8');
const expectedKeys = extractConfigKeys(repoRoot);
const missingKeys = expectedKeys.filter((key) => !docsContent.includes(key));

if (!fs.existsSync(generatedIndexPath)) {
  console.error('Missing generated config index file: src/content/generated-config-keys.json');
  console.error('Run: npm run generate-config-index');
  process.exit(1);
}

const generated = JSON.parse(fs.readFileSync(generatedIndexPath, 'utf8'));
const generatedKeys = (generated.keys || []).map((item) => item.key);
const generatedMissing = expectedKeys.filter((key) => !generatedKeys.includes(key));
const generatedExtra = generatedKeys.filter((key) => !expectedKeys.includes(key));

if (missingKeys.length > 0) {
  console.error(`Found ${missingKeys.length} undocumented config key(s):`);
  for (const key of missingKeys) {
    console.error(`- ${key}`);
  }
  process.exit(1);
}

if (generatedMissing.length > 0 || generatedExtra.length > 0) {
  console.error('Generated config index is out of date.');
  if (generatedMissing.length > 0) {
    console.error('Missing in generated file:');
    for (const key of generatedMissing) {
      console.error(`- ${key}`);
    }
  }
  if (generatedExtra.length > 0) {
    console.error('Extra keys in generated file:');
    for (const key of generatedExtra) {
      console.error(`- ${key}`);
    }
  }
  console.error('Run: npm run generate-config-index');
  process.exit(1);
}

console.log(`Verified config coverage: ${expectedKeys.length} keys documented (auto-extracted from source).`);
