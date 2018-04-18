package world.thefountain.customjukebox;

import java.util.Optional;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import com.google.common.base.Preconditions;

public class CustomJukeboxDestroyedEvent extends Event {
	
	private static final HandlerList HANDLERS = new HandlerList();
    private final CustomJukebox cj;
    private final Player player;

    public CustomJukeboxDestroyedEvent(CustomJukebox cj, Player player) {    	
        this.cj = Preconditions.checkNotNull(cj, "cj must be non-null.");
        this.player = player;
    }

    /**
     * The {@link CustomJukebox} that was just destroyed.
     * @return The {@link CustomJukebox} that was just destroyed.
     */
    public CustomJukebox getCustomJukebox() {
        return cj;
    }
    
    public Optional<Player> getPlayer() {
    	return Optional.ofNullable(player);
    }

    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

}
