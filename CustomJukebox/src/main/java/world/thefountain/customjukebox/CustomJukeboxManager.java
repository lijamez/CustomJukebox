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
import org.bukkit.ChatColor;
import org.bukkit.Location;
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

	private static final double MIN_DISTANCE_APART = CustomJukebox.MAX_AUDIBLE_DISTANCE;
	
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
						Bukkit.getLogger().info("Some sign found at " + blockState.getLocation());
						
						org.bukkit.block.Sign s = (org.bukkit.block.Sign) blockState;
						CustomJukebox cj = createCustomJukeboxFrom(blockState, s.getLines(), null)
								.orElse(null);
						
						if (cj != null) {
							customJukeboxes.add(cj);
						}
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
				event.getPlayer().sendMessage(ChatColor.RED + "Error: " + e.getMessage());
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
			
			checkMusicSignPlacementPermissions(player);
			checkSingletonMusicSign(attachedBlock, signBlock);
			
			if (player != null) {
				// Only new jukeboxes being created by players should be subject to the proximity check.
				checkProximity(attachedBlock.getLocation());
			}
			
			Song song = MusicSignUtils.parseSongFromSignLines(signLines, songLib).orElse(null);
			
			if (song != null) {
				CustomJukebox cj = new CustomJukebox(this.plugin, signBlock, attachedBlock, song);
				
				return Optional.of(cj);
			}
			
		}
		
		return Optional.empty();
	}
	
	private void checkMusicSignPlacementPermissions(Player player) {
		if (player != null && !player.hasPermission("customjukebox.placemusicsign")) {
			throw new PermissionException("You don't have permissions to do that.");
		}
	}
	
	/**
	 * Checks that the jukebox has no other music signs.
	 * @param jukeboxBlock The jukebox block.
	 * @param signBlock The sign block that you are turning into a music sign.
	 * @throws MusicSignExistsException if there already exists some other music sign on this jukebox.
	 */
	private void checkSingletonMusicSign(Block jukeboxBlock, Block signBlock) {
		List<Block> attachedMusicSigns = getAttachedMusicSignBlocks(jukeboxBlock);
		
		if (attachedMusicSigns.size() > 1 || (attachedMusicSigns.size() == 1 && !attachedMusicSigns.get(0).equals(signBlock))) {
			throw new MusicSignExistsException("A music sign is already attached to this jukebox.");
		}
	}
	
	/**
	 * Checks that there is no other custom jukebox that's closer than {@link CustomJukeboxManager#MIN_DISTANCE_APART).
	 * @param loc The location of the new jukebox.
	 */
	private void checkProximity(Location loc) {
		boolean tooCloseToAnotherCustomJukebox = customJukeboxes.stream()
			.filter(cj -> cj.getSoundSourceLocation().getWorld().equals(loc.getWorld()))
			.filter(cj -> cj.getSoundSourceLocation().distance(loc) < MIN_DISTANCE_APART)
			.findAny()
			.isPresent();
		
		if (tooCloseToAnotherCustomJukebox) {
			throw new ProximityException("You are too close to another custom jukebox.");
		}
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
