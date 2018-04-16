package world.thefountain.customjukebox;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class CustomJukeboxDestroyedEvent extends Event {
	
	private static final HandlerList HANDLERS = new HandlerList();
    private final CustomJukebox cj;

    public CustomJukeboxDestroyedEvent(CustomJukebox cj) {
    	if (cj == null) {
    		throw new IllegalArgumentException("cj must be non-null.");
    	}
    	
        this.cj = cj;
    }

    /**
     * The {@link CustomJukebox} that was just destroyed.
     * @return The {@link CustomJukebox} that was just destroyed.
     */
    public CustomJukebox getCustomJukebox() {
        return cj;
    }

    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

}
