package neko.voidwarehouse.search;

import com.github.stuxuhai.jpinyin.PinyinFormat;
import com.github.stuxuhai.jpinyin.PinyinHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * 搜索索引与匹配：把物品 id + 本地化显示名变成可检索的 token，再按查询串筛出命中的 id。
 *
 * <h2>为什么这个类是「纯 Java」的</h2>
 * <p>它<b>不 import 任何 Minecraft 类型</b> —— 物品 id 怎么解析、显示名怎么翻译，
 * 全部通过 {@link Function} 注入。这样：
 * ① 匹配逻辑可以脱离游戏跑 JUnit 单测（拼音、首字母、注册名三条路逐条咬死）；
 * ② 万一 26.x 的物品 API 变了，只改调用方一处，索引与匹配逻辑原样复用。</p>
 *
 * <h2>token 有四条</h2>
 * <ul>
 *   <li>{@code nameLower} —— 中文名原文（小写）：支持直接输中文，如 {@code 铁锭}；</li>
 *   <li>{@code pinyinFull} —— 全拼：{@code 铁锭 → tieding}（jpinyin WITHOUT_TONE，
 *       非 CJK 字符原样保留，中英混排名天然正确）；</li>
 *   <li>{@code pinyinInitials} —— 首字母：{@code 铁锭 → td}（{@code getShortPinyin}）；</li>
 *   <li>{@code idPath} —— 注册名 path：{@code minecraft:iron_ingot → iron_ingot}，输英文也命中。</li>
 * </ul>
 *
 * <p>匹配一律 {@code contains}（包含即命中），查询串统一小写、去首尾空白。
 * 多音字取 jpinyin 词库的默认读音（物品名场景足够），不做句子级消歧 ——
 * 这是评估时明确接受的误差范围。</p>
 *
 * <p>选型记录：首选 TinyPinyin 在 Maven Central 没有可用坐标（实测 404），
 * jpinyin 同为 Apache-2.0 且在 Central 有正式坐标，还自带首字母接口，故用之。</p>
 */
public final class SearchIndex {

    /** 一条可检索的目录条目：id 是回传服务端的唯一凭证，其余全是匹配用 token */
    public record Entry(String id, String nameLower, String pinyinFull, String pinyinInitials, String idPath) {
    }

    private SearchIndex() {
    }

    /** 全拼的输出格式：无声调、无分隔 */
    private static final PinyinFormat PINYIN_WITHOUT_TONE = PinyinFormat.WITHOUT_TONE;

    /**
     * 为一个目录条目建 token。{@code id} 须为注册 id 全名（{@code namespace:path}）。
     *
     * <p>jpinyin 的两个 API 都声明受检异常 {@code PinyinException}（词库文件读不出时抛，
     * 实际运行不会发生 —— 词库在 jar 里）。真发生了就退化为「空拼音 token」：
     * 中文原文与注册名两条匹配路仍然可用，绝不让一个词条把整份目录建挂。</p>
     */
    public static Entry of(String id, String displayName) {
        String full;
        String initials;
        try {
            // convertToPinyinString 对非汉字原样保留；getShortPinyin 只给汉字的首字母，
            // 非汉字字符也原样保留 —— 中英混排名两条串都是对的
            full = PinyinHelper.convertToPinyinString(displayName, "", PINYIN_WITHOUT_TONE).toLowerCase();
            initials = PinyinHelper.getShortPinyin(displayName).toLowerCase();
        } catch (com.github.stuxuhai.jpinyin.PinyinException pinyinUnavailable) {
            full = "";
            initials = "";
        }
        String path = id;
        int colon = id.indexOf(':');
        if (colon >= 0) {
            path = id.substring(colon + 1);
        }
        return new Entry(id, displayName.toLowerCase(), full, initials, path.toLowerCase());
    }

    /**
     * 建整份目录的索引：id 逐个问 {@code displayNameResolver} 要本地化显示名；
     * 解析不出显示名（客户端没有这个物品）的条目<b>静默跳过</b> ——
     * 它匹配不出来，自然也不会出现在搜索结果里，不需要报错。
     */
    public static List<Entry> build(List<String> itemIds, Function<String, String> displayNameResolver) {
        List<Entry> index = new ArrayList<>(itemIds.size());
        for (String id : itemIds) {
            if (id == null) {
                continue;
            }
            String name = displayNameResolver.apply(id);
            if (name == null || name.isBlank()) {
                continue;
            }
            index.add(of(id, name));
        }
        return List.copyOf(index);
    }

    /**
     * 按查询串筛出命中的 id 集合（保持目录顺序）。
     *
     * <p><b>空查询返回空列表</b> —— 空列表在协议里就是「清除过滤」，
     * 所以「清空输入框」与「服务器恢复完整列表」是同一条路，没有特判。</p>
     */
    public static List<String> match(List<Entry> index, String query) {
        String q = query == null ? "" : query.trim().toLowerCase();
        if (q.isEmpty()) {
            return List.of();
        }
        List<String> hits = new ArrayList<>();
        for (Entry e : index) {
            if (e.nameLower().contains(q) || e.pinyinFull().contains(q)
                    || e.pinyinInitials().contains(q) || e.idPath().contains(q)) {
                hits.add(e.id());
            }
        }
        return hits;
    }
}
