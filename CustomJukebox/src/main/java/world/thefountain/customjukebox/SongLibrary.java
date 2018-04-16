package world.thefountain.customjukebox;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.xxmicloxx.NoteBlockAPI.NBSDecoder;
import com.xxmicloxx.NoteBlockAPI.Song;

public class SongLibrary {

	private static final String SONG_FILE_EXT = ".nbs";
	
	private final File songsDir;
	private List<Song> songs;
	
	public SongLibrary(File songsDir) {
		if (songsDir == null) {
			throw new IllegalArgumentException("songsDir must be non-null.");
		}
		
		this.songsDir = songsDir;
		
		refreshSongs();
	}
	
	public void refreshSongs() {
		File[] searchResults = songsDir.listFiles((f) -> f.getName().endsWith(SONG_FILE_EXT));
		
		this.songs = Arrays.stream(searchResults)
			.map(NBSDecoder::parse)
			.collect(Collectors.toList());
	}
	
	public Optional<Song> findSong(String query) {
		if (query == null) {
			throw new IllegalArgumentException("query must be non-null.");
		}
		
		return this.songs.stream()
				.filter(song -> normalize(query).equalsIgnoreCase(normalize(song.getTitle())))
				.findFirst();
	}
	
	private String normalize(String string) {
		return string.replaceAll("\\s+", " ").trim();
	}
	
	/**
	 * Gets an immutable view of the songs list.
	 * @return 
	 */
	public List<Song> getSongs() {
		return Collections.unmodifiableList(songs);
	}
}
