package world.thefountain.customjukebox;

import java.io.File;

import org.apache.commons.lang3.StringUtils;
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
		try {
			if (cmd.getName().equalsIgnoreCase("cj")) {
				if (args.length > 0) {
					String action = args[0];
					
					if (StringUtils.equals(action, "list")) {
						listSongs(sender);
						return true;
					} else if (StringUtils.equals(action, "refresh")) {
						refreshSongs(sender);
						return true;
					}
				}
				
				showHelp(sender);
				return true;
			}
		} catch (UserException e) {
			sender.sendMessage(ChatColor.RED + "Error: " + e.getMessage());
			return true;
		}
		
		return false;
	}
	
	private void refreshSongs(CommandSender sender) {
		Utils.assertPermissions(sender, "customjukebox.refresh");
		
		sender.sendMessage("Refreshing song list...");
		// TODO: This operation can take quite some time. Consider doing it in a separate thread.
		songLib.refreshSongs();
		sender.sendMessage("Song list refreshed. Found " + songLib.getSongs().size() + " songs.");
	}
	
	private void listSongs(CommandSender sender) {
		Utils.assertPermissions(sender, "customjukebox.list");
		
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
	
	private void showHelp(CommandSender sender) {
		Utils.assertPermissions(sender, "customjukebox.help");
		
		StringBuilder sb = new StringBuilder();
		sb.append(ChatColor.LIGHT_PURPLE).append("---------- Custom Jukebox Help ----------\n")
			.append(ChatColor.GREEN).append("/cj list ").append(ChatColor.RESET).append("Lists all of the songs.\n")
			.append(ChatColor.GREEN).append("/cj refresh ").append(ChatColor.RESET).append("Refreshes the list of songs.\n")
			.append("\n\n")
			.append(ChatColor.GRAY).append("How to create a custom jukebox:\n").append(ChatColor.RESET)
			.append("Put a sign on a jukebox with the first line saying ").append(ChatColor.GOLD).append(MusicSignUtils.SIGN_HEADER)
				.append(ChatColor.RESET).append(" and then the remaining lines containing the song name that you want to play. Use ")
				.append(ChatColor.GREEN).append("/cj list").append(ChatColor.RESET).append(" to see the list of songs.\n")
			.append(ChatColor.GRAY).append("How to change the volume of a jukebox:\n").append(ChatColor.RESET)
				.append("Put an Item Frame on the jukebox, put an item inside that frame, and rotate it.");
		
		
		sender.sendMessage(sb.toString());
	}
}
