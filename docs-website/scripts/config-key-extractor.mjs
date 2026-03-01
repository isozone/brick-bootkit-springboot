import fs from 'node:fs';
import path from 'node:path';

function readFile(repoRoot, relPath) {
  return fs.readFileSync(path.resolve(repoRoot, relPath), 'utf8');
}

function extractClassBody(source, className) {
  const classPattern = new RegExp(`\\bclass\\s+${className}\\b`);
  const classMatch = classPattern.exec(source);
  if (!classMatch) {
    throw new Error(`Class "${className}" not found.`);
  }

  const startBrace = source.indexOf('{', classMatch.index);
  if (startBrace < 0) {
    throw new Error(`Class "${className}" has no body.`);
  }

  let depth = 0;
  for (let i = startBrace; i < source.length; i += 1) {
    const ch = source[i];
    if (ch === '{') {
      depth += 1;
      continue;
    }
    if (ch === '}') {
      depth -= 1;
      if (depth === 0) {
        return source.slice(startBrace + 1, i);
      }
    }
  }

  throw new Error(`Class "${className}" body parse failed.`);
}

function extractFields(classBody) {
  const fields = [];
  const fieldRegex = /^\s*private\s+(?!static\b)(?!final\b)([^;=\n()]+?)\s+([A-Za-z_][A-Za-z0-9_]*)\s*(?:=[^;]*)?;\s*$/;
  const lines = classBody.split(/\r?\n/);
  let depth = 0;

  for (const line of lines) {
    if (depth === 0) {
      const match = line.match(fieldRegex);
      if (match) {
        fields.push({
          type: match[1].trim(),
          name: match[2].trim()
        });
      }
    }

    const opens = (line.match(/\{/g) || []).length;
    const closes = (line.match(/\}/g) || []).length;
    depth += opens - closes;
    if (depth < 0) {
      depth = 0;
    }
  }

  return fields;
}

function parseConditionalKeys(source) {
  const keys = [];
  const condRegex = /@ConditionalOnProperty\s*\(([\s\S]*?)\)/g;
  let condMatch = condRegex.exec(source);
  while (condMatch) {
    const args = condMatch[1];
    const prefixMatch = args.match(/prefix\s*=\s*"([^"]+)"/);
    const singleNameMatch = args.match(/name\s*=\s*"([^"]+)"/);
    const arrayNameMatch = args.match(/name\s*=\s*\{([^}]*)\}/);

    const prefix = prefixMatch ? prefixMatch[1].trim() : '';
    const names = [];
    if (singleNameMatch) {
      names.push(singleNameMatch[1].trim());
    } else if (arrayNameMatch) {
      const extracted = arrayNameMatch[1]
        .split(',')
        .map((item) => item.trim())
        .map((item) => item.replace(/^"+|"+$/g, ''))
        .filter(Boolean);
      names.push(...extracted);
    }

    names.forEach((name) => {
      if (prefix) {
        keys.push(`${prefix}.${name}`);
      } else {
        keys.push(name);
      }
    });
    condMatch = condRegex.exec(source);
  }
  return keys;
}

const classSpecs = [
  {
    file: 'spring-boot3-brick-bootkit/src/main/java/com/zqzqq/bootkits/integration/AutoIntegrationConfiguration.java',
    className: 'AutoIntegrationConfiguration',
    prefix: 'plugin',
    exclude: ['decrypt']
  },
  {
    file: 'spring-boot3-brick-bootkit/src/main/java/com/zqzqq/bootkits/integration/decrypt/DecryptConfiguration.java',
    className: 'DecryptConfiguration',
    prefix: 'plugin.decrypt',
    exclude: []
  },
  {
    file: 'spring-boot3-brick-bootkit/src/main/java/com/zqzqq/bootkits/integration/decrypt/DecryptPluginConfiguration.java',
    className: 'DecryptPluginConfiguration',
    prefix: 'plugin.decrypt.plugins',
    exclude: []
  },
  {
    file: 'spring-boot3-brick-bootkit-web/src/main/java/com/zqzqq/bootkits/web/config/BrickWebProperties.java',
    className: 'BrickWebProperties',
    prefix: 'plugin.web',
    exclude: []
  },
  {
    file: 'spring-boot3-brick-bootkit/src/main/java/com/zqzqq/bootkits/integration/monitoring/PluginMonitoringProperties.java',
    className: 'PluginMonitoringProperties',
    prefix: 'plugin.monitoring',
    exclude: []
  },
  {
    file: 'spring-boot3-brick-bootkit-core/src/main/java/com/zqzqq/bootkits/core/config/PluginConfigurationProperties.java',
    className: 'PluginConfigurationProperties',
    prefix: 'plugin.configuration',
    exclude: []
  },
  {
    file: 'spring-boot3-brick-bootkit-web/src/main/java/com/zqzqq/bootkits/web/scripts/config/ScriptStorageProperties.java',
    className: 'ScriptStorageProperties',
    prefix: 'plugin.scripts.storage',
    exclude: ['file', 'jdbc']
  },
  {
    file: 'spring-boot3-brick-bootkit-web/src/main/java/com/zqzqq/bootkits/web/scripts/config/ScriptStorageProperties.java',
    className: 'FileStorage',
    prefix: 'plugin.scripts.storage.file',
    exclude: []
  },
  {
    file: 'spring-boot3-brick-bootkit-web/src/main/java/com/zqzqq/bootkits/web/scripts/config/ScriptStorageProperties.java',
    className: 'JdbcStorage',
    prefix: 'plugin.scripts.storage.jdbc',
    exclude: []
  }
];

const conditionalSourceFiles = [
  'spring-boot3-brick-bootkit-web/src/main/java/com/zqzqq/bootkits/web/config/BrickWebCorsConfiguration.java',
  'spring-boot3-brick-bootkit-web/src/main/java/com/zqzqq/bootkits/web/scripts/config/ScriptStorageAutoConfiguration.java'
];

export function extractConfigKeys(repoRoot) {
  const expectedSet = new Set();

  for (const spec of classSpecs) {
    const source = readFile(repoRoot, spec.file);
    const classBody = extractClassBody(source, spec.className);
    const fields = extractFields(classBody);
    fields
      .filter((field) => !spec.exclude.includes(field.name))
      .forEach((field) => {
        expectedSet.add(`${spec.prefix}.${field.name}`);
      });
  }

  for (const sourceFile of conditionalSourceFiles) {
    const source = readFile(repoRoot, sourceFile);
    parseConditionalKeys(source)
      .filter((key) => key.startsWith('plugin.'))
      .forEach((key) => expectedSet.add(key));
  }

  return Array.from(expectedSet).sort();
}
