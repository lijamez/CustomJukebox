package world.thefountain.customjukebox;

import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;

import com.xxmicloxx.NoteBlockAPI.Song;

class MusicSignUtils {

	private static final String SIGN_TITLE = "[Music]";
	
	private MusicSignUtils() { }
	
	/**
	 * Parses the sign lines for a song.
	 * @param signLines The sign's lines.
	 * @param songLib {@link SongLibrary}.
	 * @return The song.
	 * @throws SongNotFoundException if the song couldn't be found.
	 */
	public static Optional<Song> parseSongFromSignLines(String[] signLines, SongLibrary songLib) {
		if (signLines == null) {
			throw new IllegalArgumentException("signLines must be non-null.");
		}
		
		if (songLib == null) {
			throw new IllegalArgumentException("songLib must be non-null.");
		}
		
		if (signLines.length < 2) {
			return Optional.empty();
		}
		
		if (SIGN_TITLE.equals(signLines[0])) {
			
			StringBuilder trackNameSb = new StringBuilder();
			for (int i = 1; i < signLines.length; i++) {
				trackNameSb.append(signLines[i] + " ");
			}
			
			String trackName = StringUtils.trim(trackNameSb.toString());
			
			if (StringUtils.isBlank(trackName)) {
				return Optional.empty();
			}
			
			Song song = songLib.findSong(trackName).orElse(null);
			
			if (song != null) {
				return Optional.of(song);
			} else {
				throw new SongNotFoundException("Song '" + trackName + "' couldn't be found.");
			}
		} else {
			Bukkit.getLogger().info("Sign does not start with: " + SIGN_TITLE);
		}
		
		return Optional.empty();
	}
	
	public static boolean isMusicSign(Block signBlock, String[] signLines) {
		if (signBlock == null) {
			throw new IllegalArgumentException("signBlock must be non-null.");
		}
		
		if (signLines == null) {
			throw new IllegalArgumentException("signLines must be non-null.");
		}
		
		
		if (signBlock.getState().getType() == Material.WALL_SIGN || signBlock.getState().getType() == Material.SIGN_POST) {
			
			if (signLines.length <= 1) {
				return false;
			}
			
			String firstLine = signLines[0];
			return SIGN_TITLE.equals(firstLine);
		}
		
		return false;
	}
	
}
