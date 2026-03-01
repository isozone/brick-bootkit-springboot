import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const scriptDir = path.dirname(fileURLToPath(import.meta.url));
const docsWebsiteRoot = path.resolve(scriptDir, '..');
const generatedIndexPath = path.resolve(docsWebsiteRoot, 'src/content/generated-config-keys.json');
const outputDir = path.resolve(docsWebsiteRoot, 'output');
const outputJsonPath = path.resolve(outputDir, 'config-key-summary.json');
const outputMdPath = path.resolve(outputDir, 'config-key-summary.md');

if (!fs.existsSync(generatedIndexPath)) {
  console.error('Missing generated config index: src/content/generated-config-keys.json');
  console.error('Run: npm run generate-config-index');
  process.exit(1);
}

const generated = JSON.parse(fs.readFileSync(generatedIndexPath, 'utf8'));
const keys = Array.isArray(generated.keys) ? generated.keys : [];
const groups = generated.groups || {};
const sortedGroups = Object.entries(groups).sort((a, b) => a[0].localeCompare(b[0]));
const sortedKeys = keys.map((item) => item.key).sort((a, b) => a.localeCompare(b));

fs.mkdirSync(outputDir, { recursive: true });

const summaryJson = {
  keyCount: generated.keyCount || keys.length,
  groupCount: sortedGroups.length,
  groups: sortedGroups.map(([group, count]) => ({ group, count })),
  keys: sortedKeys
};
fs.writeFileSync(outputJsonPath, `${JSON.stringify(summaryJson, null, 2)}\n`, 'utf8');

const markdownLines = [];
markdownLines.push('# Config Key Summary');
markdownLines.push('');
markdownLines.push(`- Total keys: ${summaryJson.keyCount}`);
markdownLines.push(`- Total groups: ${summaryJson.groupCount}`);
markdownLines.push('');
markdownLines.push('## Group Counts');
markdownLines.push('');
markdownLines.push('| Group | Count |');
markdownLines.push('| --- | ---: |');
for (const item of summaryJson.groups) {
  markdownLines.push(`| ${item.group} | ${item.count} |`);
}
markdownLines.push('');
markdownLines.push('## Keys');
markdownLines.push('');
for (const key of summaryJson.keys) {
  markdownLines.push(`- \`${key}\``);
}
markdownLines.push('');

fs.writeFileSync(outputMdPath, `${markdownLines.join('\n')}\n`, 'utf8');

console.log(`Generated summary JSON: ${outputJsonPath}`);
console.log(`Generated summary Markdown: ${outputMdPath}`);
