package world.thefountain.customjukebox;

import java.io.File;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

public class CustomJukeboxPlugin extends JavaPlugin {
	
	private SongLibrary songLib;
	private CustomJukeboxManager cjm = null;
	
	@Override
	public void onEnable() {
		File songsDir = new File(this.getDataFolder(), "songs/");
		songsDir.mkdirs();
		
		this.songLib = new SongLibrary(songsDir);
		this.cjm = new CustomJukeboxManager(this, songLib);
		
		songLib.refreshSongs();
	}
	
	@Override
	public void onDisable() {
		if (this.cjm != null) {
			this.cjm.destroy();
			this.cjm = null;
		}
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (cmd.getName().equalsIgnoreCase("cjlist")) {
			listSongs(sender);
		} else if (cmd.getName().equalsIgnoreCase("cjrefresh")) {
			sender.sendMessage("Refreshing song list...");
			// TODO: This operation can take quite some time. Consider doing it in a separate thread.
			songLib.refreshSongs();
			sender.sendMessage("Song list refreshed. Found " + songLib.getSongs().size() + " songs.");
		}
		return false;
	}
	
	private void listSongs(CommandSender sender) {
		StringBuilder sb = new StringBuilder();
		sb.append(ChatColor.BOLD + "Custom Jukebox Songs:\n");
		songLib.getSongs().stream()
			.sorted((s1, s2) -> {
				return s1.getTitle().compareTo(s2.getTitle());
			})
			.forEach(song -> {
				sb.append(ChatColor.GOLD);
				sb.append("  " + song.getTitle());
				
				if (null != song.getAuthor() && !song.getAuthor().isEmpty()) {
					sb.append(ChatColor.GRAY);
					sb.append(ChatColor.ITALIC);
					sb.append(" by " + song.getAuthor());
				}
				
				sb.append("\n");
			});
		
		sender.sendMessage(sb.toString());
	}
}
