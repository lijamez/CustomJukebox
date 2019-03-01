package world.thefountain.customjukebox;

import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Rotation;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.xxmicloxx.NoteBlockAPI.event.SongEndEvent;
import com.xxmicloxx.NoteBlockAPI.model.Song;
import com.xxmicloxx.NoteBlockAPI.model.SoundCategory;
import com.xxmicloxx.NoteBlockAPI.songplayer.PositionSongPlayer;
import com.xxmicloxx.NoteBlockAPI.songplayer.SongPlayer;

public class CustomJukebox implements Listener {
	
	public static final int MAX_AUDIBLE_DISTANCE = 30;
	private static final int MAX_INTERNAL_VOLUME = 100;
	private static final Map<Rotation, Double> ROTATION_TO_VOLUME_FACTOR = ImmutableMap.<Rotation, Double>builder()
			.put(Rotation.NONE, 1.0d)
			.put(Rotation.CLOCKWISE_45, 0.125d)
			.put(Rotation.CLOCKWISE, 0.25d)
			.put(Rotation.CLOCKWISE_135, 0.375d)
			.put(Rotation.FLIPPED, 0.5d)
			.put(Rotation.FLIPPED_45, 0.625d)
			.put(Rotation.COUNTER_CLOCKWISE, 0.75d)
			.put(Rotation.COUNTER_CLOCKWISE_45, 0.875d)
			.build();

	private final Plugin plugin;
	private final Block sign;
	private final Block jukebox;
	private final Song song;

	private PositionSongPlayer songPlayer = null;
	
	private ItemFrame volumeKnob = null;
	private double volumeFactor = 1d;
	
	public CustomJukebox(Plugin plugin, Block sign, Block jukebox, Song song) {
		this.plugin = Preconditions.checkNotNull(plugin, "plugin must be non-null.");
		this.sign = Preconditions.checkNotNull(sign, "sign must be non-null.");
		this.jukebox = Preconditions.checkNotNull(jukebox, "jukebox must be non-null.");
		this.song = Preconditions.checkNotNull(song, "song must be non-null.");
		
		this.volumeKnob = detectVolumeKnob();
		
		this.songPlayer = createPositionSongPlayer(this.song, jukebox.getLocation());
		this.songPlayer.getFadeIn().setFadeDuration(0);
		updateVolumeFromKnob();

		this.plugin.getServer().getPluginManager().registerEvents(this, plugin);
	}
	
	private ItemFrame detectVolumeKnob() {
		// Find the first volume knob.
		return this.jukebox.getWorld().getNearbyEntities(this.jukebox.getLocation(), 2, 2, 2)
			.stream()
			.filter(entity -> (entity instanceof ItemFrame))
			.map(entity -> (ItemFrame) entity)
			.filter(itemFrame -> itemFrame.getLocation().getBlock().getRelative(itemFrame.getAttachedFace()).equals(this.jukebox))
			.findFirst()
			.orElse(null);
	}
	
	public Song getSong() {
		return this.song;
	}
	
	public void destroy() {
		destroy(null);
	}
	
	private void destroy(Player player) {
		HandlerList.unregisterAll(this);
		this.songPlayer.destroy();
		
		CustomJukeboxDestroyedEvent event = new CustomJukeboxDestroyedEvent(this, player);
		this.plugin.getServer().getPluginManager().callEvent(event);
	}
	
	public Location getSignLocation() {
		return this.sign.getLocation();
	}
	
	@EventHandler
	public void onLogin(PlayerLoginEvent event) {
		songPlayer.addPlayer(event.getPlayer());
	}
	
	@EventHandler
	public void onLogout(PlayerQuitEvent event) {
		songPlayer.removePlayer(event.getPlayer());
	}
	
	@EventHandler
	public void onSongEnd(SongEndEvent event) {
		if (event.getSongPlayer().equals(this.songPlayer)) {
			SongPlayer oldPlayer = this.songPlayer;
			
			this.songPlayer = createPositionSongPlayer(this.song, jukebox.getLocation());
			updateVolumeFromKnob();
			
			oldPlayer.destroy();
		}
	}
	
	@EventHandler(ignoreCancelled = true)
	public void onBlockBreak(BlockBreakEvent event) {
		Block brokenBlock = event.getBlock();
		if (brokenBlock.equals(this.sign) || brokenBlock.equals(this.jukebox)) {
			
			this.destroy(event.getPlayer());
		}
	}
	
	@EventHandler(ignoreCancelled = true)
	public void onHangingPlace(HangingPlaceEvent event) {
		if (this.jukebox.equals(event.getBlock())) {
			if (event.getEntity() instanceof ItemFrame) {
				if (this.volumeKnob == null) {
					this.volumeKnob = (ItemFrame) event.getEntity();
					event.getPlayer().sendMessage("Volume knob attached. Put an item inside and rotate it to adjust volume.");
				} else if (!this.volumeKnob.equals(event.getEntity())) {
					event.setCancelled(true);
					event.getPlayer().sendMessage("Only one volume knob allowed on the jukebox.");
				}
			}
		}
	}
	
	@EventHandler(ignoreCancelled = true)
	public void onHangingBreakEvent(HangingBreakEvent event) {
		if (this.volumeKnob != null && this.volumeKnob.equals(event.getEntity())) {
			this.volumeKnob = null;
			updateVolumeFromKnob();
			sendVolumeMessage(Bukkit.getServer().getConsoleSender());
		}
	}
	
	@EventHandler(ignoreCancelled = true) 
	public void onHangingBreakByEntityEvent(HangingBreakByEntityEvent event) {
		if (event.getRemover() instanceof Player) {
			Player remover = (Player) event.getRemover();
			sendVolumeMessage(remover);
		}
	}
	
	@EventHandler(ignoreCancelled = true)
	public void onEntityInteract(PlayerInteractEntityEvent event) {
		Entity interactedEntity = event.getRightClicked();
		if (this.volumeKnob != null && this.volumeKnob.equals(interactedEntity)) {
			ItemStack frameItem = this.volumeKnob.getItem();
			if (frameItem != null && frameItem.getType() != Material.AIR) {
				// This appears to be a rotation action.
				event.setCancelled(true);
				this.volumeKnob.setRotation(this.volumeKnob.getRotation().rotateClockwise());
				
				updateVolumeFromKnob();
				sendVolumeMessage(event.getPlayer());
			}
		}
	}
	
	private void sendVolumeMessage(CommandSender cmdSender) {
		String displayVolume = (this.volumeFactor * 100) + "%";
		cmdSender.sendMessage("Volume: " +  displayVolume);
	}
	
	private void updateVolumeFromKnob() {
		if (this.volumeKnob == null) {
			this.volumeFactor = 1d;
		} else {
			this.volumeFactor = ROTATION_TO_VOLUME_FACTOR.getOrDefault(this.volumeKnob.getRotation(), 1d);
		}
		
		byte targetVolume = (byte) (MAX_INTERNAL_VOLUME * volumeFactor);
		this.songPlayer.setVolume(targetVolume);
	}
	
	private PositionSongPlayer createPositionSongPlayer(Song song, Location location) {
		
		PositionSongPlayer sp = new PositionSongPlayer(song, SoundCategory.RECORDS);
		sp.setTargetLocation(location);
		sp.setAutoDestroy(true);
		sp.setDistance(MAX_AUDIBLE_DISTANCE);
		sp.setPlaying(true);
		
		this.plugin.getServer().getOnlinePlayers().stream()
			.forEach(player -> sp.addPlayer(player));
		
		return sp;
	}
}
