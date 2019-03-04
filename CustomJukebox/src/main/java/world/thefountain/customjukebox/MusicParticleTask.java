package world.thefountain.customjukebox;

import java.util.Random;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitRunnable;

import com.google.common.base.Preconditions;

class MusicParticleTask extends BukkitRunnable {

	private static final Random RANDOM = new Random();
	
	// Particle locations are constrained to at most this many units away.
	private static final double XZ_RADIUS = 1.5;
	private static final double Y_RADIUS = 0.2;
	
	private final CustomJukebox customJukebox;
	
	public MusicParticleTask(CustomJukebox customJukebox) {
		this.customJukebox = Preconditions.checkNotNull(customJukebox, "customJukebox must be non-null.");
	}
	
	@Override
	public void run() {
		Location jukeboxLocation = customJukebox.getJukeboxLocation();
		World world = customJukebox.getJukeboxLocation().getWorld();
		
		// Particle location is the center of the top of the jukebox.
		Location particleLocation = new Location(
				world, 
				jukeboxLocation.getX() + 0.5 + Math.max(Math.min(RANDOM.nextGaussian() / 3, XZ_RADIUS), -XZ_RADIUS), 
				jukeboxLocation.getY() + 0.9 + Math.max(Math.min(RANDOM.nextGaussian() / 6, Y_RADIUS), -Y_RADIUS), 
				jukeboxLocation.getZ() + 0.5 + Math.max(Math.min(RANDOM.nextGaussian() / 3, XZ_RADIUS), -XZ_RADIUS));
		
		world.spawnParticle(Particle.NOTE, particleLocation, 1);
	}
}
