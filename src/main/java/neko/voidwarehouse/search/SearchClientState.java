package neko.voidwarehouse.search;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.components.EditBox;

import java.util.List;

/**
 * 搜索框的客户端状态机：一个屏幕一份（静态单例 —— 同一时刻只有一个屏幕在前台）。
 *
 * <p>（public：mixin 在 {@code .search.mixin} 子包里，跨包访问需要。）</p>
 *
 * <p>生命周期由 mixin（{@code MixinAbstractContainerScreen}）驱动：
 * {@code init} 时 {@link #bind(EditBox)}，每 tick 调 {@link #tick()}，
 * {@code removed} 时 {@link #clear()}。</p>
 *
 * <h2>数据流</h2>
 * <pre>
 *   init ──hello──▶ 服务端（判断是虚空仓库才回） ──catalog──▶ 建索引 + 显示搜索框
 *   输入 ──▶ 本地匹配 ──pendingQuery──▶ tick 里带尾随防抖 ──apply──▶ 服务端过滤出页
 *   清空输入 ──apply(空)──▶ 服务端恢复完整列表
 * </pre>
 *
 * <h2>两个防抖各管一头</h2>
 * <ul>
 *   <li><b>hello（打开界面 / 请求刷新目录）500ms 一次</b>：界面打开后服务端变更是
 *       可接受的滞后（评估已定），用户点一下搜索框就能强制刷新；</li>
 *   <li><b>apply 尾随防抖 150ms</b>：打字快的时候前面的查询直接被覆盖，
 *       但<b>最后一个查询一定会在 150ms 内发出去</b> —— 这是「首键即滤、尾键不丢」的
 *       关键，比纯节流多出来的那个尾随保证。</li>
 * </ul>
 *
 * <h2>优雅降级</h2>
 * <p>{@code canSend} 为 false（旧服务端没这条通道）⇒ hello 发不出去 ⇒
 * 目录永远不来 ⇒ 搜索框永远 {@code setVisible(false)}，功能整个不存在，
 * 界面上没有任何残迹。</p>
 */
public final class SearchClientState {

    /** apply 尾随防抖间隔（毫秒） */
    private static final long APPLY_DEBOUNCE_MS = 150L;
    /** hello 最短间隔（毫秒） */
    private static final long HELLO_INTERVAL_MS = 500L;

    private static EditBox box;
    private static List<SearchIndex.Entry> index = List.of();
    /** 待发送的查询（{@code null} = 没有待发的） */
    private static String pendingQuery;
    private static long lastApplySentAt;
    private static long lastHelloSentAt;

    private SearchClientState() {
    }

    /** 屏幕初始化（大箱子类）时绑定新的搜索框，并要一份目录 */
    public static void bind(EditBox searchBox) {
        box = searchBox;
        index = List.of();
        pendingQuery = null;
        sendHello();
    }

    /** 屏幕关闭：状态全部作废。服务端那边的过滤跟着菜单一起消失，无需任何清理包 */
    public static void clear() {
        box = null;
        index = List.of();
        pendingQuery = null;
    }

    /** 服务端回目录了：建索引、亮出搜索框，把已输入的查询立刻补一次匹配 */
    public static void onCatalog(List<String> itemIds) {
        if (box == null) {
            return;
        }
        index = SearchIndex.build(itemIds, SearchCatalogResolver::displayNameOf);
        box.setVisible(true);
        if (!box.getValue().isBlank()) {
            requestApply(box.getValue());
        }
    }

    /** 搜索框内容变化（responder）：只记下待发查询，真正发送在 tick 里带尾随防抖 */
    public static void onQueryChanged(String query) {
        requestApply(query);
    }

    /** 每 tick 推进一次尾随防抖（mixin 的 containerTick TAIL 调用） */
    public static void tick() {
        if (pendingQuery == null) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now - lastApplySentAt < APPLY_DEBOUNCE_MS) {
            return;
        }
        String query = pendingQuery;
        pendingQuery = null;
        sendApply(SearchIndex.match(index, query));
        lastApplySentAt = now;
    }

    /** 点了搜索框 = 用户在等结果，顺手刷新一次目录（服务端限流 100ms，这里 500ms 够宽） */
    public static void onSearchBoxClicked() {
        sendHello();
    }

    private static void requestApply(String query) {
        pendingQuery = query;
    }

    private static void sendHello() {
        long now = System.currentTimeMillis();
        if (now - lastHelloSentAt < HELLO_INTERVAL_MS) {
            return;
        }
        lastHelloSentAt = now;
        // 旧服务端没这条通道：canSend 为 false，发不出去，搜索框永远不亮 —— 这就是降级
        if (ClientPlayNetworking.canSend(SearchHelloPayload.TYPE)) {
            ClientPlayNetworking.send(SearchHelloPayload.INSTANCE);
        }
    }

    private static void sendApply(List<String> matchedIds) {
        if (ClientPlayNetworking.canSend(SearchApplyPayload.TYPE)) {
            ClientPlayNetworking.send(new SearchApplyPayload(matchedIds));
        }
    }
}
