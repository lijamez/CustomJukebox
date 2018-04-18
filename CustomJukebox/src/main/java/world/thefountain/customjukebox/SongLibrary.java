package world.thefountain.customjukebox;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.bukkit.Bukkit;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
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
		this.songsDir = Preconditions.checkNotNull(songsDir, "songsDir must be non-null.");
		
		refreshSongs();
	}
	
	public void refreshSongs() {
		File[] searchResults = songsDir.listFiles((f) -> f.getName().endsWith(SONG_FILE_EXT));
		
		this.songs = Arrays.stream(searchResults)
			.flatMap(file -> {
				try {
					return Stream.of(NBSDecoder.parse(file));
				} catch (Exception e) {
					Bukkit.getLogger().log(Level.WARNING, "Unable to read NBS file " + file.getAbsolutePath(), e);
					return Stream.empty();
				}
			})
			.collect(ImmutableList.toImmutableList());
	}
	
	/**
	 * Finds the best matched song.
	 * @param query A query on the song's title.
	 * @return A {@link Song}, if it could find one with the given query.
	 */
	public Optional<Song> findSong(String query) {
		Preconditions.checkNotNull(query, "query must be non-null.");
		
		Map<String, Song> titleToSongMap = this.songs.stream()
			.collect(Collectors.toMap(song -> song.getTitle(), song -> song));
		
		List<ExtractedResult> results = FuzzySearch.extractTop(query, titleToSongMap.keySet(), 1);
		
		Song result = null;
		if (!results.isEmpty()) {
			ExtractedResult topHit = results.get(0);
			result = topHit.getScore() < HIT_SCORE_THRESHOLD ? null : titleToSongMap.get(topHit.getString());
		}
		
		return Optional.ofNullable(result);
	}
	
	/**
	 * Gets an immutable view of the songs list.
	 * @return An immutable list of songs.
	 */
	public List<Song> getSongs() {
		return songs;
	}
}
