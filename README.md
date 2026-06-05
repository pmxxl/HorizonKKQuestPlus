# HorizonKKQuestPlus

**高度定制化的 Minecraft 修仙悬赏任务系统**

一个基于 Spigot/Bukkit 的随机任务悬赏插件，融合修仙世界观，包含动态悬赏池、商店供需联动、多阶灵石货币体系等特色功能。项目同时附带一个配套的商店系统插件 **HorizonKKShopPlus**。

---

## ✨ 特性一览

### 悬赏系统 (HorizonKKQuestPlus)

- **四阶悬赏等级** — 人级、玄级、地级、天级，各有独立的物品外观与奖励梯度
- **四种任务类型** — 收集、猎杀、探索、护送
- **公共悬赏池** — 服务器定时自动发布悬赏，玩家从共享池中浏览接取
- **随机生成收集任务** — 模板定义物品池与数量范围，系统自动平衡总价值并附加随机奖励倍率 (0.7x–1.3x)
- **商店缺货联动** — 自动检测商店库存不足的商品，生成对应的收集悬赏，玩家完成后商品自动补货，形成有机供需循环
- **多阶灵石货币** — 下品/中品/上品/极品四阶灵石，1:32 进制自动面额优化
- **保证金机制** — 接取任务时缴纳奖励 8% 的保证金，完成退还，失败扣除
- **游戏内历法计时** — 使用 TYLcalendarPlus 游戏内日历，任务期限以游戏世界日/月/年计算
- **GUI 界面** — 基于物品栏的悬赏浏览、发布、提交、确认对话框
- **数据持久化** — 所有悬赏状态与玩家数据通过 YAML 文件存储，重启不丢失

### 商店系统 (HorizonKKShopPlus)

- **多种商店类型** — 购买店、出售店、混合店、限量店
- **合成台** — 支持 NeigeItems 自定义物品、原版材料组、灵石费用的多槽合成配方
- **库存管理** — 限量店逐槽追踪库存，自动定时保存
- **公共 API** — 通过 Bukkit ServiceManager 暴露，其他插件可查询/添加库存

---

## 📦 环境要求

| 项目 | 版本 |
|------|------|
| Minecraft 服务器 | Spigot 1.21.4+ |
| Java | 21+ |
| API Version | Bukkit 1.21 |

### 依赖插件

