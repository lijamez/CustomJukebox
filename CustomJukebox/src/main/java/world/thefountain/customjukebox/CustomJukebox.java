package world.thefountain.customjukebox;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

import com.google.common.base.Preconditions;
import com.xxmicloxx.NoteBlockAPI.PositionSongPlayer;
import com.xxmicloxx.NoteBlockAPI.Song;
import com.xxmicloxx.NoteBlockAPI.SongEndEvent;

public class CustomJukebox implements Listener {
	
	public static final int MAX_AUDIBLE_DISTANCE = 40;
	
	private final Plugin plugin;
	private final Block sign;
	private final Block jukebox;
	private final Song song;
	
	private PositionSongPlayer songPlayer = null;
	
	public CustomJukebox(Plugin plugin, Block sign, Block jukebox, Song song) {
		this.plugin = Preconditions.checkNotNull(plugin, "plugin must be non-null.");
		this.sign = Preconditions.checkNotNull(sign, "sign must be non-null.");
		this.jukebox = Preconditions.checkNotNull(jukebox, "jukebox must be non-null.");
		this.song = Preconditions.checkNotNull(song, "song must be non-null.");
		
		play();
	}
	
	private void play() {
		
		this.stop();
		
		PositionSongPlayer psp = createPositionSongPlayer(this.song, jukebox.getLocation());
		
		this.songPlayer = psp;
		
		this.plugin.getServer().getPluginManager().registerEvents(this, plugin);
	}
	
	public void stop() {
		if (this.songPlayer != null) {
			HandlerList.unregisterAll(this);
			this.songPlayer.destroy();
		}
	}
	
	public Song getSong() {
		return this.song;
	}
	
	private void destroy(Player player) {
		this.stop();
		
		CustomJukeboxDestroyedEvent event = new CustomJukeboxDestroyedEvent(this, player);
		this.plugin.getServer().getPluginManager().callEvent(event);
	}
	
	public Location getSignLocation() {
		return this.sign.getLocation();
	}
	
	@EventHandler
	public void onLogin(PlayerLoginEvent event) {
		if (songPlayer != null) {
			songPlayer.addPlayer(event.getPlayer());
		}
	}
	
	@EventHandler
	public void onLogout(PlayerQuitEvent event) {
		if (songPlayer != null) {
			songPlayer.removePlayer(event.getPlayer());
		}
	}
	
	@EventHandler
	public void onSongEnd(SongEndEvent event) {
		if (event.getSongPlayer().equals(this.songPlayer)) {
			PositionSongPlayer oldSongPlayer = this.songPlayer;
			
			// Loop the song by creating another SongPlayer with the same song and location. 
			if (oldSongPlayer != null) {
				//Bukkit.getLogger().log(Level.INFO, "Song '" + this.song.getTitle() + "' playing at" + oldSongPlayer.getTargetLocation() + " has ended. Looping...");
				oldSongPlayer.destroy();
				this.songPlayer = createPositionSongPlayer(oldSongPlayer.getSong(), oldSongPlayer.getTargetLocation());
			}
		}
	}
	
	@EventHandler
	public void onBlockBreak(BlockBreakEvent event) {
		if (event.getBlock().equals(this.sign) || event.getBlock().equals(this.jukebox)) {
			
			this.destroy(event.getPlayer());
		}
	}
	
	private PositionSongPlayer createPositionSongPlayer(Song song, Location location) {
		
		PositionSongPlayer sp = new PositionSongPlayer(song);
		sp.setTargetLocation(location);
		sp.setAutoDestroy(true);
		sp.setDistance(MAX_AUDIBLE_DISTANCE);
		sp.setPlaying(true);
		
		this.plugin.getServer().getOnlinePlayers().stream()
			.forEach(player -> sp.addPlayer(player));
		
		return sp;
	}
}
