package world.thefountain.customjukebox;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.bukkit.Bukkit;

import com.xxmicloxx.NoteBlockAPI.NBSDecoder;
import com.xxmicloxx.NoteBlockAPI.Song;

import me.xdrop.fuzzywuzzy.FuzzySearch;
import me.xdrop.fuzzywuzzy.model.ExtractedResult;

public class SongLibrary {

	private static final int HIT_SCORE_THRESHOLD = 50;
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
		
		List<Song> songsMutable = Arrays.stream(searchResults)
			.flatMap(file -> {
				try {
					return Stream.of(NBSDecoder.parse(file));
				} catch (Exception e) {
					Bukkit.getLogger().log(Level.WARNING, "Unable to read NBS file " + file.getAbsolutePath(), e);
					return Stream.empty();
				}
			})
			.collect(Collectors.toList());
		
		this.songs = Collections.unmodifiableList(songsMutable);
	}
	
	/**
	 * Finds the best matched song.
	 * @param query A query on the song's title.
	 * @return A {@link Song}, if it could find one with the given query.
	 */
	public Optional<Song> findSong(String query) {
		if (query == null) {
			throw new IllegalArgumentException("query must be non-null.");
		}
		
		Map<String, Song> titleToSongMap = this.songs.stream()
			.collect(Collectors.toMap(song -> song.getTitle(), song -> song));
		
		List<ExtractedResult> results = FuzzySearch.extractTop(query, titleToSongMap.keySet(), 1);
		if (results.isEmpty()) {
			return Optional.empty();
		} else {
			ExtractedResult topHit = results.get(0);
			Song topHitSong = topHit.getScore() < HIT_SCORE_THRESHOLD ? null : titleToSongMap.get(topHit.getString());
			
			return Optional.ofNullable(topHitSong);
		}
	}
	
	/**
	 * Gets an immutable view of the songs list.
	 * @return 
	 */
	public List<Song> getSongs() {
		return songs;
	}
}
