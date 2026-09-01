# 健身自律记录工具

一款面向健身小白的**周计划打卡 + 身体数据记录**工具。核心目标：**减脂为主、顺带紧致增肌**。原生 Web 应用，已用 Capacitor 封装为安卓 APK。

## 功能特性

- **4 天循环训练计划**：下肢推/胸推、背拉、臀腿髋、全身燃脂，含器械与动作要点。
- **顺序解锁**：必须完成当天训练才能解锁下一天，保证训练节奏。
- **摸鱼机制**：可跳过单个动作或整天（记为 0），不会被卡死。
- **多套计划管理**：查看 / 导入（预览+命名）/ 切换 / 删除训练计划。
- **打卡趋势**：按训练日查看各动作数值（重量/次数/时长）变化折线图，支持图例高亮。
- **GitHub 风格活跃日历**：按周查看每天完成度，摸鱼不计入，点击格可跳转对应周。
- **一键导出**：训练计划 / 记录表支持 **JSON / HTML / PDF** 三种格式，非 JSON 会先预览。
- **身体数据**：每周记录体重 / 体脂率 / 腰围 / 臀围 / 腿围，绘制趋势图。
- **数据本地存储**：所有记录保存在设备本地（localStorage），无需登录。

## 项目结构

```
健身打卡App/            # Web 应用源码（index.html 单文件）
健身打卡App-desktop/    # Capacitor 安卓工程（cap sync 后由 web 生成）
减脂增肌健身计划.html    # 训练计划文档
组数记录表/             # 组数记录模板
```

## 本地运行

直接用浏览器打开 `健身打卡App/index.html` 即可体验网页版。

构建安卓 APK：

```bash
cd 健身打卡App-desktop
npx cap sync android
./android/gradlew.bat assembleDebug -p ./android
# 产物：android/app/build/outputs/apk/debug/app-debug.apk
```

## 在线更新机制

应用启动时会查询本仓库的 **GitHub Releases** 最新版本并对比：

1. 发布新版本时，给仓库打上 tag（如 `v1.1.0`），并上传对应 `*.apk` 作为 Release 附件。
2. 用户打开 App 后会自动检测：若远端 tag 版本高于本地 `APP_VERSION`，弹出「发现新版本」提示。
3. 用户点击「下载并安装」即可下载并覆盖安装新 APK，本地训练与身体数据会保留。

在 `健身打卡App/index.html` 顶部维护：

- `APP_VERSION`：当前版本号，发新版本时递增。
- `UPDATE_REPO`：`owner/repo`，指向本仓库。

> 仓库名：`fitness-discipline-tracker`（GitHub 不允许仓库名含中文）。