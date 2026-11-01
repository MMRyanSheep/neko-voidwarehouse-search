package neko.voidwarehouse.search;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 搜索索引与匹配的纯函数单测（不依赖 Minecraft —— {@link SearchIndex} 刻意不 import 游戏类型）。
 */
class SearchIndexTest {

    /** 测试用的显示名解析器：内置一张小目录 */
    private static final Map<String, String> NAMES = Map.of(
            "minecraft:iron_ingot", "铁锭",
            "minecraft:iron_block", "铁块",
            "minecraft:diamond", "钻石",
            "minecraft:crafting_table", "工作台",
            "minecraft:oak_log", "橡木原木");

    private static List<SearchIndex.Entry> build() {
        return SearchIndex.build(new ArrayList<>(NAMES.keySet()), NAMES::get);
    }

    private static List<String> match(String query) {
        return SearchIndex.match(build(), query);
    }

    @Test
    void emptyQueryMatchesNothingAndMeansClearFilter() {
        // 空查询 = 空列表 = 协议里的「清除过滤」，这条等价关系是协议的一半，咬死
        assertTrue(SearchIndex.match(build(), "").isEmpty());
        assertTrue(SearchIndex.match(build(), "   ").isEmpty());
        assertTrue(SearchIndex.match(build(), null).isEmpty());
    }

    @Test
    void matchesChineseName() {
        assertEquals(List.of("minecraft:iron_ingot"), match("铁锭"));
    }

    @Test
    void matchesPinyinFullWithoutSeparator() {
        // 全拼连写（tieDing）与带空格（tie ding）都必须命中同一个
        assertTrue(match("tieding").contains("minecraft:iron_ingot"));
        assertTrue(match("TieDing").contains("minecraft:iron_ingot"));
    }

    @Test
    void matchesPinyinInitials() {
        assertTrue(match("td").contains("minecraft:iron_ingot"));
        assertTrue(match("zs").contains("minecraft:diamond"));
        assertTrue(match("gzt").contains("minecraft:crafting_table"));
    }

    @Test
    void matchesRegistryPathInEnglish() {
        assertTrue(match("iron").containsAll(List.of("minecraft:iron_ingot", "minecraft:iron_block")));
        assertTrue(match("oak_log").contains("minecraft:oak_log"));
    }

    @Test
    void pinyinDoesNotMatchWrongInitialsOrder() {
        // 只认顺序正确的首字母：ds 不该命中铁锭
        assertTrue(!match("dt").contains("minecraft:iron_ingot"));
    }

    @Test
    void distinctCharactersGiveDistinctInitials() {
        // 铁锭 = tie ding = td；铁块 = tie kuai = tk —— 拼音串咬死（词典事实钉子）
        assertEquals("td", SearchIndex.of("minecraft:iron_ingot", "铁锭").pinyinInitials());
        assertEquals("tk", SearchIndex.of("minecraft:iron_block", "铁块").pinyinInitials());
        assertEquals("tieding", SearchIndex.of("minecraft:iron_ingot", "铁锭").pinyinFull());
    }

    @Test
    void unknownIdsAreSkippedSilently() {
        // Arrays.asList 允许 null 元素（List.of 会当场抛 NPE，拒绝null 是它自己的契约）
        List<String> ids = Arrays.asList("minecraft:iron_ingot", "somemod:ghost_item", null);
        List<SearchIndex.Entry> index = SearchIndex.build(ids, id -> NAMES.get(id));
        assertEquals(1, index.size());
        assertEquals("minecraft:iron_ingot", index.get(0).id());
    }

    @Test
    void blankDisplayNameIsSkipped() {
        Map<String, String> names = new HashMap<>(NAMES);
        names.put("weird:blank", "");
        List<SearchIndex.Entry> index = SearchIndex.build(List.of("weird:blank"), names::get);
        assertTrue(index.isEmpty());
    }

    @Test
    void queryIsTrimmedAndCaseInsensitive() {
        assertEquals(match("IRON"), match("iron"));
        assertEquals(match(" 铁锭 "), match("铁锭"));
    }

    @Test
    void tokenPathsStripNamespace() {
        SearchIndex.Entry e = SearchIndex.of("minecraft:iron_ingot", "铁锭");
        assertEquals("iron_ingot", e.idPath());
        assertEquals("tieding", e.pinyinFull());
        assertEquals("td", e.pinyinInitials());
        assertEquals("铁锭", e.nameLower());
    }
}
