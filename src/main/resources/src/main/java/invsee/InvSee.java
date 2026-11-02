package invsee;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class InvSee extends JavaPlugin implements Listener {

    private static class View implements InventoryHolder {
        UUID target;
        boolean armor;
        Inventory inv;

        @Override
        public Inventory getInventory() {
            return inv;
        }
    }

    private final Map<UUID, View> viewers = new HashMap<>();

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        Bukkit.getScheduler().runTaskTimer(this, this::refreshAll, 10L, 10L);
        getLogger().info("InvSee yoqildi.");
    }

    @Override
    public boolean onCommand(CommandSender s, Command cmd, String label, String[] a) {
        if (!(s instanceof Player p)) {
            s.sendMessage("Bu buyruq faqat o'yinda ishlaydi.");
            return true;
        }
        if (!p.hasPermission("invsee.use")) {
            p.sendMessage(Component.text("Sizda ruxsat yo'q.", NamedTextColor.RED));
            return true;
        }
        boolean armor = cmd.getName().equalsIgnoreCase("armorsee");
        if (a.length != 1) {
            p.sendMessage(Component.text("Ishlatish: /" + (armor ? "armorsee" : "invsee")
                    + " <nick>", NamedTextColor.YELLOW));
            return true;
        }
        Player t = Bukkit.getPlayerExact(a[0]);
        if (t == null) {
            p.sendMessage(Component.text("O'yinchi topilmadi yoki serverda emas.",
                    NamedTextColor.RED));
            return true;
        }
        if (t.hasPermission("invsee.exempt") && !p.hasPermission("invsee.exempt")) {
            p.sendMessage(Component.text("Bu o'yinchining narsalarini ko'rib bo'lmaydi.",
                    NamedTextColor.RED));
            return true;
        }
        View v = new View();
        v.target = t.getUniqueId();
        v.armor = armor;
        Component title = Component.text((armor ? "Zirh: " : "Inventar: ") + t.getName());
        v.inv = Bukkit.createInventory(v, armor ? 27 : 36, title);
        fill(v, t);
        p.openInventory(v.inv);
        viewers.put(p.getUniqueId(), v);
        return true;
    }

    // ---------- Ko'rinishni to'ldirish ----------
    private Component line(String text, NamedTextColor c) {
        return Component.text(text, c).decoration(TextDecoration.ITALIC, false);
    }

    private ItemStack pane(Material mat, String name) {
        ItemStack it = new ItemStack(mat);
        ItemMeta m = it.getItemMeta();
        m.displayName(line(name, NamedTextColor.GRAY));
        it.setItemMeta(m);
        return it;
    }

    private ItemStack orEmpty(ItemStack it, String emptyName) {
        if (it == null || it.getType().isAir()) {
            return pane(Material.GRAY_STAINED_GLASS_PANE, emptyName);
        }
        return it;
    }

    private ItemStack info(Player t) {
        ItemStack it = new ItemStack(Material.PAPER);
        ItemMeta m = it.getItemMeta();
        m.displayName(line("Ma'lumot: " + t.getName(), NamedTextColor.GOLD));
        m.lore(List.of(
                line("Salomatlik: " + Math.round(t.getHealth()), NamedTextColor.RED),
                line("Ochlik: " + t.getFoodLevel(), NamedTextColor.GOLD),
                line("Daraja: " + t.getLevel(), NamedTextColor.GREEN),
                line("Rejim: " + t.getGameMode().name(), NamedTextColor.AQUA),
                line("Dunyo: " + t.getWorld().getName(), NamedTextColor.GRAY),
                line("Joy: " + t.getLocation().getBlockX() + " " + t.getLocation().getBlockY()
                        + " " + t.getLocation().getBlockZ(), NamedTextColor.GRAY)));
        it.setItemMeta(m);
        return it;
    }

    private void fill(View v, Player t) {
        PlayerInventory pi = t.getInventory();
        Inventory inv = v.inv;
        if (!v.armor) {
            for (int i = 0; i < 27; i++) inv.setItem(i, pi.getItem(i + 9));
            for (int i = 0; i < 9; i++) inv.setItem(27 + i, pi.getItem(i));
            return;
        }
        ItemStack filler = pane(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 27; i++) inv.setItem(i, filler);
        inv.setItem(10, orEmpty(pi.getHelmet(), "Dubulg'a yo'q"));
        inv.setItem(11, orEmpty(pi.getChestplate(), "Ko'krak zirhi yo'q"));
        inv.setItem(12, orEmpty(pi.getLeggings(), "Shim zirhi yo'q"));
        inv.setItem(13, orEmpty(pi.getBoots(), "Etik yo'q"));
        inv.setItem(15, orEmpty(pi.getItemInOffHand(), "Chap qo'l bo'sh"));
        inv.setItem(16, info(t));
    }

    private void refreshAll() {
        for (Map.Entry<UUID, View> en : new ArrayList<>(viewers.entrySet())) {
            Player viewer = Bukkit.getPlayer(en.getKey());
            Player target = Bukkit.getPlayer(en.getValue().target);
            if (viewer == null) {
                viewers.remove(en.getKey());
                continue;
            }
            if (target == null) {
                viewers.remove(en.getKey());
                viewer.closeInventory();
                continue;
            }
            fill(en.getValue(), target);
        }
    }

    // ---------- Faqat ko'rish: narsalarni olib bo'lmaydi ----------
    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (e.getInventory().getHolder() instanceof View) e.setCancelled(true);
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        if (e.getInventory().getHolder() instanceof View) e.setCancelled(true);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        if (e.getInventory().getHolder() instanceof View) {
            viewers.remove(e.getPlayer().getUniqueId());
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        viewers.remove(e.getPlayer().getUniqueId());
    }
                    }
