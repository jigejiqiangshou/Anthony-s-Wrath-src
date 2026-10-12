# src —— 核心资源层

本目录是本项目的第三层,存放**全部代码、数据与资源**。

## 归属

- 代码、资源、数据只写在本层。
- 构建配置**不**在本层:顶层仓库的 `pom.xml` 才是 Maven 工程根,本层只作为源码树被它引用。
- 顶层只放项目配置与三层挂载点,不放任何具体资源或产物。

## Maven 布局约定

顶层 `pom.xml` 必须显式声明源与资源目录指向本子模块:

- `sourceDirectory` → `src/main/java`
- `resources` → `src/main/resources`

构建命令一律在**顶层目录**执行,产物落在被 `.gitignore` 忽略的顶层 `target/`。

## 初始状态

本仓库在建立三层结构时以空仓库创建,尚未有任何源码。首个功能(`specs/001-anthonys-wrath`)的 T001 负责建立 Maven 骨架。
