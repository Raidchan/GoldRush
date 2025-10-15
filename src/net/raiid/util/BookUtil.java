package net.raiid.util;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public final class BookUtil {

    private BookUtil() {}

    public static BookBuilder create() {
        return new BookBuilder();
    }

    public static class BookBuilder {
        private String title = "";
        private String author = "Server";
        private final List<BaseComponent[]> pages = new ArrayList<>();

        public BookBuilder title(String title) {
            this.title = TextUtil.color(title);
            return this;
        }

        public BookBuilder author(String author) {
            this.author = author;
            return this;
        }

        public BookBuilder addPage(Consumer<PageBuilder> pageContent) {
            PageBuilder pageBuilder = new PageBuilder();
            pageContent.accept(pageBuilder);
            this.pages.add(pageBuilder.build());
            return this;
        }

        public ItemStack build() {
            ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
            BookMeta meta = (BookMeta) book.getItemMeta();
            if (meta == null) return book;

            meta.setTitle(title);
            meta.setAuthor(author);
            if (!pages.isEmpty()) {
                meta.spigot().setPages(pages.toArray(new BaseComponent[0][]));
            }

            book.setItemMeta(meta);
            return book;
        }
    }

    public static class PageBuilder {
        private final ComponentBuilder pageBuilder = new ComponentBuilder();

        public PageBuilder add(String text, Consumer<TextBuilder> textContent) {
            TextBuilder textBuilder = new TextBuilder(text);
            textContent.accept(textBuilder);
            pageBuilder.append(textBuilder.build(), ComponentBuilder.FormatRetention.NONE);
            return this;
        }

        public PageBuilder add(String text) {
            pageBuilder.append(TextUtil.color(text), ComponentBuilder.FormatRetention.NONE);
            return this;
        }

        public PageBuilder newLine() {
            pageBuilder.append("\n");
            return this;
        }

        private BaseComponent[] build() {
            return pageBuilder.create();
        }
    }

    public static class TextBuilder {
        private final TextComponent component;

        private TextBuilder(String text) {
            this.component = new TextComponent(TextUtil.color(text));
        }

        public TextBuilder color(ChatColor color) {
            component.setColor(color);
            return this;
        }

        public TextBuilder bold(boolean bold) {
            component.setBold(bold);
            return this;
        }
        
        public TextBuilder italic(boolean italic) {
            component.setItalic(italic);
            return this;
        }

        public TextBuilder underlined(boolean underlined) {
            component.setUnderlined(underlined);
            return this;
        }

        public TextBuilder onClick(ClickEvent.Action action, String value) {
            component.setClickEvent(new ClickEvent(action, value));
            return this;
        }

        @SuppressWarnings("deprecation")
		public TextBuilder onHover(String text) {
            BaseComponent[] hover = new ComponentBuilder(TextUtil.color(text)).create();
            component.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hover));
            return this;
        }

        private BaseComponent[] build() {
            return new BaseComponent[]{component};
        }
    }

}