package world.thefountain.customjukebox;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.material.Sign;
import org.bukkit.plugin.Plugin;

import com.google.common.collect.Lists;
import com.xxmicloxx.NoteBlockAPI.Song;

public class CustomJukeboxManager implements Listener {

	private static final List<BlockFace> ATTACHABLE_FACES = Collections.unmodifiableList(Lists.newArrayList(
			BlockFace.NORTH,
			BlockFace.SOUTH,
			BlockFace.EAST,
			BlockFace.WEST,
			BlockFace.UP));
	
	private final List<CustomJukebox> customJukeboxes = new ArrayList<>();
	
	private final Plugin plugin;
	private final SongLibrary songLib;
	
	public CustomJukeboxManager(Plugin plugin, SongLibrary songLib) {
		if (plugin == null) {
			throw new IllegalArgumentException("plugin must be non-null.");
		}
		
		if (songLib == null) {
			throw new IllegalArgumentException("songLib must be non-null.");
		}
		
		this.plugin = plugin;
		this.songLib = songLib;
		
		this.plugin.getServer().getPluginManager().registerEvents(this, this.plugin);
		
		initCustomJukeboxes();
	}
	
	public void destroy() {
		customJukeboxes.forEach(cj -> {
			cj.stop();
		});
		customJukeboxes.clear();
		
		HandlerList.unregisterAll(this);
	}
	
	private void initCustomJukeboxes() {
		
		customJukeboxes.clear();
		
		this.plugin.getServer().getWorlds().stream()
			.flatMap(world -> Arrays.stream(world.getLoadedChunks()))
			.flatMap(chunk -> Arrays.stream(chunk.getTileEntities()))
			.forEach(blockState -> {
				try {
					if (blockState instanceof org.bukkit.block.Sign) {
						org.bukkit.block.Sign s = (org.bukkit.block.Sign) blockState;
						createCustomJukeboxFrom(blockState, s.getLines(), null);
					}
				} catch (MusicSignExistsException | SongNotFoundException e) {
					Bukkit.getLogger().log(Level.WARNING, "Found a music sign, but is bad.", e);
				}
			});
	}
	
	@EventHandler
	public void onSignChanged(SignChangeEvent event) {
		if (!event.isCancelled()) {
			try {
				CustomJukebox cj = createCustomJukeboxFrom(
						event.getBlock().getState(), 
						event.getLines(),
						event.getPlayer())
					.orElse(null);
				
				if (cj != null) {
					this.customJukeboxes.add(cj);
				}
			} catch (UserException e) {
				event.getPlayer().sendMessage("Error: " + e.getMessage());
				event.getBlock().breakNaturally();
			}
		}
	}
	
	private Optional<CustomJukebox> createCustomJukeboxFrom(BlockState bs, String[] signLines, Player player) {
		
		if (!(bs instanceof org.bukkit.block.Sign)) {
			return Optional.empty();
		}
		
		Block signBlock = bs.getBlock();
		Sign sign = (Sign) bs.getData();
		Block attachedBlock = signBlock.getRelative(sign.getAttachedFace());
		
		if (MusicSignUtils.isMusicSign(signBlock, signLines) && attachedBlock != null && attachedBlock.getType() == Material.JUKEBOX) {
			
			if (player != null && !player.hasPermission("customjukebox.placemusicsign")) {
				throw new PermissionException("You don't have permissions to do that.");
			}
			
			List<Block> attachedMusicSigns = getAttachedMusicSignBlocks(attachedBlock);
			
			if (attachedMusicSigns.size() > 1 || (attachedMusicSigns.size() == 1 && !attachedMusicSigns.get(0).equals(signBlock))) {
				throw new MusicSignExistsException("A music sign is already attached to this jukebox.");
			} 
			
			Song song = MusicSignUtils.parseSignLines(signLines, songLib).orElse(null);
			
			if (song != null) {
				CustomJukebox cj = new CustomJukebox(this.plugin, signBlock, attachedBlock, song);
				
				return Optional.of(cj);
			}
			
		}
		
		return Optional.empty();
	}
	
	private List<Block> getAttachedMusicSignBlocks(Block block) {
		return ATTACHABLE_FACES.stream()
			.flatMap(face -> {
				Block adjacentBlock = block.getRelative(face);
				
				if (adjacentBlock.getState() instanceof org.bukkit.block.Sign) {
					org.bukkit.block.Sign s = (org.bukkit.block.Sign) adjacentBlock.getState();
					
					if (MusicSignUtils.isMusicSign(adjacentBlock, s.getLines())) {
						return Lists.newArrayList(adjacentBlock).stream();
					}
				}
				
				return Stream.empty();
			})
			.collect(Collectors.toList());
	}
	
	@EventHandler
	public void onCustomJukeboxDestroyed(CustomJukeboxDestroyedEvent event) {
		this.customJukeboxes.remove(event.getCustomJukebox());
	}
}
