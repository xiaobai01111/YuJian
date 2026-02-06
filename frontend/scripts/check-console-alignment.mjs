import fs from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const __filename = fileURLToPath(import.meta.url)
const __dirname = path.dirname(__filename)
const frontendRoot = path.resolve(__dirname, '..')
const backendSeed = path.resolve(frontendRoot, '..', 'backend', 'src', 'main', 'resources', 'db', 'migration', 'V2__seed.sql')
const frontendSrc = path.resolve(frontendRoot, 'src')
const sidebarItemFile = path.resolve(frontendRoot, 'src', 'views', 'console', 'layout', 'ConsoleSidebarItem.vue')

function listFiles(dir, exts) {
  const out = []
  const stack = [dir]
  while (stack.length > 0) {
    const cur = stack.pop()
    if (!cur) continue
    for (const entry of fs.readdirSync(cur, { withFileTypes: true })) {
      const full = path.join(cur, entry.name)
      if (entry.isDirectory()) {
        stack.push(full)
      } else if (exts.has(path.extname(entry.name))) {
        out.push(full)
      }
    }
  }
  return out
}

function collectFrontendPerms() {
  const files = listFiles(frontendSrc, new Set(['.vue', '.ts']))
  const permRe = /['"`]([a-z]+:[a-z0-9-]+:[a-z0-9-]+(?:[:a-z0-9-]+)?)['"`]/g
  const perms = new Set()
  for (const file of files) {
    const content = fs.readFileSync(file, 'utf8')
    let m
    while ((m = permRe.exec(content)) !== null) {
      perms.add(m[1])
    }
  }
  return perms
}

function collectSeedPerms(seedSql) {
  const permRe = /'([a-z]+:[a-z0-9-]+:[a-z0-9-]+(?:[:a-z0-9-]+)?)'/g
  const perms = new Set()
  let m
  while ((m = permRe.exec(seedSql)) !== null) {
    perms.add(m[1])
  }
  return perms
}

function collectSeedComponents(seedSql) {
  const compRe = /'((?:views\/console\/)[^']+\.vue)'/g
  const comps = new Set()
  let m
  while ((m = compRe.exec(seedSql)) !== null) {
    comps.add(m[1])
  }
  return comps
}

function collectSeedIcons(seedSql) {
  const iconRe = /\(\d+,\s*\d+,\s*'[^']+',\s*'[^']*',\s*'[^']*',\s*[01],\s*'([^']+)'/g
  const icons = new Set()
  let m
  while ((m = iconRe.exec(seedSql)) !== null) {
    icons.add(m[1])
  }
  return icons
}

function collectSidebarIconKeys() {
  const content = fs.readFileSync(sidebarItemFile, 'utf8')
  const keyRe = /\n\s*(?:'([^']+)'|([a-z][a-z0-9-]*)):\s*'/g
  const keys = new Set()
  let m
  while ((m = keyRe.exec(content)) !== null) {
    keys.add(m[1] || m[2])
  }
  return keys
}

if (!fs.existsSync(backendSeed)) {
  console.error(`Missing backend seed file: ${backendSeed}`)
  process.exit(1)
}

const seedSql = fs.readFileSync(backendSeed, 'utf8')
const frontendPerms = collectFrontendPerms()
const seedPerms = collectSeedPerms(seedSql)
const seedComponents = collectSeedComponents(seedSql)
const seedIcons = collectSeedIcons(seedSql)
const sidebarIcons = collectSidebarIconKeys()

const frontendOnlyPerms = [...frontendPerms].filter((p) => !seedPerms.has(p)).sort()
const missingComponentFiles = [...seedComponents]
  .filter((c) => !fs.existsSync(path.resolve(frontendRoot, 'src', c)))
  .sort()
const missingSidebarIcons = [...seedIcons].filter((i) => !sidebarIcons.has(i)).sort()

let hasError = false

if (frontendOnlyPerms.length > 0) {
  hasError = true
  console.error('\n[FAIL] Frontend uses permissions not found in backend seed:')
  for (const p of frontendOnlyPerms) {
    console.error(`- ${p}`)
  }
}

if (missingComponentFiles.length > 0) {
  hasError = true
  console.error('\n[FAIL] Backend menu components missing in frontend:')
  for (const c of missingComponentFiles) {
    console.error(`- ${c}`)
  }
}

if (missingSidebarIcons.length > 0) {
  hasError = true
  console.error('\n[FAIL] Backend menu icons missing in ConsoleSidebarItem icon map:')
  for (const i of missingSidebarIcons) {
    console.error(`- ${i}`)
  }
}

if (hasError) {
  process.exit(1)
}

console.log('[PASS] Console alignment check passed.')
console.log(`- Frontend permissions: ${frontendPerms.size}`)
console.log(`- Backend permissions: ${seedPerms.size}`)
console.log(`- Seed console components checked: ${seedComponents.size}`)
console.log(`- Seed icons covered: ${seedIcons.size}`)