| 插件 | 用途 | 必需 |
|------|------|------|
| [NeigeItems](https://www.mcmod.cn/class/11503.html) | 自定义物品框架 | ✅ |
| TYLcalendarPlus | 游戏内历法系统 | ✅ |
| Chuwudai | 灵石货币袋 | ✅ (商店系统需要) |

---

## 🔨 构建

### 前置准备

将以下 JAR 文件放入 `libs/` 目录：

```
libs/
  NeigeItems-1.21.129-api.jar
  TYLcalendarPlus-1.0-SNAPSHOT.jar
```

商店插件还需：

```
HorizonKKShopPlus/libs/
  NeigeItems-1.21.129-api.jar
  chuwudai-1.0-SNAPSHOT.jar
```

### 编译

```bash
# 1. 先构建商店插件
cd HorizonKKShopPlus
mvn clean package

# 2. 将商店 JAR 复制到主项目的 libs/ 目录
cp target/HorizonKKShopPlus-1.0-SNAPSHOT.jar ../libs/

# 3. 构建主插件
cd ..
mvn clean package
```

构建产物位于 `target/HorizonKKQuestPlus-1.0-SNAPSHOT.jar`。

### 部署

将以下 JAR 文件放入服务器的 `plugins/` 目录：

```
plugins/
  HorizonKKQuestPlus-1.0-SNAPSHOT.jar   # 悬赏系统
  HorizonKKShopPlus-1.0-SNAPSHOT.jar    # 商店系统
  NeigeItems-xxx.jar                     # 自定义物品
  TYLcalendarPlus-xxx.jar               # 游戏内历法
  Chuwudai-xxx.jar                       # 货币袋 (商店需要)
```

---

## 🎮 命令

### 悬赏系统

| 命令 | 说明 | 权限 |
|------|------|------|
| `/horizonquest reload` | 重载配置 | `horizonkkquestplus.admin` |
| `/horizonquest open [玩家]` | 打开悬赏菜单 | `horizonkkquestplus.admin` |
| `/horizonquest publish <模板ID> [anonymous\|named]` | 发布悬赏 | `horizonkkquestplus.admin` |
| `/horizonquest list <public\|accepted\|published>` | 查看悬赏列表 | `horizonkkquestplus.use` |
| `/horizonquest test random\|deficit\|template` | 测试功能 | `horizonkkquestplus.admin` |

别名：`/quest`、`/hkq`

### 商店系统

| 命令 | 说明 | 权限 |
|------|------|------|
| `/horizonshop reload` | 重载配置 | `horizonkkshopplus.admin` |
| `/horizonshop open <商店名> [玩家]` | 打开商店 | `horizonkkshopplus.admin` |

别名：`/shop`、`/hks`

---

## ⚙️ 配置说明

### config.yml — 主配置

```yaml
# 灵石货币配置 (NeigeItems 物品ID映射)
currency:
  lower_stone: "item_id_1"    # 下品灵石
  mid_stone: "item_id_2"      # 中品灵石
  high_stone: "item_id_3"     # 上品灵石
  top_stone: "item_id_4"      # 极品灵石

# 面额转换阈值 (超过此数量自动升级)
thresholds:
  lower_to_mid: 32
  mid_to_high: 32
  high_to_top: 32

# 奖励随机倍率范围
reward_multiplier:
  min: 0.7
  max: 1.3

# 保证金比例
deposit_rate: 0.08

# 公共悬赏池
public_quest:
  interval: 3600    # 发布间隔 (秒)
  batch_size: 3     # 每次发布数量

# 可收集物品列表 (物品ID -> 价值)
collectible_items:
  item_id: 10

# 悬赏模板池 (按等级分组)
templates:
  human: [...]
  mystery: [...]
  earth: [...]
  sky: [...]
```

### Quests/Example.yml — 悬赏定义

```yaml
quests:
  hunt_zombie:
    type: HUNT
    level: HUMAN
    name: "讨伐僵尸"
    description: "消灭僵尸以保一方平安"
    requirements:
      mob: ZOMBIE
      amount: 10
    reward:
      lower_stone: 5
    time_limit: "5y"   # 游戏内5年
```

---

## 📁 项目结构

```
HorizonKKQuestPlus/
├── pom.xml                          # Maven 构建配置
├── libs/                            # 本地依赖 JAR
├── src/main/
│   ├── java/com/horizonkk/horizonKKQuestPlus/
│   │   ├── HorizonKKQuestPlus.java  # 插件入口
│   │   ├── command/                 # 命令处理
│   │   ├── event/                   # 事件监听器
│   │   ├── gui/                     # GUI 界面
│   │   ├── hook/                    # 外部插件集成
│   │   ├── manager/                 # 核心业务逻辑
│   │   └── model/                   # 数据模型
│   └── resources/
│       ├── plugin.yml               # 插件描述
│       ├── config.yml               # 主配置
│       └── Quests/Example.yml       # 示例悬赏定义
│
└── HorizonKKShopPlus/               # 配套商店插件
    ├── pom.xml
    └── src/main/
        ├── java/.../HorizonKKShopPlus/
        │   ├── api/                 # 公共 API
        │   ├── gui/                 # 商店/合成 GUI
        │   ├── hook/                # NeigeItems/Chuwudai 集成
        │   ├── listener/            # 事件监听器
        │   └── manager/             # 业务逻辑
        └── resources/
            ├── plugin.yml
            ├── Shop/Example.yml     # 示例商店定义
            └── Craft/Example.yml    # 示例合成配方
```

---

## 🔧 技术栈

- **语言**：Java 21
- **构建工具**：Apache Maven
- **服务器端**：Spigot/Bukkit API 1.21
- **配置格式**：YAML (Bukkit YamlConfiguration)
- **数据存储**：YAML 文件持久化
- **插件打包**：maven-shade-plugin (fat JAR)

---

## 📄 License

本项目由 [HorizonKK](https://github.com/HorizonKK) 开发。

---

> ⚠️ 本插件为修仙主题服务器设计，依赖 NeigeItems 自定义物品体系与 TYLcalendarPlus 游戏内历法，请确保这些插件已正确安装后再使用。
