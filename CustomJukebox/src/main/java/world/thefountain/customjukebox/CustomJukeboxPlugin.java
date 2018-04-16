package world.thefountain.customjukebox;

import java.io.File;

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
			songLib.refreshSongs();
			sender.sendMessage("Song list refreshed.");
		}
		return false;
	}
	
	private void listSongs(CommandSender sender) {
		StringBuilder sb = new StringBuilder();
		sb.append("Custom Jukebox Songs:\n");
		songLib.getSongs()
			.forEach(song -> sb.append("  " + song.getTitle() + "\n"));
		
		sender.sendMessage(sb.toString());
	}
}
