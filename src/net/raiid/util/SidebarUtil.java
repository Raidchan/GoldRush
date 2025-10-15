package net.raiid.util;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.*;

import java.util.List;
import java.util.Objects;

public class SidebarUtil {

    private final Player player;
    private final Scoreboard scoreboard;
    private final Objective objective;

    public SidebarUtil(Player player, String title) {
        this.player = player;
        this.scoreboard = Objects.requireNonNull(Bukkit.getScoreboardManager()).getNewScoreboard();
        this.objective = scoreboard.registerNewObjective("sidebar", Criteria.DUMMY, format(title));
        this.objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        player.setScoreboard(this.scoreboard);
    }

    public void setTitle(String title) {
        this.objective.setDisplayName(format(title));
    }

    public void setLines(List<String> lines) {
        for (String entry : scoreboard.getEntries()) {
            scoreboard.resetScores(entry);
        }
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            String entry = ChatColor.values()[i] + "" + ChatColor.WHITE;
            objective.getScore(entry + format(line)).setScore(lines.size() - i);
        }
    }

    public void remove() {
        player.setScoreboard(Objects.requireNonNull(Bukkit.getScoreboardManager()).getMainScoreboard());
    }

    private String format(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }

}
