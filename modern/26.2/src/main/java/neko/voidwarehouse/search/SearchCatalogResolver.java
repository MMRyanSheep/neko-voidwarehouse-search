package neko.voidwarehouse.search;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * 客户端侧的注册表查询：物品 id → 本地化显示名（目录索引入口）。
 *
 * <p><b>只有客户端能做这一步</b>：专用服务器没有 {@code zh_cn.json}，
 * 服务端把「铁锭」翻译成中文这件事在原理上就不成立 —— 这正是
 * 「目录下发 + 客户端匹配」方案的出发点（见 {@link SearchCatalogPayload}）。</p>
 *
 * <p>仓库里的物品全部不含额外数据组件（服务端 {@code insert} 拒收），
 * 所以显示名只取决于物品本身，一个默认 {@code ItemStack} 就能解析，
 * 不需要任何组件上下文。解析结果按条目缓存在 {@link SearchIndex} 里，
 * 同一次目录只查一遍。</p>
 */
public final class SearchCatalogResolver {

    private SearchCatalogResolver() {
    }

    /**
     * 物品 id → 本地化显示名；客户端不认识这个 id（例如服务端装了客户端没有的模组）
     * 时返回 {@code null}，调用方（{@link SearchIndex#build}）会静默跳过该条目。
     */
    public static String displayNameOf(String itemId) {
        Identifier key = Identifier.tryParse(itemId);
        if (key == null || !BuiltInRegistries.ITEM.containsKey(key)) {
            return null;
        }
        Item item = BuiltInRegistries.ITEM.getValue(key);
        return new ItemStack(item).getHoverName().getString();
    }
}
