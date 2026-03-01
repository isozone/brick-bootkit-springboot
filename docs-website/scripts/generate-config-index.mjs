import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { extractConfigKeys } from './config-key-extractor.mjs';

const scriptDir = path.dirname(fileURLToPath(import.meta.url));
const docsWebsiteRoot = path.resolve(scriptDir, '..');
const repoRoot = path.resolve(docsWebsiteRoot, '..');
const outputJsonPath = path.resolve(docsWebsiteRoot, 'src/content/generated-config-keys.json');
const outputJsPath = path.resolve(docsWebsiteRoot, 'src/content/generated-config-keys.js');

function keyGroup(key) {
  if (key.startsWith('plugin.decrypt.plugins')) {
    return 'plugin.decrypt.plugins';
  }
  if (key.startsWith('plugin.decrypt')) {
    return 'plugin.decrypt';
  }
  if (key.startsWith('plugin.scripts.storage.file')) {
    return 'plugin.scripts.storage.file';
  }
  if (key.startsWith('plugin.scripts.storage.jdbc')) {
    return 'plugin.scripts.storage.jdbc';
  }
  if (key.startsWith('plugin.scripts.storage')) {
    return 'plugin.scripts.storage';
  }
  if (key.startsWith('plugin.web.')) {
    return 'plugin.web';
  }
  if (key.startsWith('plugin.monitoring.')) {
    return 'plugin.monitoring';
  }
  if (key.startsWith('plugin.configuration.')) {
    return 'plugin.configuration';
  }
  if (key.startsWith('plugin.scripts.')) {
    return 'plugin.scripts';
  }
  if (key.startsWith('plugin.')) {
    return 'plugin';
  }
  return 'other';
}

const keys = extractConfigKeys(repoRoot).map((key) => ({
  key,
  group: keyGroup(key)
}));

const groups = {};
keys.forEach((item) => {
  groups[item.group] = (groups[item.group] || 0) + 1;
});

const payload = {
  keyCount: keys.length,
  keys,
  groups
};

fs.writeFileSync(outputJsonPath, `${JSON.stringify(payload, null, 2)}\n`, 'utf8');
const jsModule = `const generatedConfigKeys = ${JSON.stringify(payload, null, 2)};\n\nexport default generatedConfigKeys;\n`;
fs.writeFileSync(outputJsPath, jsModule, 'utf8');
console.log(`Generated ${keys.length} config keys: ${outputJsonPath}`);
console.log(`Generated JS module: ${outputJsPath}`);
