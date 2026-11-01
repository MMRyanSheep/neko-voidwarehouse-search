package neko.voidwarehouse.search.mixin;

import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * 暴露 {@link Screen#addRenderableWidget}（protected）。
 *
 * <h2>为什么不直接在容器屏 mixin 里 @Shadow</h2>
 * <p>🔴 这个方法<b>声明在父类 {@code Screen}</b>，而 @Shadow 只在 mixin 的
 * <b>目标类自己声明的成员</b>里找（2026-11-01 真机实证：shadow 写在
 * {@code AbstractContainerScreen} 的 mixin 里 = apply 阶段直接崩服崩端，
 * 日志原文 {@code @Shadow method ... was not located in the target class}）。
 * 父类成员的正规入口是 {@code @Invoker} accessor —— 目标类改成 {@code Screen}，
 * 方法就在那里声明，查找必然命中。</p>
 *
 * <p>签名用擦除后的形态（{@code GuiEventListener → GuiEventListener}），
 * 与字节码描述符逐字对齐，不需要 refMap。</p>
 */
@Mixin(Screen.class)
public interface AccessorScreen {

    @Invoker("addRenderableWidget")
    GuiEventListener voidwarehouseSearch$invokeAddRenderableWidget(GuiEventListener widget);
}
