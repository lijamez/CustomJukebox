package world.thefountain.customjukebox;

@SuppressWarnings("serial")
public abstract class UserException extends RuntimeException {

	public UserException(String message) {
		super(message);
	}
}
