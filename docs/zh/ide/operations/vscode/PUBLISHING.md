# 发布 VS Code 扩展

本指南介绍如何构建、打包并将 JiuwenSwarm VS Code 扩展发布到 VS Code 应用商店和 OpenVSX
注册表。

## 从源码快速构建

如果只想在本地构建扩展并手动安装：

```bash
cd packages/vscode-extension
npm install          # 安装 esbuild + TypeScript 开发依赖
npm run build        # 一次性编译 → out/extension.js
npx vsce package --no-dependencies   # → jiuwenswarm-0.1.0.vsix
```

安装生成的 VSIX：

```bash
code --install-extension jiuwenswarm-0.1.0.vsix
```

---

## 开发构建

```bash
cd packages/vscode-extension
npm install
npm run build      # 一次性 → out/extension.js
npm run watch      # 监听模式
```

在 VS Code 中打开 `packages/vscode-extension` 并按 **F5** 启动扩展开发主机。

## 打包分发

```bash
npm install -g @vscode/vsce
npx vsce package --no-dependencies   # → jiuwenswarm-0.1.0.vsix
```

> 使用 `--no-dependencies` 是因为运行时依赖（`ws`）未打包；扩展通过宿主机的原生 WebSocket 连接。

本地安装 VSIX 进行测试：

```bash
code --install-extension jiuwenswarm-0.1.0.vsix
```

## VS Code 应用商店

### 一次性发布者设置

1. 使用 Microsoft 账户登录 [marketplace.visualstudio.com/manage](https://marketplace.visualstudio.com/manage)。
2. 点击**创建发布者**，使用 `openjiuwen` 作为发布者 ID（必须与 `package.json` 中的
   `"publisher"` 一致）。

### 生成个人访问令牌（PAT）

1. 前往 [dev.azure.com](https://dev.azure.com) → 你的组织 → **用户设置 → 个人访问令牌**。
2. 点击**新建令牌**：
   - **范围**：自定义定义 → **应用商店 → 管理**（或完全访问）
   - **过期时间**：设置为至少 1 年
3. 复制令牌。

### 确保所需的 package.json 字段

应用商店要求：

```json
{
  "repository": {
    "type": "git",
    "url": "https://github.com/openjiuwen/jiuwenswarm-ide"
  },
  "license": "MIT",
  "homepage": "https://github.com/openjiuwen/jiuwenswarm-ide#readme",
  "galleryBanner": {
    "color": "#1e1e2e",
    "theme": "dark"
  }
}
```

同时确保 `resources/icon.png` 存在且为 128×128 像素。

### 安装 vsce 并登录

```bash
npm install -g @vscode/vsce
vsce login openjiuwen
# 提示时粘贴你的 PAT
```

### 发布

```bash
cd packages/vscode-extension
npm run build
vsce publish
```

自动提升版本：

```bash
vsce publish minor   # 0.1.0 → 0.2.0
vsce publish patch   # 0.1.0 → 0.1.1
vsce publish major   # 0.1.0 → 1.0.0
```

或使用显式令牌发布：

```bash
vsce publish --pat "$VSCE_PAT"
```

## OpenVSX

[Open VSX](https://open-vsx.org) 是 VSCodium、Gitpod、Eclipse Theia 及其他 VS Code 兼容
编辑器使用的社区注册表。

### 创建账户和命名空间

1. 前往 [open-vsx.org](https://open-vsx.org) → **使用 GitHub 登录**。
2. 导航到 **用户设置 → 访问令牌** → 生成令牌。
3. 在**命名空间**标签页中，认领 `openjiuwen` 命名空间。

### 发布

```bash
npm install -g ovsx
cd packages/vscode-extension
npm run build
vsce package   # 生成 jiuwenswarm-0.1.0.vsix
ovsx publish jiuwenswarm-0.1.0.vsix --pat "$OVSX_PAT"
```

## CI 自动化（GitHub Actions）

创建 `.github/workflows/publish-vscode.yml`：

```yaml
name: Publish VS Code Extension

on:
  push:
    tags: ['vscode-v*']

jobs:
  publish:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - uses: actions/setup-node@v4
        with:
          node-version: 20

      - name: Install dependencies
        working-directory: packages/vscode-extension
        run: npm ci

      - name: Build
        working-directory: packages/vscode-extension
        run: npm run build

      - name: Publish to Marketplace
        working-directory: packages/vscode-extension
        run: npx vsce publish --pat "${{ secrets.VSCE_PAT }}"

      - name: Publish to OpenVSX
        working-directory: packages/vscode-extension
        run: |
          npx vsce package
          npx ovsx publish jiuwenswarm-*.vsix --pat "${{ secrets.OVSX_PAT }}"
```

将 `VSCE_PAT` 和 `OVSX_PAT` 添加为仓库机密。

## 发布清单

- [ ] 提升 `packages/vscode-extension/package.json` 中的 `version`
- [ ] 本地安装构建好的 VSIX 并冒烟测试关键流程
- [ ] 为发布打标签：`git tag vscode-vX.Y.Z`
- [ ] 推送标签到远程——CI 工作流会自动拾取
