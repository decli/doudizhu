# 长青斗地主

适配老人使用的 Android 15 平板斗地主应用，面向 14 寸横屏大屏设计。项目使用 Kotlin + Jetpack Compose，从零实现三人本地单机斗地主、AI 出牌、实时进度保存和 GitHub Actions 自动发布 APK。

## 设计目标

- 大屏适老：全屏铺满、按钮大、字号大、信息高对比，适合老人看牌和点按。
- 算法扎实：完整支持常见斗地主牌型，AI 用手牌评估与组合搜索做叫分和出牌决策。
- 体验完整：机器人出牌时使用系统 TTS 中文播报牌型，支持实时存档恢复。
- 发布直达：推送标签后自动构建、签名并发布 APK 到 GitHub Release。

## 已实现功能

- 三人斗地主标准流程：洗牌、发牌、叫分、地主/农民、轮流出牌、炸弹/王炸加倍、胜负结算。
- 牌型识别与比牌：
  - 单张、对子、三张
  - 三带一、三带一对
  - 顺子、连对
  - 飞机、飞机带单、飞机带对
  - 四带二、四带两对
  - 炸弹、王炸
- AI 能力：
  - 根据手牌结构、控制牌和炸弹数评估叫分
  - 根据剩余手牌代价选择主动出牌
  - 在跟牌时尽量保留炸弹，并识别农民同伴已占优时的让牌策略
- 适老界面：
  - 深绿牌桌 + 金色高亮
  - 超大卡牌、超大操作按钮
  - 桌面播报区、当前轮次状态、明示地主身份
- 实时保存：
  - 每次状态变化写入 DataStore
  - 重新打开应用自动恢复对局
- 图标：
  - 自定义自适应图标，使用金色皇冠 + 扑克牌主题

## 运行环境

- Android Studio / Gradle Wrapper
- JDK 17+
- Android SDK 35
- 推荐设备：Android 15，14 寸平板，横屏

## 本地构建

```powershell
./gradlew.bat assembleDebug
./gradlew.bat test
```

调试 APK 输出位置：

`app/build/outputs/apk/debug/app-debug.apk`

## 项目结构

- `app/src/main/java/com/decli/doudizhu/engine`
  - 斗地主规则识别、AI 决策、对局推进
- `app/src/main/java/com/decli/doudizhu/viewmodel`
  - UI 状态管理、AI 异步轮转、实时存档
- `app/src/main/java/com/decli/doudizhu/ui`
  - 适老化平板 UI 与大屏牌桌布局
- `app/src/main/java/com/decli/doudizhu/audio`
  - 机器人中文出牌播报

## 自动发布

仓库内置 GitHub Actions 工作流：

- 推送标签 `v*` 时自动：
  - 构建 release APK
  - 生成临时签名证书
  - 对 APK 进行 zipalign 和 apksigner 签名
  - 上传到 GitHub Release

建议发布命令：

```powershell
git tag v1.0.0
git push origin main --tags
```

## 适老化说明

- 所有主要操作按钮均为大尺寸圆角按钮
- 牌面使用高对比深浅配色，红黑花色区分明显
- 中央区域始终显示当前轮次、上一手牌和桌面播报
- 存档恢复状态在顶部醒目提示，减少误操作焦虑

## 后续可扩展

- 更多音效包和真人语音包
- 托管模式、难度档位
- 更细的战绩系统与老人偏好设置
- 平板双栏设置页和无障碍增强模式

