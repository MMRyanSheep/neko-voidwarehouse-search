package neko.voidwarehouse.search;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 客户端入口：注册搜索通道的三条 payload + 目录接收端。
 *
 * <h2>C2S / S2C 的双端注册红线</h2>
 * <p>这三条通道在服务端（carpet-Neko-Addition）已经注册过一份。
 * 客户端这边<b>仍要自己注册，且必须用「撞名吞掉」的幂等写法</b>：</p>
 * <ul>
 *   <li><b>专用服务器上两端是两个进程</b>：客户端进程的两张注册表是空的，
 *       不自己注册的话 C2S 会被编码成原版的 {@code DiscardedPayload}
       —— 不崩，但数据整包丢，{@code canSend} 还显示「支持」，最难查；</li>
 *   <li><b>单人存档里两端同进程</b>：服务端那份先注册（Fabric 按通道名严格去重），
 *       这里的注册抛 {@link IllegalArgumentException} —— 吞掉即可，
 *       注册顺序有保证（服务端在主入口初始化，客户端入口必然更晚）；</li>
 *   <li>S2C（catalog）同理：接收端要求 payload 类型已在 clientbound 表里，
 *       单人时靠服务端那份，专用服务器上靠这里这份。</li>
 * </ul>
 */
public final class VoidWarehouseSearchClient implements ClientModInitializer {

    public static final Logger LOGGER = LoggerFactory.getLogger("voidwarehouse-search");

    @Override
    public void onInitializeClient() {
        // C2S ×2：客户端要「发」的 payload，两端都得在 serverbound 表里
        try {
            PayloadTypeRegistry.serverboundPlay().register(
                    SearchHelloPayload.TYPE, SearchHelloPayload.CODEC);
        } catch (IllegalArgumentException alreadyRegistered) {
            // 单人：服务端同进程已注册，吞掉
        }
        try {
            PayloadTypeRegistry.serverboundPlay().register(
                    SearchApplyPayload.TYPE, SearchApplyPayload.CODEC);
        } catch (IllegalArgumentException alreadyRegistered) {
            // 单人：同上
        }
        // S2C ×1：客户端要「收」的 payload，两端都得在 clientbound 表里
        try {
            PayloadTypeRegistry.clientboundPlay().register(
                    SearchCatalogPayload.TYPE, SearchCatalogPayload.CODEC);
        } catch (IllegalArgumentException alreadyRegistered) {
            // 单人：同上
        }

        ClientPlayNetworking.registerGlobalReceiver(
                SearchCatalogPayload.TYPE,
                (payload, context) -> SearchClientState.onCatalog(payload.itemIds()));

        // 断线就清状态（服务端的过滤跟着菜单消失，这里只是别留着旧引用）
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> SearchClientState.clear());

        LOGGER.info("[虚空仓库搜索] 客户端通道已就绪（对端没装服务端模组时搜索框自动隐藏）");
    }
}
