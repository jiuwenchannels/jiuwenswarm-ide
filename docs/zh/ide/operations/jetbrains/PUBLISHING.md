# 发布 JetBrains 插件

本指南介绍如何构建、签名并将 JiuwenSwarm 插件发布到 JetBrains 插件应用商店。

## 从源码快速构建

如果只想在本地构建插件并手动安装：

```bash
cd packages/jetbrains-plugin
.\gradlew.bat --no-daemon buildPlugin -x buildSearchableOptions
# 或 macOS/Linux：
# ./gradlew buildPlugin -x buildSearchableOptions
```

插件 ZIP 生成于：

```
build/distributions/jiuwenswarm-plugin-0.1.0.zip
```

通过 **设置 → 插件 → ⚙ → 从磁盘安装插件…** 安装并选择该 ZIP。

---

## 开发构建

```bash
cd packages/jetbrains-plugin
./gradlew buildPlugin   # → build/distributions/*.zip
```

需要 JDK 17 和 Gradle 8.7。

## 一次性账户设置

1. 在 [plugins.jetbrains.com](https://plugins.jetbrains.com) 登录（或创建账户）。
2. 进入 **上传插件** → 确认你的供应商信息。
3. `plugin.xml` 中的 `<vendor>` 标签必须与你的注册账户一致：
   ```xml
   <vendor email="dev@openjiuwen.com" url="https://github.com/openjiuwen">OpenJiuwen</vendor>
   ```

## 生成应用商店令牌

1. 前往 [plugins.jetbrains.com/author/me](https://plugins.jetbrains.com/author/me)。
2. 在**令牌**下点击**生成新令牌**——命名为 `jiuwenswarm-publish`。
3. 立即复制令牌（仅显示一次）。
4. 将其存为环境变量（绝不硬编码）：
   ```bash
   export JETBRAINS_MARKETPLACE_TOKEN=perm:...
   ```

## 验证插件

插件验证器会检查与所有目标 IDE 版本的兼容性：

```bash
cd packages/jetbrains-plugin
./gradlew verifyPlugin
```

提交前修复任何报告的错误。常见问题：
- 访问内部 `@ApiStatus.Internal` API
- 缺少 `since-build` / `until-build` 范围
- Kotlin stdlib 版本不兼容

## 更新发布元数据

每次发布前，更新 `build.gradle.kts` 中的 `changeNotes` 并提升 `version`：

```kotlin
version = "0.2.0"

intellijPlatform {
    pluginConfiguration {
        changeNotes = """
            <b>0.1.0</b>
            <ul>
              <li>Streaming chat panel with real-time token output and full Markdown rendering</li>
              <li>Thinking / reasoning block support (collapsible)</li>
              <li>Tool call cards with collapsible inputs and outputs</li>
              <li>Mode selector: Plan &amp; Execute, Execute, Team Coding</li>
              <li>Default mode applied from settings on every connect</li>
              <li>Model selector with live model list from server</li>
              <li>@ file mention picker — inline workspace file autocomplete</li>
              <li># skill picker — select skills directly from the chat input</li>
              <li>! preset prompt templates — eight built-in prompt shortcuts</li>
              <li>Session management: create, switch, resume, delete</li>
              <li>Session history loading after switch</li>
              <li>Skills panel with ON/OFF toggle</li>
              <li>Checkpoint / rewind — undo all file edits from last agent turn</li>
              <li>Context bar showing model context window occupancy</li>
              <li>Session stats chips (turns, tokens, cost, tool calls, latency, TTFT)</li>
              <li>Mini bar charts (tokens and duration per turn)</li>
              <li>Server memory usage chip (live RSS / available)</li>
              <li>Git status chip (branch + changed file count)</li>
              <li>Git quick actions: Commit and Push (optional, enabled in settings)</li>
              <li>IDE context injection: active file, cursor, selection, diagnostics, open tabs, project tree, git, project rules, @-mentioned files</li>
              <li>Project rules auto-injection from .jiuwenswarm/instructions.md, .jiuwenswarm/rules.md, or AGENTS.md</li>
              <li>Side-by-side diff window for agent file edits</li>
              <li>Auto-apply mode (skip diff, undoable with Ctrl+Z)</li>
              <li>Approval prompt before applying file edits</li>
              <li>Agent shell commands routed to IDE terminal (JiuwenSwarm tab)</li>
              <li>Alt+Enter quick fix: Fix with JiuwenSwarm</li>
              <li>Send Selection action (Ctrl+Shift+E / ⌘⇧E)</li>
              <li>New Session shortcut (Ctrl+Shift+J / ⌘⇧J)</li>
              <li>Clickable file links in agent responses (open file at line)</li>
              <li>Symbol navigation links (PascalCase / SCREAMING_SNAKE)</li>
              <li>Connection status bar widget with token count</li>
              <li>Dark / light / auto theme</li>
              <li>Debug log panel</li>
              <li>Image attachments (PNG, JPEG, WebP, GIF)</li>
              <li>E2A streaming protocol support</li>
              <li>Exponential backoff reconnect (1 s → 30 s)</li>
              <li>Keep-alive ping frames (configurable interval)</li>
            </ul>
        """.trimIndent()
    }
}
```

## 配置 Gradle 发布

取消注释并填写 `build.gradle.kts` 中的 `publishing` 块：

```kotlin
intellijPlatform {
    publishing {
        token = providers.environmentVariable("JETBRAINS_MARKETPLACE_TOKEN")
        // channels = listOf("beta")
    }
}
```

## 签名插件

JetBrains 要求插件签名。生成证书链和私钥：

```bash
# 安装 JetBrains 插件签名 CLI
brew install jetbrains/tools/marketplace-zip-signer   # macOS
# 或从 https://github.com/JetBrains/marketplace-zip-signer/releases 下载

# 生成自签名证书（有效期 10 年）
marketplace-zip-signer generate-certificate \
  --output private.pem \
  --cert chain.crt \
  --password "$KEY_PASSWORD"
```

在 `build.gradle.kts` 中配置 `signing`：

```kotlin
signing {
    certificateChain = providers.environmentVariable("JB_CERT_CHAIN")
    privateKey        = providers.environmentVariable("JB_PRIVATE_KEY")
    password          = providers.environmentVariable("JB_KEY_PASSWORD")
}
```

将 PEM 内容存为环境变量：

```bash
export JB_CERT_CHAIN=$(cat chain.crt)
export JB_PRIVATE_KEY=$(cat private.pem)
export JB_KEY_PASSWORD=your_passphrase
```

## 构建、签名并发布

```bash
cd packages/jetbrains-plugin
./gradlew signPlugin publishPlugin
```

这会依次运行：
1. `buildPlugin` —— 生成 `.zip`
2. `signPlugin` —— 签名
3. `publishPlugin` —— 上传到应用商店

首次提交在 2–5 个工作日内审查。后续更新审查更快。

## CI 自动化（GitHub Actions）

创建 `.github/workflows/publish-jetbrains.yml`：

```yaml
name: Publish JetBrains Plugin

on:
  push:
    tags: ['jetbrains-v*']

jobs:
  publish:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: 17

      - name: Build and publish
        working-directory: packages/jetbrains-plugin
        env:
          JETBRAINS_MARKETPLACE_TOKEN: ${{ secrets.JETBRAINS_MARKETPLACE_TOKEN }}
          JB_CERT_CHAIN: ${{ secrets.JB_CERT_CHAIN }}
          JB_PRIVATE_KEY: ${{ secrets.JB_PRIVATE_KEY }}
          JB_KEY_PASSWORD: ${{ secrets.JB_KEY_PASSWORD }}
        run: ./gradlew signPlugin publishPlugin
```

在 GitHub → 设置 → 机密中添加全部四个值作为仓库机密。

## 发布清单

- [ ] 提升 `packages/jetbrains-plugin/build.gradle.kts` 中的 `version`
- [ ] 用新版本块更新 `build.gradle.kts` 中的 `changeNotes`
- [ ] 运行 `./gradlew verifyPlugin` 并修复任何错误
- [ ] 本地安装构建好的工件（`buildPlugin` → 从磁盘安装）并冒烟测试
- [ ] 为发布打标签：`git tag jetbrains-vX.Y.Z`
- [ ] 推送标签到远程——CI 工作流会自动拾取
