# Console Menu Alignment Plan (Enterprise)

## 1. Scope
- Backend source of truth: `backend/src/main/resources/db/migration/V2__seed.sql`
- Backend route API: `GET /api/v1/system/menu/routes`
- Frontend sidebar/router: `frontend/src/views/console/layout/*`, `frontend/src/router/index.ts`

## 2. Target Principles
- Single source of truth: menu, route, permission all come from backend.
- Frontend only renders and enforces, never hard-codes business grouping by title.
- Menu visibility and API permission must be consistent to avoid “visible but unusable”.
- Any menu/permission drift must fail in CI before release.

## 3. Current Menu Tree Baseline
- `仪表盘`
- `系统管理`
  - `用户管理`
  - `角色管理`
  - `部门管理`
  - `认证规则`
  - `敏感词管理`
  - `公告管理`
  - `登录日志`
  - `操作日志`
  - `个人中心`
  - `邮箱管理`
- `内容管理`
  - `帖子管理`
  - `评论管理`
  - `举报管理`
- `系统监控`
  - `在线用户`
  - `服务监控`
  - `Redis监控`
  - `阻止名单`
- `系统工具`
  - `回收站`
    - `帖子回收站`
    - `评论回收站`
    - `举报回收站`
  - `文件管理`
  - `图库管理`
- `校园管理`
  - `身份审核`
  - `Hero管理`

## 4. Issues Found
- Sidebar grouping hard-coded by title; renaming menus can break grouping semantics.
- Route access check only handled shallow children; deep menus risk false denial.
- Icon dictionary missed backend-defined keys in some cases.
- Path governance conflict:
  - seed child paths are absolute (`/console/...`)
  - service validation expects child paths to be relative.

## 5. Changes Applied (Phase 1)
- Removed title-based split in sidebar and render backend menu tree directly.
- Replaced shallow route permission check with recursive path matching.
- Added missing icon keys for backend-defined icons.
- Added alignment script:
  - verifies frontend permission keys exist in backend seed
  - verifies backend menu component paths exist in frontend
  - verifies backend icon keys are covered by sidebar icon map
  - command: `pnpm run check:console-align` (in `frontend`)

## 6. Progress & Next Phases
- Phase 2 (contract) completed:
  - backend route contract now includes `groupCode`, `featureCode`, `routeType`.
  - frontend sidebar now groups by backend `groupCode` contract.
  - DB migration added: `backend/src/main/resources/db/migration/V3__menu_group_code.sql`.
- Phase 3 (governance):
  - enforce `check:console-align` in CI
  - add DB migration checks for menu path rules
  - add E2E smoke for each role’s menu visibility + button operability + API permission.

## 7. Acceptance Criteria
- Same role sees same menu tree, has matching route access, and only actionable buttons.
- No frontend permission key exists without backend definition.
- No backend menu component path points to non-existent frontend file.
- Any drift blocks merge/release automatically.
