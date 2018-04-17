package world.thefountain.customjukebox;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

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
		
		
		if (plugin == null) {
			throw new IllegalArgumentException("plugin must be non-null.");
		}

		if (sign == null) {
			throw new IllegalArgumentException("sign must be non-null.");
		}
		
		if (jukebox == null) {
			throw new IllegalArgumentException("jukebox must be non-null.");
		}
		
		if (song == null) {
			throw new IllegalArgumentException("song must be non-null.");
		}
		
		this.plugin = plugin;
		this.sign = sign;
		this.jukebox = jukebox;
		this.song = song;
		
		play();
	}
	
	private void play() {
		
		this.stop();
		
		PositionSongPlayer psp = createPositionSongPlayer(this.song, jukebox.getLocation());
		
		this.songPlayer = psp;
		
		this.plugin.getServer().getPluginManager().registerEvents(this, plugin);
		
		Bukkit.getLogger().info("Created custom jukebox at " + psp.getTargetLocation() + " playing '" + this.song.getTitle() + "'");
	}
	
	public void stop() {
		if (this.songPlayer != null) {
			HandlerList.unregisterAll(this);
			this.songPlayer.destroy();
		}
	}
	
	private void destroy() {
		this.stop();
		
		CustomJukeboxDestroyedEvent event = new CustomJukeboxDestroyedEvent(this);
		this.plugin.getServer().getPluginManager().callEvent(event);
		
		if (songPlayer != null) {
			Bukkit.getLogger().info("Destroyed custom jukebox at " + songPlayer.getTargetLocation());
		}
	}
	
	/**
	 * Get the location of the sound source.
	 * @return A clone of the sound source's {@link Location}.
	 */
	public Location getSoundSourceLocation() {
		return this.songPlayer.getTargetLocation().clone();
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
			this.destroy();
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
