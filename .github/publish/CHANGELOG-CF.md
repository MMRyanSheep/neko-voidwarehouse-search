## ✨ VoidWarehouse Search 0.1.0 — first release

A **client-side only** Fabric mod: a search bar on top of the Neko void warehouse UI. Filter by **Chinese name / full pinyin / pinyin initials / registry id** — `铁锭`, `tieding`, `td` and `iron_ingot` all find the same stack. Clear the box to restore the full list.

### Highlights
- **Server GUI byte-for-byte untouched** — layout, navigation row, click semantics stay vanilla; only "what fills page n" changes
- Matching runs client-side (only clients have language files); the server just intersects by item identity over three lightweight, rate-limited channels
- Session-level filter: closing the GUI clears it, nothing written to the world save
- **Graceful degradation**: on servers without the companion Carpet mod the search bar simply never appears — vanilla clients notice nothing

### Supported Minecraft versions
26.3 / 26.2 / 26.1.2 / 26.1.1 / 1.21.11 — pick the jar matching your client.

### Server requirement
[Neko Carpet Addition](https://modrinth.com/mod/neko-carpet-addition) **1.0.16+** (matching version) with `voidWarehouse` enabled.

### 🔴 Key handling
While the search box is focused **every key press is captured** (ESC = leave the box; press again to close the GUI). Otherwise digits `1-9` would trigger vanilla hotbar-swap and store your whole hotbar into the warehouse.

## 中文
- 首版发布：虚空仓库界面搜索框，**中文 / 拼音全拼 / 拼音首字母 / 注册名**四种匹配
- 服务端 GUI 零改动；过滤是会话级状态，不写存档
- 服务端需搭配 carpet-Neko-Addition 1.0.16+；没装时搜索框自动隐藏
- 🔴 聚焦搜索框时所有按键被吞（ESC = 退焦，再按一次关界面）——防止数字键触发「快捷栏整组入库」
