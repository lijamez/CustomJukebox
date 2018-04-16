package world.thefountain.customjukebox;

@SuppressWarnings("serial")
public class SongNotFoundException extends UserException {

	public SongNotFoundException(String message) {
		super(message);
	}
}
