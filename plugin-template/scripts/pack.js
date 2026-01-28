const fs = require('fs')
const path = require('path')
const archiver = require('archiver')

const ROOT = path.resolve(__dirname, '..')
const DIST = path.join(ROOT, 'dist')
const OUTPUT = path.join(ROOT, 'release')

async function pack() {
  // 读取 manifest
  const manifest = JSON.parse(fs.readFileSync(path.join(ROOT, 'manifest.json'), 'utf-8'))
  const pluginId = manifest.id
  const version = manifest.version
  const outputFileName = `${pluginId}-${version}.zip`

  // 确保输出目录存在
  if (!fs.existsSync(OUTPUT)) {
    fs.mkdirSync(OUTPUT, { recursive: true })
  }

  const outputPath = path.join(OUTPUT, outputFileName)
  const output = fs.createWriteStream(outputPath)
  const archive = archiver('zip', { zlib: { level: 9 } })

  output.on('close', () => {
    console.log(`✅ 插件打包完成: ${outputPath}`)
    console.log(`   大小: ${(archive.pointer() / 1024).toFixed(2)} KB`)
  })

  archive.on('error', (err) => {
    throw err
  })

  archive.pipe(output)

  // 添加 manifest.json
  archive.file(path.join(ROOT, 'manifest.json'), { name: 'manifest.json' })

  // 添加前端构建产物
  if (fs.existsSync(DIST)) {
    archive.directory(DIST, 'frontend')
  }

  // 添加后端 Groovy 代码
  const backendDir = path.join(ROOT, 'src/backend')
  if (fs.existsSync(backendDir)) {
    archive.directory(backendDir, 'backend')
  }

  // 添加数据库迁移脚本
  const migrationsDir = path.join(ROOT, 'migrations')
  if (fs.existsSync(migrationsDir)) {
    archive.directory(migrationsDir, 'migrations')
  }

  // 添加配置 schema
  const configSchema = path.join(ROOT, 'config.schema.json')
  if (fs.existsSync(configSchema)) {
    archive.file(configSchema, { name: 'config.schema.json' })
  }

  await archive.finalize()
}

pack().catch(console.error)
